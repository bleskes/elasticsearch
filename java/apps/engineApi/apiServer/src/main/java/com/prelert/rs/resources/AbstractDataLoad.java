/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2015     *
 *                                                          *
 *----------------------------------------------------------*
 *----------------------------------------------------------*
 * WARNING:                                                 *
 * THIS FILE CONTAINS UNPUBLISHED PROPRIETARY               *
 * SOURCE CODE WHICH IS THE PROPERTY OF PRELERT LTD AND     *
 * PARENT OR SUBSIDIARY COMPANIES.                          *
 * PLEASE READ THE FOLLOWING AND TAKE CAREFUL NOTE:         *
 *                                                          *
 * This source code is confidential and any person who      *
 * receives a copy of it, or believes that they are viewing *
 * it without permission is asked to notify Prelert Ltd     *
 * on +44 (0)20 3567 1249 or email to legal@prelert.com.    *
 * All intellectual property rights in this source code     *
 * are owned by Prelert Ltd.  No part of this source code   *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ************************************************************/
package com.prelert.rs.resources;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataCounts;
import com.prelert.job.JobDetails;
import com.prelert.job.JobException;
import com.prelert.job.UnknownJobException;
import com.prelert.job.data.DataStreamer;
import com.prelert.job.data.DataStreamerThread;
import com.prelert.job.data.InputStreamDuplicator;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.exceptions.TooManyJobsException;
import com.prelert.job.messages.Messages;
import com.prelert.job.process.exceptions.MalformedJsonException;
import com.prelert.job.process.exceptions.MissingFieldException;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.process.params.DataLoadParams;
import com.prelert.job.process.params.InterimResultsParams;
import com.prelert.job.process.params.TimeRange;
import com.prelert.job.status.HighProportionOfBadTimestampsException;
import com.prelert.job.status.OutOfOrderRecordsException;
import com.prelert.rs.data.Acknowledgement;
import com.prelert.rs.data.ApiError;
import com.prelert.rs.data.DataPostResponse;
import com.prelert.rs.data.MultiDataPostResult;
import com.prelert.rs.exception.InvalidParametersException;
import com.prelert.rs.provider.MapperUtils;


public abstract class AbstractDataLoad extends ResourceWithJobManager
{
    private static final Logger LOGGER = Logger.getLogger(AbstractDataLoad.class);

    /**
     * Parameter to control whether interim results are calculated on a flush
     */
    public static final String CALC_INTERIM_PARAM = "calcInterim";

    /** Parameter to specify start time of buckets to be reset */
    public static final String RESET_START_PARAM = "resetStart";

    /** Parameter to specify end time of buckets to be reset */
    public static final String RESET_END_PARAM = "resetEnd";

    public static final int MILLISECONDS_IN_SECOND = 1000;

    public static final String JOB_SEPARATOR = ",";

    /**
     * Data upload endpoint.
     *
     * @param headers
     * @param jobId
     * @param input
     * @param resetStart Optional parameter to specify start of range for bucket resetting
     * @param resetEnd Optional parameter to specify end of range for bucket resetting
     * @return
     * @throws IOException
     * @throws UnknownJobException
     * @throws NativeProcessRunException
     * @throws MissingFieldException
     * @throws JobInUseException if the data cannot be written to because
     * the job is already handling data
     * @throws HighProportionOfBadTimestampsException
     * @throws OutOfOrderRecordsException
     * @throws TooManyJobsException If the license is violated
     * @throws MalformedJsonException If JSON data is malformed and we cannot recover
     */
    @POST
    @Path("/{jobId}")
    @Consumes({MediaType.APPLICATION_FORM_URLENCODED, MediaType.APPLICATION_JSON,
        MediaType.APPLICATION_OCTET_STREAM})
    @Produces(MediaType.APPLICATION_JSON)
    public Response streamData(@Context HttpHeaders headers,
            @PathParam("jobId") String jobId, InputStream input,
            @DefaultValue("") @QueryParam(RESET_START_PARAM) String resetStart,
            @DefaultValue("") @QueryParam(RESET_END_PARAM) String resetEnd)
    throws IOException
    {
        LOGGER.debug("Post data to job(s) " + jobId);

        String [] jobIds = jobId.split(JOB_SEPARATOR);
        MultiDataPostResult result = new MultiDataPostResult();

        DataLoadParams params;
        String contentEncoding;

        // Validate request parameters
        try
        {
            params = createDataLoadParams(resetStart, resetEnd);
            contentEncoding = headers.getHeaderString(HttpHeaders.CONTENT_ENCODING);
            if (params.isResettingBuckets())
            {
                checkBucketResettingIsSupported(jobId);
            }
        }
        catch (JobException e)
        {
            for (String job : jobIds)
            {
                ApiError error = MapperUtils.apiErrorFromJobException(e);
                result.addResult(new DataPostResponse(job, error));
            }

            return Response.status(Response.Status.BAD_REQUEST).entity(result).build();
        }

        // remove duplicate and empty job ids
        Set<String> idSet = new HashSet<>(Arrays.asList(jobIds));
        idSet.remove(null);
        idSet.remove("");

        if (idSet.size() == 0)
        {
            return Response.status(Response.Status.BAD_REQUEST).entity(result).build();
        }
        else if (idSet.size() == 1)
        {
            result = streamToSingleJob(idSet.iterator().next(), params, contentEncoding, input);
        }
        else
        {
            result = streamToMultipleJobs(idSet, params, contentEncoding, input);
        }

        Response.Status statusCode = statusCodeForResponse(result);
        return Response.status(statusCode).entity(result).build();
    }

