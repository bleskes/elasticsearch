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

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import com.prelert.job.JobException;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.logs.JobLogs;
import com.prelert.job.messages.Messages;


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
    private static final Logger LOGGER = Logger.getLogger(Logs.class);

    /**
     * The name of this endpoint
     */
    public static final String ENDPOINT = "logs";

    /**
     * Never tail more than this many lines in case the
     * input parameter asks for a ridculous amount
     */
    public static final int MAX_TAIL_LINES = 10000;

    /**
     * Get the contents of the log directory zipped.
     * This is the same as {@link #zipLogFiles(String)}
     *
     * @param jobId
     * @return
     * @throws JobException
     */
    @GET
    @Path("/{jobId}/")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response jobLogFiles(@PathParam("jobId") String jobId)
    throws JobException
    {
        checkValidFilePath(jobId);
        return zipJobLogFiles(jobId);
    }


    /**
     * Get the contents of the log directory zipped
     * This is the same as {@link #jobLogFiles(String)}
     *
     * @param jobId
     * @return
     * @throws JobException
     */
    @GET
    @Path("/{jobId}/zip")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response zipLogFiles(@PathParam("jobId") String jobId)
    throws JobException
    {
        checkValidFilePath(jobId);
        return zipJobLogFiles(jobId);
    }

    /**
     * Return a response with a file attachment of the zipped log files.
     * @param jobId
     * @return
     * @throws JobException
     */
    private Response zipJobLogFiles(String jobId)
    throws JobException
    {
        LOGGER.debug("Get zipped logs for job '" + jobId + "'");

        JobLogs logs = new JobLogs();
        byte [] compressFiles = logs.zippedLogFiles(jobId);

        String filename = jobId + "_logs.zip";

        return Response.ok(compressFiles)
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .build();
    }

    /**
     * Tail the default log file for this job id.
     *
     * @param jobId
     * @param lines Number of lines to tail
     * @return
     * @throws JobException
     */
    @GET
    @Path("/{jobId}/tail")
    @Produces(MediaType.TEXT_PLAIN)
    public String tailDefaultLogFile(@PathParam("jobId") String jobId,
            @DefaultValue("10") @QueryParam("lines") int lines)
    throws JobException
    {
        LOGGER.debug("Tail default log for job '" + jobId + "'");

        checkValidFilePath(jobId);
        JobLogs logs = new JobLogs();
        return logs.tail(jobId, lines);
    }

    /**
     * Read the entire log file
     *
     * @param jobId
     * @param lines Number of lines to tail
     * @return
     * @throws JobException
     * @throws IOException
     */
    @GET
    @Path("/{jobId}/{filename}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getLogFile(@PathParam("jobId") String jobId,
            @PathParam("filename") String filename)
    throws JobException
    {
        LOGGER.debug(String.format("Get the log file %s for job %s", filename, jobId));

        checkValidFilePath(jobId);
        checkValidFilePath(filename);
        JobLogs logs = new JobLogs();
        return logs.file(jobId, filename);
    }


    /**
     * Tail the specific log file
     *
     * @param jobId
     * @param lines Number of lines to tail
     * @return
     * @throws JobException
     */
    @GET
    @Path("/{jobId}/{filename}/tail")
    @Produces(MediaType.TEXT_PLAIN)
    public String tailLogFile(@PathParam("jobId") String jobId,
            @PathParam("filename") String filename,
            @DefaultValue("10") @QueryParam("lines") int lines)
    throws JobException
    {
        lines = Math.min(lines, MAX_TAIL_LINES);
        LOGGER.debug(String.format("Tail %d lines from log file %s for job %s",
                        lines, filename, jobId));

        checkValidFilePath(jobId);
        checkValidFilePath(filename);
        JobLogs logs = new JobLogs();
        return logs.tail(jobId, filename, lines);
    }


    /**
     * Path to log files cannot contain '/' or '\' which could
     * potentially be used to escape the logs directory
     */
    private void checkValidFilePath(String path)
    throws JobException
    {
        if (path.contains("\\") || path.contains("/"))
        {
            String msg = Messages.getMessage(Messages.LOGFILE_INVALID_CHARS_IN_PATH, path);
            LOGGER.warn(msg);
            throw new JobException(msg, ErrorCodes.INVALID_LOG_FILE_PATH);
        }

    }

}
