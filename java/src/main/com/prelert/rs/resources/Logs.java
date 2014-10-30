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

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import com.prelert.job.UnknownJobException;
import com.prelert.job.logs.JobLogs;


/**
 * REST API logs end point use to download or tail the latest job log files.
 * The <pre>zip</pre> endpoint returns a file attachment of the zipped log files,
 * <pre>tail</pre> returns the last 10 lines of text of the latest log file but
 * accepts the <pre>lines</pre> query parameter if more than the last 10 lines
 * are wanted. 
 */
@Path("/logs")
public class Logs extends ResourceWithJobManager
{	
	static final private Logger s_Logger = Logger.getLogger(Logs.class);
	
	/**
	 * The name of this endpoint
	 */
	static public final String ENDPOINT = "logs";
	
    /**
     * Get the contents of the log directory zipped. 
     * This is the same as {@link #zipLogFiles(String)} 
     * 
     * @param jobId
     * @return
     * @throws UnknownJobException
     */
    @GET
	@Path("/{jobId}/")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response jobLogFiles(@PathParam("jobId") String jobId)
    throws UnknownJobException
    {   	
    	return zipJobLogFiles(jobId);
    }
    
    
    /**
     * Get the contents of the log directory zipped
     * This is the same as {@link #jobLogFiles(String)} 
     * 
     * @param jobId
     * @return
     * @throws UnknownJobException
     */
    @GET
	@Path("/{jobId}/zip")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response zipLogFiles(@PathParam("jobId") String jobId)
    throws UnknownJobException
    {   	
    	return zipJobLogFiles(jobId);
    }
    
    /**
     * Return a response with a file attachment of the zipped log files.
     * @param jobId
     * @return
     * @throws UnknownJobException
     */
    private Response zipJobLogFiles(String jobId)
    throws UnknownJobException
    {   	
    	s_Logger.debug("Get zipped logs for job '" + jobId + "'");
    	
		JobLogs logs = new JobLogs();
		byte [] compressFiles = logs.zippedLogFiles(jobId);
		
		String filename = jobId + "_logs.zip";
		
		return Response.ok(compressFiles)
				.header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
				.build();
    }
    
    /**
     * Tail the log file with the same name as the job id.
     * 
     * @param jobId
     * @param lines Number of lines to tail
     * @return
     * @throws UnknownJobException
     */
    @GET
	@Path("/{jobId}/tail")
    @Produces(MediaType.TEXT_PLAIN)
    public String tailDefaultLogFile(@PathParam("jobId") String jobId,
    		@DefaultValue("10") @QueryParam("lines") int lines)
    throws UnknownJobException
    {   	
    	s_Logger.debug("Tail log for job '" + jobId + "'");
    	
		JobLogs logs = new JobLogs();
		return logs.tail(jobId, lines);
    }
    
    /**
     * Read the entire log file
     * 
     * @param jobId
     * @param lines Number of lines to tail
     * @return
     * @throws UnknownJobException
     * @throws IOException 
     */
    @GET
	@Path("/{jobId}/{filename}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getLogFile(@PathParam("jobId") String jobId,
    		@PathParam("filename") String filename)
    throws UnknownJobException
    {   	
    	s_Logger.debug(String.format("Get the log file %s for job %s", 
    			filename, jobId));
    	
		JobLogs logs = new JobLogs();
		return logs.file(jobId, filename);
    }
    
    
    /**
     * Tail the specific log file
     * 
     * @param jobId
     * @param lines Number of lines to tail
     * @return
     * @throws UnknownJobException
     */
    @GET
	@Path("/{jobId}/{filename}/tail")
    @Produces(MediaType.TEXT_PLAIN)
    public String tailLogFile(@PathParam("jobId") String jobId,
    		@PathParam("filename") String filename,
    		@DefaultValue("10") @QueryParam("lines") int lines)
    throws UnknownJobException
    {   	
    	s_Logger.debug("Tail log for job '" + jobId + "'");
    	
		JobLogs logs = new JobLogs();
		return logs.tail(jobId, filename, lines);
    }

}
