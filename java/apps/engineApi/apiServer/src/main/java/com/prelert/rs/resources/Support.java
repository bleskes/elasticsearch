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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import com.prelert.job.UnknownJobException;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.logs.JobLogs;
import com.prelert.job.messages.Messages;
import com.prelert.job.process.ProcessCtrl;
import com.prelert.rs.data.ApiError;

/**
 * The support endpoint
 */
@Path("support")
public class Support {
    private static final Logger LOGGER = Logger.getLogger(Support.class);

    /**
     * The name of this endpoint
     */
    public static final String SUPPORT = "support";


    /**
     * Run the support bundle script returning the generated files & logs
     * as a compressed binary stream.
     */
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response supportBundle()
    throws UnknownJobException
    {
        LOGGER.info("Support Bundle request");

        Process proc;
        try
        {
            proc = Runtime.getRuntime().exec(new String [] {ProcessCtrl.SUPPORT_BUNDLE_CMD,
                                            ProcessCtrl.SUPPORT_BUNDLE_DIR});
            int exitValue = proc.waitFor();
            if (exitValue != 0)
            {
                return buildErrorResponse(
                        Messages.getMessage(Messages.SUPPORT_BUNDLE_SCRIPT_ERROR,
                             ProcessCtrl.SUPPORT_BUNDLE_CMD + " " + ProcessCtrl.SUPPORT_BUNDLE_DIR),
                        readString(proc.getErrorStream()));
            }
        }
        catch (SecurityException | IOException |InterruptedException e)
        {
            LOGGER.error("Cannot execute support bundle script", e);

            return buildErrorResponse(
                    Messages.getMessage(Messages.SUPPORT_BUNDLE_SCRIPT_ERROR,
                           ProcessCtrl.SUPPORT_BUNDLE_CMD + " " + ProcessCtrl.SUPPORT_BUNDLE_DIR),
                           e.toString());
        }


        try
        {
            return Response.ok(zippedLogFiles())
                    .header("Content-Disposition", "attachment; filename=\"prelert_support_bundle.zip\"")
                    .build();
        }
        catch (UnknownJobException e)
        {
            LOGGER.error("Support bundle output not found", e);

            return buildErrorResponse(
                    Messages.getMessage(Messages.SUPPORT_BUNDLE_SCRIPT_ERROR,
                           ProcessCtrl.SUPPORT_BUNDLE_CMD + " " + ProcessCtrl.SUPPORT_BUNDLE_DIR),
                           e.toString());
        }
    }

    private byte[] zippedLogFiles() throws UnknownJobException
    {
        JobLogs logs = new JobLogs();
        return logs.zippedLogFiles(new File(ProcessCtrl.LOG_DIR), "prelert_support_bundle" );
    }

    private Response buildErrorResponse(String message, String cause)
    {
        ApiError error = new ApiError(ErrorCodes.SUPPORT_BUNDLE_EXECUTION_ERROR);
        error.setCause(cause);
        error.setMessage(message);

        return Response.serverError()
                        .type(MediaType.APPLICATION_JSON)
                        .entity(error.toJson()).build();
    }


    private String readString(InputStream input) throws IOException
    {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input)))
        {
            return buffer.lines().collect(Collectors.joining("\n"));
        }
    }

}