    private MultiDataPostResult streamToSingleJob(String jobId, DataLoadParams params,
            String contentEncoding, InputStream input)
     throws IOException
    {
        DataStreamer dataStreamer = new DataStreamer(jobManager());
        MultiDataPostResult result = new MultiDataPostResult();

        try
        {
            DataCounts stats = dataStreamer.streamData(contentEncoding, jobId, input, params);
            result.addResult(new DataPostResponse(jobId, stats));
        }
        catch (JobException e)
        {
            ApiError error = MapperUtils.apiErrorFromJobException(e);
            result.addResult(new DataPostResponse(jobId, error));
        }

        return result;
    }

    private MultiDataPostResult streamToMultipleJobs(Collection<String> jobIds, DataLoadParams params,
                                            String contentEncoding, InputStream input)
    throws IOException
    {
        InputStreamDuplicator duplicator = new InputStreamDuplicator(input);

        List<DataStreamerThread> threads = new ArrayList<>();
        for (String job : jobIds)
        {
            InputStream duplicateInput = duplicator.createDuplicateStream();

            DataStreamer dataStreamer = new DataStreamer(jobManager());
            DataStreamerThread thread = new DataStreamerThread(dataStreamer, job, contentEncoding,
                                                                params, duplicateInput);
            threads.add(thread);
            thread.start();
        }

        duplicator.duplicate();

        for (DataStreamerThread thread : threads)
        {
            try
            {
                thread.join();
            }
            catch (InterruptedException e)
            {
                LOGGER.warn("Interrupted joining DataStreamerThread", e);
            }
        }

        MultiDataPostResult results = new MultiDataPostResult();
        for (DataStreamerThread thread : threads)
        {
            results.addResult(toDataPostResult(thread));
        }

        return results;
    }

    private Response.Status statusCodeForResponse(MultiDataPostResult result)
    {
        Response.Status status = Response.Status.ACCEPTED;

        if (result.anErrorOccurred())
        {
            status = Response.Status.BAD_REQUEST;
            for (DataPostResponse response : result.getResponses())
            {
                if (response.getError() != null &&
                        response.getError().getErrorCode() == ErrorCodes.MISSING_JOB_ERROR)
                {
                    status = Response.Status.NOT_FOUND;
                }
            }
        }

        return status;
    }

    DataLoadParams createDataLoadParams(String resetStart, String resetEnd)
    {
        if (!isValidTimeRange(resetStart, resetEnd))
        {
            String msg = Messages.getMessage(Messages.REST_INVALID_RESET_PARAMS, RESET_START_PARAM);
            LOGGER.info(msg);
            throw new InvalidParametersException(msg, ErrorCodes.INVALID_BUCKET_RESET_RANGE_PARAMS);
        }
        TimeRange timeRange = createTimeRange(RESET_START_PARAM, resetStart, RESET_END_PARAM, resetEnd);
        return new DataLoadParams(shouldPersist(), timeRange);
    }

    private boolean isValidTimeRange(String start, String end)
    {
        return !start.isEmpty() || (start.isEmpty() && end.isEmpty());
    }

    TimeRange createTimeRange(String startParam, String start, String endParam, String end)
    {
        Long epochStart = null;
        Long epochEnd = null;
        if (!start.isEmpty())
        {
            epochStart = paramToEpochIfValidOrThrow(startParam, start, LOGGER) / MILLISECONDS_IN_SECOND;
            epochEnd = paramToEpochIfValidOrThrow(endParam, end, LOGGER) / MILLISECONDS_IN_SECOND;
            if (end.isEmpty() || epochEnd.equals(epochStart))
            {
                epochEnd = epochStart + 1;
            }
            if (epochEnd.longValue() < epochStart.longValue())
            {
                String msg = Messages.getMessage(Messages.REST_START_AFTER_END, end, start);
                LOGGER.info(msg);
                throw new InvalidParametersException(msg, ErrorCodes.END_DATE_BEFORE_START_DATE);
            }
        }
        return new TimeRange(epochStart, epochEnd);
    }

