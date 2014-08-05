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

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;

import com.prelert.job.alert.manager.AlertManager;

/**
 * The alerts long poll endpoint. 
 * Subscribe to alerts from all jobs or an individual job. 
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
	
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public void poll(
			@DefaultValue("90") @QueryParam(TIMEOUT) int timeout,
			@DefaultValue("") @QueryParam(CURSOR) String cursor,
			@Suspended final AsyncResponse asyncResponse)
    throws InterruptedException 
	{
		s_Logger.debug("long poll alerts");
		
		AlertManager alertManager = alertManager();
		
		if (cursor.isEmpty() == false)
		{
			alertManager.registerRequestWithCursor(asyncResponse, cursor, timeout); 
		}
		else
		{
			alertManager.registerRequest(asyncResponse, timeout);
		}
	}

	
	@GET
	@Path("/{jobId}")	
	@Produces(MediaType.APPLICATION_JSON)
	public void pollJob(
			@PathParam("jobId") String jobId,
			@DefaultValue("90") @QueryParam(TIMEOUT) int timeout,
			@DefaultValue("") @QueryParam(CURSOR) String cursor,
			@Suspended final AsyncResponse asyncResponse)
    throws InterruptedException 
	{
		s_Logger.debug("long poll alerts for job " + jobId);
		
		AlertManager alertManager = alertManager();
		
		if (cursor.isEmpty() == false)
		{
			alertManager.registerRequestWithCursor(asyncResponse, jobId, 
					cursor, timeout); 
		}
		else
		{
			alertManager.registerRequest(asyncResponse, jobId, timeout);
		}
	}
		
}
