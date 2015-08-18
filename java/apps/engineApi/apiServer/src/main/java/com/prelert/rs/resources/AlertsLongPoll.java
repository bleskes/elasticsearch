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

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import com.prelert.job.UnknownJobException;
import com.prelert.job.alert.manager.AlertManager;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.messages.Messages;
import com.prelert.rs.provider.RestApiException;

/**
 * The alerts long poll endpoint.
 * Subscribe to alerts from an individual job
 */
@Path("/alerts_longpoll")
public class AlertsLongPoll extends ResourceWithJobManager
{
    private static final Logger LOGGER = Logger.getLogger(AlertsLongPoll.class);

    /**
     * The name of this endpoint
     */
    public static final String ENDPOINT = "alerts_longpoll";

    /**
     * The timeout query parameter
     */
    public static final String TIMEOUT = "timeout";
    /**
     * The alert cursor query parameter
     */
    public static final String SCORE = "score";
    public static final String PROBABILITY = "probability";


    @GET
    @Path("/{jobId}")
    @Produces(MediaType.APPLICATION_JSON)
    public void pollJob(
            @PathParam("jobId") String jobId,
            @DefaultValue("90") @QueryParam(TIMEOUT) int timeout,
            @QueryParam(SCORE) Double anomalyScoreThreshold,
            @QueryParam(PROBABILITY) Double normalizedProbabiltyThreshold,
            @Suspended final AsyncResponse asyncResponse)
    throws InterruptedException, UnknownJobException
    {
        logEndpointCall(jobId, anomalyScoreThreshold, normalizedProbabiltyThreshold);

        if (anomalyScoreThreshold == null && normalizedProbabiltyThreshold == null)
        {
            String msg = Messages.getMessage(Messages.REST_ALERT_MISSING_ARGUMENT);
            LOGGER.info(msg);
            throw new RestApiException(msg, ErrorCodes.INVALID_THRESHOLD_ARGUMENT, Response.Status.BAD_REQUEST);
        }

        if (!isWithinRange0To100(anomalyScoreThreshold)
                || !isWithinRange0To100(normalizedProbabiltyThreshold))
        {
            String error = createThresholdOutOfRangeMsg(anomalyScoreThreshold,
                    normalizedProbabiltyThreshold);

            String msg = Messages.getMessage(Messages.REST_ALERT_INVALID_THRESHOLD, error);
            LOGGER.info(msg);
            throw new RestApiException(msg, ErrorCodes.INVALID_THRESHOLD_ARGUMENT, Response.Status.BAD_REQUEST);
        }

        if (timeout <= 0)
        {
            String msg = Messages.getMessage(Messages.REST_ALERT_INVALID_TIMEOUT);
            LOGGER.info(msg);
            throw new RestApiException(msg, ErrorCodes.INVALID_TIMEOUT_ARGUMENT, Response.Status.BAD_REQUEST);
        }

        AlertManager alertManager = alertManager();
        alertManager.registerRequest(asyncResponse, jobId, m_UriInfo.getBaseUri(),
                timeout, anomalyScoreThreshold, normalizedProbabiltyThreshold);
    }

    private void logEndpointCall(String jobId, Double anomalyScore, Double normalizedProbability)
    {
        StringBuilder msg = new StringBuilder();
        msg.append("long poll alerts for job ");
        msg.append(jobId);
        if (anomalyScore != null)
        {
            msg.append(", anomalyScore >= ");
            msg.append(anomalyScore);
            msg.append(" ");
        }
        if (normalizedProbability != null)
        {
            msg.append(", normalized prob >= ");
            msg.append(normalizedProbability);
        }
        LOGGER.debug(msg.toString());
    }

    private static boolean isWithinRange0To100(Double value)
    {
        return value == null ? true : value >= 0.0 && value <= 100.0;
    }

    private static String createThresholdOutOfRangeMsg(Double anomalyScore,
            Double normalizedProbability)
    {
        StringBuilder msg = new StringBuilder();
        if (anomalyScore != null)
        {
            msg.append(SCORE);
            msg.append(" (");
            msg.append(anomalyScore);
            msg.append(")");
            if (normalizedProbability != null)
            {
                msg.append(" and ");
            }
        }
        if (normalizedProbability != null)
        {
            msg.append(PROBABILITY);
            msg.append(" (");
            msg.append(normalizedProbability);
            msg.append(")");
        }
        return msg.toString();
    }
}