    private void checkBucketResettingIsSupported(String jobId) throws UnknownJobException
    {
        Optional<JobDetails> job = jobManager().getJob(jobId);
        AnalysisConfig config = job.get().getAnalysisConfig();
        checkLatencyIsNonZero(config.getLatency());
    }

    void checkLatencyIsNonZero(Long latency)
    {
        if (latency == null || latency.longValue() == 0)
        {
            String message = Messages.getMessage(Messages.REST_RESET_BUCKET_NO_LATENCY);
            LOGGER.info(message);
            throw new InvalidParametersException(message, ErrorCodes.BUCKET_RESET_NOT_SUPPORTED);
        }
    }

    /**
     * Calling this endpoint ensures that the native process has received all
     * data that has been previously uploaded, and that none is being buffered.
     * At present a flush is not permitted while a data upload in another
     * thread is part way through.
     * @param jobId
     * @param calcInterim Should interim results be calculated based on the data
     * up to the point of the flush?
     * @return
     * @throws UnknownJobException
     * @throws NativeProcessRunException
     * @throws JobInUseException if a data upload is part way through
     */
    @Path("/{jobId}/flush")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response flushUpload(@PathParam("jobId") String jobId,
            @DefaultValue("false") @QueryParam(CALC_INTERIM_PARAM) boolean calcInterim,
            @DefaultValue("") @QueryParam(START_QUERY_PARAM) String start,
            @DefaultValue("") @QueryParam(END_QUERY_PARAM) String end)
    throws UnknownJobException, NativeProcessRunException, JobInUseException
    {
        LOGGER.debug("Post to flush data upload for job " + jobId +
                     " with " + CALC_INTERIM_PARAM + '=' + calcInterim);
        checkValidFlushArgumentsCombination(calcInterim, start, end);
        TimeRange timeRange = createTimeRange(START_QUERY_PARAM, start, END_QUERY_PARAM, end);
        jobManager().flushJob(jobId, new InterimResultsParams(calcInterim, timeRange));
        return Response.ok().entity(new Acknowledgement()).build();
    }

    private void checkValidFlushArgumentsCombination(boolean calcInterim, String start, String end)
    {
        if (calcInterim == false && (!start.isEmpty() || !end.isEmpty()))
        {
            String msg = Messages.getMessage(Messages.REST_INVALID_FLUSH_PARAMS_UNEXPECTED,
                                START_QUERY_PARAM, END_QUERY_PARAM);
            throwInvalidFlushParamsException(msg);
        }
        if (!isValidTimeRange(start, end))
        {
            String msg = Messages.getMessage(Messages.REST_INVALID_FLUSH_PARAMS_MISSING,
                                            START_QUERY_PARAM);
            throwInvalidFlushParamsException(msg);
        }
    }

    private void throwInvalidFlushParamsException(String msg)
    {
        LOGGER.info(msg);
        throw new InvalidParametersException(msg, ErrorCodes.INVALID_FLUSH_PARAMS);
    }

    /**
     * Calling this endpoint indicates that data transfer is complete.
     * The job is retired and cleaned up after this
     * @param jobId
     * @return
     * @throws UnknownJobException
     * @throws NativeProcessRunException
     * @throws JobInUseException
     */
    @Path("/{jobId}/close")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response commitUpload(@PathParam("jobId") String jobId)
    throws UnknownJobException, NativeProcessRunException, JobInUseException
    {
        LOGGER.debug("Post to close data upload for job " + jobId);
        jobManager().closeJob(jobId);
        LOGGER.debug("Process finished successfully, Job Id = '" + jobId + "'");
        return Response.ok().entity(new Acknowledgement()).build();
    }


    public DataPostResponse toDataPostResult(DataStreamerThread dataStreamer)
    {
        if (dataStreamer.getDataCounts() != null)
        {
            return new DataPostResponse(dataStreamer.getJobId(), dataStreamer.getDataCounts());
        }
        else if (dataStreamer.getJobException().isPresent())
        {
            JobException e = dataStreamer.getJobException().get();
            return new DataPostResponse(dataStreamer.getJobId(),
                                        MapperUtils.apiErrorFromJobException(e));
        }
        else
        {
            ApiError error = new ApiError();
            if (dataStreamer.getIOException().isPresent())
            {
                IOException e = dataStreamer.getIOException().get();
                error.setErrorCode(ErrorCodes.UNKNOWN_ERROR);
                error.setMessage(e.getMessage());
                if (e.getCause() != null)
                {
                    error.setCause(e.getCause().toString());
                }
                else
                {
                    error.setCause(e.toString());
                }
            }

            return new DataPostResponse(dataStreamer.getJobId(), error);
        }
    }

    protected abstract boolean shouldPersist();
}
