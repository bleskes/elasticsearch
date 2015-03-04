/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2014     *
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

import com.prelert.job.JobInUseException;
import com.prelert.job.TooManyJobsException;
import com.prelert.job.UnknownJobException;
import com.prelert.job.process.exceptions.MalformedJsonException;
import com.prelert.job.process.exceptions.MissingFieldException;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.status.HighProportionOfBadTimestampsException;
import com.prelert.job.status.OutOfOrderRecordsException;
import com.prelert.rs.resources.data.AbstractDataStreamer;
import com.prelert.rs.resources.data.DataStreamerAndPersister;


/**
 * Streaming and persisting dataload endpoint
 *
 * <pre>curl -X POST 'http://localhost:8080/api/dataload/<jobid>/' --data @<filename></pre>
 * <br/>
 * Binary gzipped files must be POSTed with the --data-binary option
 * <pre>curl -X POST 'http://localhost:8080/api/dataload/<jobid>/' --data-binary @<filename.gz></pre>
 *
 */
@Path("/dataload")
public class DataLoad extends ResourceWithJobManager
{

    private static final Logger LOGGER = Logger.getLogger(DataLoad.class);

    /**
     * The name of this endpoint
     */
    public static final String ENDPOINT = "dataload";

    /**
     * Parameter to control whether interim results are calculated on a flush
     */
    public static final String CALC_INTERIM_PARAM = "calcInterim";

    /**
     * Data upload and persist endpoint.
     *
     * @param headers
     * @param jobId
     * @param input
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
    public Response streamAndPersistData(@Context HttpHeaders headers,
            @PathParam("jobId") String jobId, InputStream input)
    throws IOException, UnknownJobException, NativeProcessRunException,
            MissingFieldException, JobInUseException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, TooManyJobsException, MalformedJsonException
    {
        AbstractDataStreamer dataStreamer = new DataStreamerAndPersister(jobManager());
        String contentEncoding = headers.getHeaderString(HttpHeaders.CONTENT_ENCODING);
        dataStreamer.streamData(contentEncoding, jobId, input);
        return Response.accepted().build();
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
            @DefaultValue("false") @QueryParam(CALC_INTERIM_PARAM) boolean calcInterim)
    throws UnknownJobException, NativeProcessRunException, JobInUseException
    {
        LOGGER.debug("Post to flush data upload for job " + jobId +
                     " with " + CALC_INTERIM_PARAM + '=' + calcInterim);
        jobManager().flushJob(jobId, calcInterim);
        return Response.ok().build();
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
        return Response.accepted().build();
    }
}
