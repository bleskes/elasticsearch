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

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import com.prelert.job.JobConfiguration;
import com.prelert.job.JobDetails;
import com.prelert.job.JobException;
import com.prelert.job.JobIdAlreadyExistsException;
import com.prelert.job.UnknownJobException;
import com.prelert.job.config.verification.JobConfigurationException;
import com.prelert.job.config.verification.JobConfigurationVerifier;
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.exceptions.LicenseViolationException;
import com.prelert.job.logs.JobLogs;
import com.prelert.job.manager.CannotPauseJobException;
import com.prelert.job.manager.CannotResumeJobException;
import com.prelert.job.persistence.DataStoreException;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.reader.JobDataReader;
import com.prelert.job.scheduler.CannotStartSchedulerException;
import com.prelert.job.scheduler.CannotStopSchedulerException;
import com.prelert.rs.data.Acknowledgement;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.data.SingleDocument;
import com.prelert.rs.job.update.JobUpdater;
import com.prelert.rs.validation.PaginationParamsValidator;


/**
 * REST API Jobs end point use to create new Jobs list all jobs or get
 * details of a particular job.
 * <br>
 * Jobs are created by POSTing to this endpoint:<br>
 * <pre>curl -X POST -H 'Content-Type: application/json' 'http://localhost:8080/api/jobs'</pre>
 * Get details of a specific job:<br>
 * <pre>curl 'http://localhost:8080/api/jobs/{job_id}'</pre>
 * or all jobs:<br>
 * <pre>curl 'http://localhost:8080/api/jobs'</pre>
 * Delete a job with:<br>
 * <pre>curl -X DELETE 'http://localhost:8080/api/jobs/{job_id}'</pre>
 */

@Path("/jobs")
public class Jobs extends ResourceWithJobManager
{
    private static final Logger LOGGER = Logger.getLogger(Jobs.class);

    /**
     * The name of this endpoint
     */
    public static final String ENDPOINT = "jobs";

    /**
     * Message returned if deletion of a job fails
     */
    private static final String RESULTS = "results";

