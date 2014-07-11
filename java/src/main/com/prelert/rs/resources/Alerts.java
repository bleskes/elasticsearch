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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import com.prelert.job.alert.Alert;
import com.prelert.job.alert.manager.AlertManager;
import com.prelert.job.manager.JobManager;
import com.prelert.rs.data.ErrorCode;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.provider.RestApiException;

/**
 * The alerts endpoint. 
 * Either query for all alerts or alerts by job id. 
 */

@Path("/alerts")
public class Alerts extends ResourceWithJobManager
{
	static final private Logger s_Logger = Logger.getLogger(Alerts.class);

	/**
	 * The name of this endpoint
	 */
	static public final String ENDPOINT = "alerts";
	
	/**
	 * The severity query parameter
	 */
	static public final String SEVERITY_QUERY_PARAM = "severity";
	/**
	 * The anomaly score query parameter
	 */
	static public final String ANOMALY_SCORE_QUERY_PARAM = "score";
	
	
	static private final DateFormat s_DateFormat = new SimpleDateFormat(ISO_8601_DATE_FORMAT); 
	static private final DateFormat s_DateFormatWithMs = new SimpleDateFormat(ISO_8601_DATE_FORMAT_WITH_MS); 
	
	static private final DateFormat [] s_DateFormats = new DateFormat [] {
		s_DateFormat, s_DateFormatWithMs};
	
	
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Pagination<Alert> alerts(
    		@DefaultValue("0") @QueryParam("skip") int skip,
    		@DefaultValue(JobManager.DEFAULT_PAGE_SIZE_STR) @QueryParam("take") int take,
			@DefaultValue("") @QueryParam(START_QUERY_PARAM) String start,
			@DefaultValue("") @QueryParam(END_QUERY_PARAM) String end,
			@DefaultValue("") @QueryParam(SEVERITY_QUERY_PARAM) String severity,
			@DefaultValue("0f") @QueryParam(END_QUERY_PARAM) float anomalyScore)
    {      
    	boolean expand = true;
    	
		s_Logger.debug(String.format("Get %s alerts, skip = %d, take = %d"
				+ " start = '%s', end='%s'", 
				expand?"expanded ":"", skip, take, start, end));
    	
		long epochStart = 0;
		if (start.isEmpty() == false)
		{
			epochStart = paramToEpoch(start, s_DateFormats);	
			if (epochStart == 0) // could not be parsed
			{
				String msg = String.format(BAD_DATE_FROMAT_MSG, START_QUERY_PARAM, start);
				s_Logger.info(msg);
				throw new RestApiException(msg, ErrorCode.UNPARSEABLE_DATE_ARGUMENT,
						Response.Status.BAD_REQUEST);
			}
		}
		
		long epochEnd = 0;
		if (end.isEmpty() == false)
		{
			epochEnd = paramToEpoch(end, s_DateFormats);	
			if (epochEnd == 0) // could not be parsed
			{
				String msg = String.format(BAD_DATE_FROMAT_MSG, START_QUERY_PARAM, end);
				s_Logger.info(msg);
				throw new RestApiException(msg, ErrorCode.UNPARSEABLE_DATE_ARGUMENT,
						Response.Status.BAD_REQUEST);
			}			
		}
		
		
		AlertManager manager = alertManager();
		Pagination<Alert> alerts;
		
		if (epochStart > 0 || epochEnd > 0)
		{
			alerts = manager.alerts(skip, take, epochStart, epochEnd, expand);
		}
		else
		{
			alerts = manager.alerts(skip, take, expand);
		}
		
		// paging
    	if (alerts.isAllResults() == false)
    	{
    		String path = "alerts/";
    		
    		List<ResourceWithJobManager.KeyValue> queryParams = new ArrayList<>();
    		if (epochStart > 0)
    		{
    			queryParams.add(this.new KeyValue(START_QUERY_PARAM, start));
    		}
    		if (epochEnd > 0)
    		{
    			queryParams.add(this.new KeyValue(END_QUERY_PARAM, end));
    		}
    		
    		setPagingUrls(path, alerts, queryParams);
    	}		
			
		s_Logger.debug(String.format("Return %d alerts", alerts.getDocuments().size()));
		
		return alerts;
    }
    
    @GET
	@Path("/{jobId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Pagination<Alert> jobAlerts(
    		@PathParam("jobId") String jobId,
    		@DefaultValue("0") @QueryParam("skip") int skip,
    		@DefaultValue(JobManager.DEFAULT_PAGE_SIZE_STR) @QueryParam("take") int take,
			@DefaultValue("") @QueryParam(START_QUERY_PARAM) String start,
			@DefaultValue("") @QueryParam(END_QUERY_PARAM) String end,
			@DefaultValue("") @QueryParam(SEVERITY_QUERY_PARAM) String severity,
			@DefaultValue("0f") @QueryParam(END_QUERY_PARAM) float anomalyScore)
    {      
    	boolean expand = true;
    	
		s_Logger.debug(String.format("Get %s alerts for job %s, skip = %d, take = %d"
				+ " start = '%s', end='%s'", 
				expand?"expanded ":"", jobId, skip, take, start, end));
    	
		long epochStart = 0;
		if (start.isEmpty() == false)
		{
			epochStart = paramToEpoch(start, s_DateFormats);	
			if (epochStart == 0) // could not be parsed
			{
				String msg = String.format(BAD_DATE_FROMAT_MSG, START_QUERY_PARAM, start);
				s_Logger.info(msg);
				throw new RestApiException(msg, ErrorCode.UNPARSEABLE_DATE_ARGUMENT,
						Response.Status.BAD_REQUEST);
			}
		}
		
		long epochEnd = 0;
		if (end.isEmpty() == false)
		{
			epochEnd = paramToEpoch(end, s_DateFormats);	
			if (epochEnd == 0) // could not be parsed
			{
				String msg = String.format(BAD_DATE_FROMAT_MSG, START_QUERY_PARAM, end);
				s_Logger.info(msg);
				throw new RestApiException(msg, ErrorCode.UNPARSEABLE_DATE_ARGUMENT,
						Response.Status.BAD_REQUEST);
			}			
		}
		
		
		AlertManager manager = alertManager();
		Pagination<Alert> alerts;
		
		if (epochStart > 0 || epochEnd > 0)
		{
			alerts = manager.jobAlerts(jobId, skip, take, epochStart, epochEnd, expand);
		}
		else
		{
			alerts = manager.jobAlerts(jobId, skip, take, expand);
		}
		
		// paging
    	if (alerts.isAllResults() == false)
    	{
    		String path = new StringBuilder()
    							.append(jobId)
								.append("/alerts/")
								.toString();
    		
    		List<ResourceWithJobManager.KeyValue> queryParams = new ArrayList<>();
    		if (epochStart > 0)
    		{
    			queryParams.add(this.new KeyValue(START_QUERY_PARAM, start));
    		}
    		if (epochEnd > 0)
    		{
    			queryParams.add(this.new KeyValue(END_QUERY_PARAM, end));
    		}
    		
    		setPagingUrls(path, alerts, queryParams);
    	}		
			
		s_Logger.debug(String.format("Return %d alerts", alerts.getDocumentCount()));
		
		return alerts;
    }
	
}
