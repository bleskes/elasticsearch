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
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataCounts;
import com.prelert.job.Detector;
import com.prelert.job.JobDetails;
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.exceptions.TooManyJobsException;
import com.prelert.job.exceptions.UnknownJobException;
import com.prelert.job.process.exceptions.MalformedJsonException;
import com.prelert.job.process.exceptions.MissingFieldException;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.process.params.DataLoadParams;
import com.prelert.job.process.params.InterimResultsParams;
import com.prelert.job.process.params.TimeRange;
import com.prelert.job.status.HighProportionOfBadTimestampsException;
import com.prelert.job.status.OutOfOrderRecordsException;
import com.prelert.rs.data.Acknowledgement;
import com.prelert.rs.data.ErrorCode;
import com.prelert.rs.data.SingleDocument;
import com.prelert.rs.provider.RestApiException;
import com.prelert.rs.resources.data.DataStreamer;


public abstract class AbstractDataLoad extends ResourceWithJobManager
{
    private static final Logger LOGGER = Logger.getLogger(AbstractDataLoad.class);

    /**
     * Parameter to control whether interim results are calculated on a flush
     */
    public static final String CALC_INTERIM_PARAM = "calcInterim";

    /** Parameter to specify start time of buckets to be reset */
    private static final String RESET_START_PARAM = "resetStart";

    /** Parameter to specify end time of buckets to be reset */
    private static final String RESET_END_PARAM = "resetEnd";

    private static final int MILLISECONDS_IN_SECOND = 1000;

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
    public Response streamData(@Context HttpHeaders headers,
            @PathParam("jobId") String jobId, InputStream input,
            @DefaultValue("") @QueryParam(RESET_START_PARAM) String resetStart,
            @DefaultValue("") @QueryParam(RESET_END_PARAM) String resetEnd)
    throws IOException, UnknownJobException, NativeProcessRunException,
            MissingFieldException, JobInUseException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, TooManyJobsException, MalformedJsonException
    {
        DataLoadParams params = createDataLoadParams(resetStart, resetEnd);
        if (params.isResettingBuckets())
        {
            checkBucketResettingIsSupported(jobId);
        }

        DataStreamer dataStreamer = new DataStreamer(jobManager());
        String contentEncoding = headers.getHeaderString(HttpHeaders.CONTENT_ENCODING);
        DataCounts stats = dataStreamer.streamData(contentEncoding, jobId, input, params);
        return Response.accepted().entity(stats).build();
    }

    private DataLoadParams createDataLoadParams(String resetStart, String resetEnd)
    {
        if (!isValidTimeRange(resetStart, resetEnd))
        {
            String msg = String.format("Invalid reset range parameters: '%s' has not been specified.",
                    RESET_START_PARAM);
            throw new RestApiException(msg, ErrorCode.INVALID_BUCKET_RESET_RANGE_PARAMS,
                    Response.Status.BAD_REQUEST);
        }
        TimeRange timeRange = createTimeRange(RESET_START_PARAM, resetStart, RESET_END_PARAM, resetEnd);
        return new DataLoadParams(shouldPersist(), timeRange);
    }

    private boolean isValidTimeRange(String start, String end)
    {
        return !start.isEmpty() || (start.isEmpty() && end.isEmpty());
    }

    private TimeRange createTimeRange(String startParam, String start, String endParam, String end)
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
                String msg = String.format("Invalid time range: end time '%s' is earlier than start"
                        + " time '%s'.", end, start);
                throw new RestApiException(msg, ErrorCode.END_DATE_BEFORE_START_DATE,
                        Response.Status.BAD_REQUEST);
            }
        }
        return new TimeRange(epochStart, epochEnd);
    }

    private void checkBucketResettingIsSupported(String jobId) throws UnknownJobException
    {
        SingleDocument<JobDetails> job = jobManager().getJob(jobId);
        AnalysisConfig config = job.getDocument().getAnalysisConfig();
        checkLatencyIsNonZero(config.getLatency());
        checkAllDetectorsHaveBucketResetSupportingFunctions(config.getDetectors());
    }

    private void checkLatencyIsNonZero(Long latency)
    {
        if (latency == null || latency.longValue() == 0)
        {
            throw new RestApiException(
                    "Bucket resetting is not supported when no latency is configured.",
                    ErrorCode.BUCKET_RESET_NOT_SUPPORTED, Response.Status.BAD_REQUEST);
        }
    }

    private void checkAllDetectorsHaveBucketResetSupportingFunctions(List<Detector> detectors)
    {
        for (Detector detector : detectors)
        {
            if (!detector.isSupportingBucketResetting())
            {
                String function = detector.getFunction();
                function = function == null ? "metric" : function;
                String msg = String.format("At least one detector contains a function that does not"
                        + " support bucket resetting: %s.", function);
                throw new RestApiException(msg, ErrorCode.BUCKET_RESET_NOT_SUPPORTED,
                        Response.Status.BAD_REQUEST);
            }
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
            String msg = String.format("Invalid flush parameters: unexpected '%s' and/or '%s'.",
                    START_QUERY_PARAM, END_QUERY_PARAM);
            throwInvalidFlushParamsException(msg);
        }
        if (!isValidTimeRange(start, end))
        {
            String msg = String.format("Invalid flush parameters: '%s' has not been specified.",
                            START_QUERY_PARAM);
            throwInvalidFlushParamsException(msg);
        }
    }

    private void throwInvalidFlushParamsException(String msg)
    {
        throw new RestApiException(msg, ErrorCode.INVALID_FLUSH_PARAMS,
                Response.Status.BAD_REQUEST);
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
    public Response commitUpload(@PathParam("jobId") String jobId)
    throws UnknownJobException, NativeProcessRunException, JobInUseException
    {
        LOGGER.debug("Post to close data upload for job " + jobId);
        jobManager().finishJob(jobId);
        LOGGER.debug("Process finished successfully, Job Id = '" + jobId + "'");
        return Response.accepted().entity(new Acknowledgement()).build();
    }

    protected abstract boolean shouldPersist();
}