    /**
     * Get all job details.
     *
     * @return Array of JSON objects string
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Pagination<JobDetails> jobs(
            @DefaultValue("0") @QueryParam("skip") int skip,
            @DefaultValue(DEFAULT_PAGE_SIZE_STR) @QueryParam("take") int take)
    {
        LOGGER.debug(String.format("Get all jobs, skip=%d, take=%d", skip, take));

        new PaginationParamsValidator(skip, take).validate();

        JobDataReader reader = jobReader();
        Pagination<JobDetails> results = this.paginationFromQueryPage(
                                                        reader.getJobs(skip, take), skip, take);

        setPagingUrls(ENDPOINT, results);

        for (JobDetails job : results.getDocuments())
        {
            setEndPointLinks(job);
        }

        LOGGER.debug(String.format("Returning %d of %d jobs",
                results.getDocuments().size(), results.getHitCount()));

        return results;
    }

    @GET
    @Path("/{jobId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response job(@PathParam("jobId") String jobId)
    {
        LOGGER.debug("Get job '" + jobId + "'");

        JobDataReader reader = jobReader();
        SingleDocument<JobDetails> job;

        Optional<JobDetails> result = reader.getJob(jobId);
        if (result.isPresent())
        {
            job = singleDocFromOptional(result, JobDetails.TYPE);
            setEndPointLinks(job.getDocument());
        }
        else
        {
            job = new SingleDocument<>();
            job.setType(JobDetails.TYPE);
        }

        if (job.isExists())
        {
            LOGGER.debug("Returning job '" + jobId + "'");

            return Response.ok(job).build();
        }
        else
        {
            LOGGER.debug(String.format("Cannot find job '%s'", jobId));

            return Response.status(Response.Status.NOT_FOUND).entity(job).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createJob(@DefaultValue("false") @QueryParam("overwrite") boolean overwrite,
                                JobConfiguration config)
    throws UnknownJobException,
            JobConfigurationException, IOException, LicenseViolationException,
            JobIdAlreadyExistsException, CannotStartSchedulerException,
            DataStoreException, NativeProcessRunException, JobInUseException,
            CannotStopSchedulerException
    {
        LOGGER.debug("Creating new job");

        JobDetails job = null;
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
                LOGGER.error("Bad job configuration ", e);
                throw e;
            }

            job = jobManager().createJob(config, overwrite);
        }
        if (job == null)
        {
            LOGGER.debug("Failed to create job");
            return Response.serverError().build();
        }

        setEndPointLinks(job);

        LOGGER.debug("Returning new job details location " + job.getLocation());
        String ent = String.format("{\"id\":\"%s\"}\n", job.getId());

        return Response.created(job.getLocation()).entity(ent).build();
    }

    @PUT
    @Path("{jobId}/update")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateJob(@PathParam("jobId") String jobId, String updateJson)
            throws JobException
    {
        return new JobUpdater(jobManager(), jobId).update(updateJson);
    }

    /**
     * Delete the job.
     *
     * @param jobId
     * @return
     * @throws NativeProcessRunException If there is an error deleting the job
     * @throws UnknownJobException If the job id is not known
     * @throws JobInUseException If the job cannot be deleted because the
     * native process is in use.
     * @throws DataStoreException
     * @throws CannotStopSchedulerException If the job is scheduled and its scheduler fails to stop
     */
    @DELETE
    @Path("/{jobId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteJob(@PathParam("jobId") String jobId) throws UnknownJobException,
            NativeProcessRunException, JobInUseException, DataStoreException,
            CannotStopSchedulerException, JobException
    {
        LOGGER.debug("Delete job '" + jobId + "'");

        boolean deleted = jobManager().deleteJob(jobId);

        if (deleted)
        {
            new JobLogs().deleteLogs(jobId);

            LOGGER.debug("Job '" + jobId + "' deleted");
            return Response.ok().entity(new Acknowledgement()).build();
        }
        else
        {
            String msg = "Error deleting job '" + jobId + "'";
            LOGGER.warn(msg);

            return Response.status(Response.Status.NOT_FOUND).entity(new Acknowledgement(false)).build();
        }
    }

    @POST
    @Path("/{jobId}/pause")
    @Produces(MediaType.APPLICATION_JSON)
    public Response pauseJob(@PathParam("jobId") String jobId) throws JobInUseException,
            NativeProcessRunException, UnknownJobException, CannotPauseJobException
    {
        checkJobIsNotScheduled(jobId);
        jobManager().pauseJob(jobId);
        return Response.ok().entity(new Acknowledgement()).build();
    }

    @POST
    @Path("/{jobId}/resume")
    @Produces(MediaType.APPLICATION_JSON)
    public Response resumeJob(@PathParam("jobId") String jobId)
            throws JobInUseException, UnknownJobException, CannotResumeJobException
    {
        checkJobIsNotScheduled(jobId);
        jobManager().resumeJob(jobId);
        return Response.ok().entity(new Acknowledgement()).build();
    }

    /**
     * Sets the URLs to the data, logs & results endpoints and the
     * location of this job
     * @param job
     */
    private void setEndPointLinks(JobDetails job)
    {
        URI location = m_UriInfo.getBaseUriBuilder()
                .path(ENDPOINT)
                .path(job.getId())
                .build();
        job.setLocation(location);

        Map<String, URI> endpoints = new HashMap<>();

        URI data = m_UriInfo.getBaseUriBuilder()
                .path(Data.ENDPOINT)
                .path(job.getId())
                .build();
        endpoints.put(JobDetails.DATA_ENDPOINT_KEY, data);

        URI buckets = m_UriInfo.getBaseUriBuilder()
                .path(RESULTS)
                .path(job.getId())
                .path(Buckets.ENDPOINT)
                .build();
        endpoints.put(JobDetails.BUCKETS_ENDPOINT_KEY, buckets);

        URI categoryDefinitions = m_UriInfo.getBaseUriBuilder()
                .path(RESULTS)
                .path(job.getId())
                .path(CategoryDefinitions.ENDPOINT)
                .build();
        endpoints.put(JobDetails.CATEGORY_DEFINITIONS_ENDPOINT_KEY, categoryDefinitions);

        URI records = m_UriInfo.getBaseUriBuilder()
                .path(RESULTS)
                .path(job.getId())
                .path(Records.ENDPOINT)
                .build();
        endpoints.put(JobDetails.RECORDS_ENDPOINT_KEY, records);

        URI logs = m_UriInfo.getBaseUriBuilder()
                .path(Logs.ENDPOINT)
                .path(job.getId())
                .build();
        endpoints.put(JobDetails.LOGS_ENDPOINT_KEY, logs);

        URI longpoll = m_UriInfo.getBaseUriBuilder()
                .path(AlertsLongPoll.ENDPOINT)
                .path(job.getId())
                .build();
        endpoints.put(JobDetails.ALERT_LONG_POLL_ENDPOINT_KEY, longpoll);

        URI modelSnapshots = m_UriInfo.getBaseUriBuilder()
                .path(ModelSnapshots.ENDPOINT)
                .path(job.getId())
                .build();
        endpoints.put(JobDetails.MODEL_SNAPSHOTS_ENDPOINT_KEY, modelSnapshots);

        job.setEndpoints(endpoints);
    }
}
