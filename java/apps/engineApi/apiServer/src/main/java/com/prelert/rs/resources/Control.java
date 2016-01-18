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


import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import com.prelert.job.UnknownJobException;
import com.prelert.job.manager.CannotStartSchedulerWhileItIsStoppingException;
import com.prelert.job.manager.NoSuchScheduledJobException;
import com.prelert.rs.data.Acknowledgement;


/**
 * Control REST paths are operations that allow the user
 * to control actions with regard to the operations of the engine.
 */
@Path("/control")
public class Control extends ResourceWithJobManager
{
    private static final Logger LOGGER = Logger.getLogger(Control.class);

    /**
     * The name of this endpoint
     */
    public static final String ENDPOINT = "control";

    /**
     * Starts the scheduler for the given job
     *
     * @param jobId
     * @return
     * @throws UnknownJobException
     * @throws NoSuchScheduledJobException
     * @throws CannotStartSchedulerWhileItIsStoppingException
     */
    @POST
    @Path("/scheduler/{jobId}/start")
    @Produces(MediaType.APPLICATION_JSON)
    public Response startScheduledJob(@PathParam("jobId") String jobId)
            throws CannotStartSchedulerWhileItIsStoppingException, NoSuchScheduledJobException
    {
        LOGGER.debug("Received request to start scheduler for job: " + jobId);
        jobManager().startExistingJobScheduler(jobId);
        return Response.ok(new Acknowledgement()).build();
    }

    /**
     * Stops the scheduler for the given job
     *
     * @param jobId
     * @return
     * @throws UnknownJobException
     * @throws NoSuchScheduledJobException
     */
    @POST
    @Path("/scheduler/{jobId}/stop")
    @Produces(MediaType.APPLICATION_JSON)
    public Response stopScheduledJob(@PathParam("jobId") String jobId)
            throws NoSuchScheduledJobException
    {
        LOGGER.debug("Received request to stop scheduler for job: " + jobId);
        jobManager().stopExistingJobScheduler(jobId);
        return Response.ok(new Acknowledgement()).build();
    }
}
