/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Inc 2006-2014     *
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
 * on +44 (0)20 7953 7243 or email to legal@prelert.com.    *
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

import java.net.URI;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.prelert.job.JobConfiguration;
import com.prelert.job.NativeProcessRunException;
import com.prelert.job.UnknownJobException;
import com.prelert.job.JobDetails;
import com.prelert.job.JobManager;
import com.prelert.job.logs.JobLogs;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.data.SingleDocument;


/**
 * REST API Jobs end point use to create new Jobs list all jobs or get
 * details of a particular job.   
 * </br>
 * Jobs are created by POSTing to this endpoint:</br>
 * <pre>curl -X POST -H 'Content-Type: application/json' 'http://localhost:8080/api/jobs'</pre>
 * Get details of a specific job:</br> 
 * <pre>curl 'http://localhost:8080/api/jobs/{job_id}'</pre>
 * or all jobs:</br> 
 * <pre>curl 'http://localhost:8080/api/jobs'</pre>
 */

@Path("/jobs")
public class Jobs extends ResourceWithJobManager
{	
	static final private Logger s_Logger = Logger.getLogger(Jobs.class);
	
	/**
	 * The name of this endpoint
	 */
	static public final String ENDPOINT = "jobs";
	
	/**
	 * Get all job details.
	 * 
	 * @return Array of JSON objects stringn 
	 */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Pagination<JobDetails> jobs(
    		@DefaultValue("0") @QueryParam("skip") int skip,
    		@DefaultValue(JobManager.DEFAULT_PAGE_SIZE_STR) @QueryParam("take") int take) 
    {      
    	s_Logger.debug(String.format("Get all jobs, skip=%d, take=%d", skip, take));
    	
    	JobManager manager = jobManager();
    	Pagination<JobDetails> results = manager.getAllJobs(skip, take);
    	
    	setPagingUrls(ENDPOINT, results);
    	
    	for (JobDetails job : results.getDocuments())
    	{
    		setEndPointLinks(job);
    	}
    	
    	s_Logger.debug(String.format("Returning %d of %d jobs", 
    			results.getDocuments().size(), results.getHitCount()));
    	
    	return results;
    }
	
    @GET
	@Path("/{jobId}")
    @Produces(MediaType.APPLICATION_JSON)
    public SingleDocument<JobDetails> job(@PathParam("jobId") String jobId)
    throws UnknownJobException
    {   	
    	s_Logger.debug("Get job '" + jobId + "'");
    	
		JobManager manager = jobManager();
		SingleDocument<JobDetails> job = manager.getJob(jobId);
		
		if (job.isExists() == false)
		{
			s_Logger.debug("Job " + jobId + " not found");			
			throw new WebApplicationException(Response.Status.NOT_FOUND);
		}
		
		setEndPointLinks(job.getDocument());
		
		s_Logger.debug("Returning job '" + job + "'");
		return job;
    }
		
    @POST
    @Consumes(MediaType.APPLICATION_JSON)    
    public Response createJob(JobConfiguration config) 
    throws UnknownJobException, JsonProcessingException 
    {   		
    	s_Logger.debug("Creating new job");
    	
    	JobManager manager = jobManager();
    	JobDetails job = manager.createJob(config);
    	if (job == null)
    	{
    		s_Logger.debug("Failed to create job");
    		return Response.serverError().build();
    	}
    	
    	setEndPointLinks(job);
    	
    	s_Logger.debug("Returning new job details location " + job.getLocation());
    	String ent = String.format("{\"id\":\"%s\"}", job.getId()); 
    	
		return Response.created(job.getLocation()).entity(ent).build();
    }      
    
    
    /**
     * Delete the job.
     * 
     * @param jobId
     * @return
     * @throws NativeProcessRunException If there is an error deleting the job
     * @throws UnknownJobException If the job id is not known
     */
    @DELETE
	@Path("/{jobId}")
    public Response deleteJob(@PathParam("jobId") String jobId) 
    throws UnknownJobException, NativeProcessRunException
    {   	
    	s_Logger.debug("Delete job '" + jobId + "'");
    	
		JobManager manager = jobManager();
		boolean deleted = manager.deleteJob(jobId);
		
		if (deleted)
		{
			s_Logger.debug("Job '" + jobId + "' deleted");
			return Response.ok().build();
		}
		else
		{
			String msg = "Error deleting job '" + jobId + "'";
			s_Logger.warn(msg);
			
			return Response.status(Response.Status.NOT_FOUND).build();
		}
    }
    
    /**
     * Get the contents of the log directory zipped
     * 
     * @param jobId
     * @return
     * @throws UnknownJobException
     */
    @GET
	@Path("/{jobId}/logs")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response jobLogFiles(@PathParam("jobId") String jobId)
    throws UnknownJobException
    {   	
    	s_Logger.debug("Download logs for job '" + jobId + "'");
    	
		JobLogs logs = new JobLogs();
		byte [] compressFiles = logs.zippedLogFiles(jobId);
		
		String filename = jobId + "_logs.zip";
		
		return Response.ok(compressFiles)
				.header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
				.build();
    }
    
    /**
     * Tail the log file
     * 
     * @param jobId
     * @param lines Number of lines to tail
     * @return
     * @throws UnknownJobException
     */
    @GET
	@Path("/{jobId}/logs/tail")
    @Produces(MediaType.TEXT_PLAIN)
    public String tailLogFile(@PathParam("jobId") String jobId,
    		@DefaultValue("10") @QueryParam("lines") int lines)
    throws UnknownJobException
    {   	
    	s_Logger.debug("Tail log for job '" + jobId + "'");
    	
		JobLogs logs = new JobLogs();
		return logs.tail(jobId, lines);
    }
        
    /**
     * Sets the URLs to the streaming & results endpoints and the 
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
    	
    	URI streaming = m_UriInfo.getBaseUriBuilder()
				.path(ENDPOINT)
				.path(job.getId())
				.path(Streaming.ENDPOINT)
				.build();
    	job.setStreamingEndpoint(streaming);
    	
    	URI results = m_UriInfo.getBaseUriBuilder()
				.path(ENDPOINT)
				.path(job.getId())
				.path(Results.ENDPOINT)
				.build();
    	job.setResultsEndpoint(results);    	
    	
    }
}
