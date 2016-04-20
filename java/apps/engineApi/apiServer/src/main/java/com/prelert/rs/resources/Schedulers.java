/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2016     *
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


import java.util.OptionalLong;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import com.prelert.job.UnknownJobException;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.exceptions.LicenseViolationException;
import com.prelert.job.exceptions.TooManyJobsException;
import com.prelert.job.manager.NoSuchScheduledJobException;
import com.prelert.job.messages.Messages;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.scheduler.CannotStartSchedulerException;
import com.prelert.job.scheduler.CannotStopSchedulerException;
import com.prelert.rs.data.Acknowledgement;
import com.prelert.rs.exception.InvalidParametersException;


/**
 * API scheduler management end point.
 * Allows start/end of a job scheduler.
 */
@Path("/schedulers")
public class Schedulers extends ResourceWithJobManager
{
    private static final Logger LOGGER = Logger.getLogger(Schedulers.class);

    /**
     * The name of this endpoint
     */
    public static final String ENDPOINT = "schedulers";

    /**
     * Starts the scheduler for the given job
     *
     * @param jobId
     * @param start the time from which scheduler should start/resume
     * @param end the time on which the scheduler will stop. Empty means real-time
     * @return
     * @throws UnknownJobException
     * @throws NoSuchScheduledJobException
     * @throws CannotStartSchedulerException
     * @throws LicenseViolationException
     * @throws TooManyJobsException
     */
    @POST
    @Path("/{jobId}/start")
    @Produces(MediaType.APPLICATION_JSON)
    public Response startScheduledJob(@PathParam("jobId") String jobId,
            @DefaultValue("") @QueryParam(START_QUERY_PARAM) String start,
            @DefaultValue("") @QueryParam(END_QUERY_PARAM) String end)
            throws CannotStartSchedulerException, NoSuchScheduledJobException,
            UnknownJobException, TooManyJobsException, LicenseViolationException
    {
        LOGGER.debug("Received request to start scheduler for job: " + jobId);

        long startEpochMs = paramToEpochIfValidOrThrow(START_QUERY_PARAM, start, LOGGER);
        OptionalLong endEpochMs = OptionalLong.empty();
        if (!end.isEmpty())
        {
            endEpochMs = OptionalLong.of(paramToEpochIfValidOrThrow(END_QUERY_PARAM, end, LOGGER));

            if (endEpochMs.getAsLong() <= startEpochMs)
            {
                String msg = Messages.getMessage(Messages.REST_START_AFTER_END, end, start);
                throw new InvalidParametersException(msg, ErrorCodes.END_DATE_BEFORE_START_DATE);
            }
        }

        jobManager().startJobScheduler(jobId, startEpochMs, endEpochMs);
        return Response.ok(new Acknowledgement()).build();
    }

    /**
     * Stops the scheduler for the given job
     *
     * @param jobId
     * @return
     * @throws NoSuchScheduledJobException
     * @throws CannotStopSchedulerException
     * @throws JobInUseException
     * @throws NativeProcessRunException
     * @throws UnknownJobException
     */
    @POST
    @Path("/{jobId}/stop")
    @Produces(MediaType.APPLICATION_JSON)
    public Response stopScheduledJob(@PathParam("jobId") String jobId)
            throws NoSuchScheduledJobException, CannotStopSchedulerException, UnknownJobException,
            NativeProcessRunException, JobInUseException
    {
        LOGGER.debug("Received request to stop scheduler for job: " + jobId);
        jobManager().stopJobScheduler(jobId);
        return Response.ok(new Acknowledgement()).build();
    }
}
