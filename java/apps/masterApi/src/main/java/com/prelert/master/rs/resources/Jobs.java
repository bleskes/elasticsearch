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

package com.prelert.master.rs.resources;

import java.io.IOException;
import java.util.Arrays;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import com.prelert.job.JobConfiguration;
import com.prelert.job.JobException;
import com.prelert.job.UnknownJobException;
import com.prelert.job.config.verification.JobConfigurationException;
import com.prelert.job.config.verification.JobConfigurationVerifier;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.persistence.DataStoreException;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.scheduler.CannotStopSchedulerException;
import com.prelert.rs.client.EngineApiClient;
import com.prelert.rs.data.Acknowledgement;
import com.prelert.settings.PrelertSettings;


/**
 *
 */
@Path("/jobs")
public class Jobs
{
    private static final Logger LOGGER = Logger.getLogger(Jobs.class);

    /**
     * The name of this endpoint
     */
    public static final String ENDPOINT = "jobs";

    private String [] m_EngineUrls = {};
    private int m_JobsPerNode;

    public Jobs()
    {
        readSettings();
        LOGGER.info(Arrays.asList(m_EngineUrls));
    }



    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createJob(@DefaultValue("false") @QueryParam("overwrite") boolean overwrite,
                                JobConfiguration config)
    throws JobConfigurationException
    {
        LOGGER.debug("Creating new job");

        // Only allow one job creation at any time - this avoids potential race
        // conditions with too many jobs due to licensing or number of CPU cores
        // when overwriting
        synchronized (ENDPOINT)
        {
            // throws if a bad config
            try
            {
                JobConfigurationVerifier.verify(config);
            }
            catch (JobConfigurationException e)
            {
                // log error and rethrow
                LOGGER.error("Bad job configuration: " + e.getMessage());
                throw e;
            }
        }

        if (config.getId() == null || config.getId().isEmpty())
        {
            String msg = "A Job ID must be specified";
            LOGGER.warn(msg);
            throw new JobConfigurationException(msg, ErrorCodes.PROHIBITIED_CHARACTER_IN_JOB_ID);
        }

        createSubJobs(config);
        return Response.ok().entity(new Acknowledgement()).build();
    }

    @DELETE
    @Path("/{jobId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteJob(@PathParam("jobId") String jobId) throws UnknownJobException,
            NativeProcessRunException, JobInUseException, DataStoreException,
            CannotStopSchedulerException, JobException
    {
        LOGGER.debug("Delete job '" + jobId + "'");

        if (deleteSuperJob(jobId))
        {
            LOGGER.debug("Job '" + jobId + "' deleted");
            return Response.ok().entity(new Acknowledgement()).build();
        }
        else
        {
            String msg = "Error deleting jobs";
            LOGGER.warn(msg);

            return Response.status(Response.Status.NOT_FOUND).entity(msg).build();
        }
    }

    private boolean createSubJobs(JobConfiguration config) throws JobConfigurationException
    {
        String jobId = config.getId();

        StringBuilder errors = new StringBuilder();

        boolean allCreated = true;

        int subJobIndex = 1;
        for (String url : m_EngineUrls)
        {
            try (EngineApiClient client = new EngineApiClient(url))
            {
                for (int i=0; i<m_JobsPerNode; i++)
                {
                    String subJobId = subJobId(jobId, subJobIndex++);
                    config.setId(subJobId);
                    boolean created = client.createJob(config).isEmpty() == false;
                    if (!created)
                    {
                        String lastError = client.getLastError() == null ? "" : client.getLastError().toJson();
                        String msg = "Failed to create job " + subJobId + lastError;
                        LOGGER.warn(msg);
                        errors.append(msg).append("\n");
                        allCreated = false;
                    }
                }
            }
            catch (IOException e)
            {
                LOGGER.error("Error creating sub job", e);

                throw new JobConfigurationException("Not all sub jobs could be created",
                        ErrorCodes.INVALID_VALUE, e);
            }

            if (allCreated == false)
            {
                throw new JobConfigurationException(
                        "Not all sub jobs could be created\n" + errors.toString(),
                                                    ErrorCodes.INVALID_VALUE);
            }
        }

        return allCreated;
    }


    private boolean deleteSuperJob(String jobId) throws JobConfigurationException
    {
        boolean allDeleted = true;
        StringBuilder errors = new StringBuilder();

        int subJobIndex = 1;
        for (String url : m_EngineUrls)
        {
            try (EngineApiClient client = new EngineApiClient(url))
            {
                for (int i=0; i<m_JobsPerNode; i++)
                {
                    String subJobId = subJobId(jobId, subJobIndex++);

                    try
                    {
                        boolean deleted = client.deleteJob(subJobId);
                        if (!deleted)
                        {
                            String lastError = client.getLastError() == null ? "" : client.getLastError().toJson();
                            String msg = "Failed to delete job " + subJobId + lastError;
                            LOGGER.warn(msg);
                            errors.append(msg).append("\n");
                            allDeleted = false;
                        }
                    }
                    catch (IOException e)
                    {
                        LOGGER.error("Error deleting sub job " + subJobId, e);
                        allDeleted = false;
                        errors.append(e.getMessage());
                    }
                }

            }
            catch (IOException e)
            {
                LOGGER.error("Error closing client", e);
            }
        }

        if (allDeleted == false)
        {
            throw new JobConfigurationException("Not all sub jobs could be delete\n" + errors.toString(),
                                                ErrorCodes.INVALID_VALUE);
        }

        return allDeleted;
    }

    private String subJobId(String jobId, int index)
    {
        return String.format("%s-%03d", jobId, index);
    }

    private void readSettings()
    {
        String commaSeparatedUrls = PrelertSettings.getSettingOrDefault("engine.urls", "");
        if (commaSeparatedUrls.isEmpty())
        {
            return;
        }

        m_EngineUrls = commaSeparatedUrls.split(",");

        m_JobsPerNode = PrelertSettings.getSettingOrDefault("jobs.per.node", 2);
    }
}
