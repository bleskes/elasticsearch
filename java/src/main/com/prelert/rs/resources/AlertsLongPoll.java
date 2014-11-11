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
import com.prelert.rs.data.ErrorCode;
import com.prelert.rs.provider.RestApiException;

/**
 * The alerts long poll endpoint.
 * Subscribe to alerts from an individual job
 */
@Path("/alerts_longpoll")
public class AlertsLongPoll extends ResourceWithJobManager
{
	static final private Logger s_Logger = Logger.getLogger(AlertsLongPoll.class);

	/**
	 * The name of this endpoint
	 */
	static public final String ENDPOINT = "alerts_longpoll";

	/**
	 * The timeout query parameter
	 */
	static public final String TIMEOUT = "timeout";
	/**
	 * The alert cursor query parameter
	 */
	static public final String CURSOR = "cursor";
	static public final String SCORE = "score";
	static public final String PROBABILITY = "probability";


	@GET
	@Path("/{jobId}")
	@Produces(MediaType.APPLICATION_JSON)
	public void pollJob(
			@PathParam("jobId") String jobId,
			@DefaultValue("90") @QueryParam(TIMEOUT) int timeout,
			@DefaultValue("100.0") @QueryParam(SCORE) double anomalyScoreThreshold,
			@DefaultValue("100.0") @QueryParam(PROBABILITY) double normalizedProbabiltyThreshold,
			@Suspended final AsyncResponse asyncResponse)
    throws InterruptedException, UnknownJobException
	{
		s_Logger.debug(String.format("long poll alerts for job %s, anomalyScore >= %f "
				+ "normalized prob >= %f", jobId, anomalyScoreThreshold,
				normalizedProbabiltyThreshold));

		if ((anomalyScoreThreshold < 0 || anomalyScoreThreshold >= 100.0)
				|| (normalizedProbabiltyThreshold < 0 || normalizedProbabiltyThreshold >= 100.0))
		{
			String msg = String.format("Invalid alert parameters. %s (%.2f) and %s (%.2f) must "
					+ "be in the range 0-100", SCORE, anomalyScoreThreshold,
					PROBABILITY, normalizedProbabiltyThreshold);
			s_Logger.info(msg);
			throw new RestApiException(msg, ErrorCode.INVALID_THRESHOLD_ARGUMENT, Response.Status.BAD_REQUEST);
		}

		AlertManager alertManager = alertManager();
		alertManager.registerRequest(asyncResponse, jobId, m_UriInfo.getBaseUri(),
				timeout, anomalyScoreThreshold, normalizedProbabiltyThreshold);
	}

}
