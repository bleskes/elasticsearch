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

import com.prelert.job.UnknownJobException;
import com.prelert.job.alert.Alert;
import com.prelert.job.manager.JobManager;
import com.prelert.rs.data.ErrorCode;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.provider.RestApiException;

/**
 * The alerts endpoint.
 * Query for all alerts, alerts by job id and between 2 dates
 * or subscribe to the long_poll endpoint.
 */
@Path("/alerts")
public class Alerts extends ResourceWithJobManager
{
	private static final Logger LOGGER = Logger.getLogger(Alerts.class);

	/**
	 * The name of this endpoint
	 */
	public static final String ENDPOINT = "alerts";

	/**
	 * The severity query parameter
	 */
	public static final String SEVERITY_QUERY_PARAM = "severity";
	/**
	 * The anomaly score query parameter
	 */
	public static final String ANOMALY_SCORE_QUERY_PARAM = "score";


	private static final DateFormat DATE_FORMAT = new SimpleDateFormat(ISO_8601_DATE_FORMAT);
	private static final DateFormat DATE_FORMAT_WITH_MS = new SimpleDateFormat(ISO_8601_DATE_FORMAT_WITH_MS);

	private static final DateFormat [] DATE_FORMATS = new DateFormat [] {
		DATE_FORMAT, DATE_FORMAT_WITH_MS};



    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Pagination<Alert> alerts(
    		@DefaultValue("0") @QueryParam("skip") int skip,
    		@DefaultValue(JobManager.DEFAULT_PAGE_SIZE_STR) @QueryParam("take") int take,
			@DefaultValue("") @QueryParam(START_QUERY_PARAM) String start,
			@DefaultValue("") @QueryParam(END_QUERY_PARAM) String end,
			@DefaultValue("") @QueryParam(SEVERITY_QUERY_PARAM) String severity,
			@DefaultValue("0.0") @QueryParam(ANOMALY_SCORE_QUERY_PARAM) double anomalyScore)
    {
    	boolean expand = true;

		LOGGER.debug(String.format("Get %s alerts, skip = %d, take = %d"
				+ " start = '%s', end='%s'",
				expand?"expanded ":"", skip, take, start, end));

		long epochStart = 0;
		if (start.isEmpty() == false)
		{
			epochStart = paramToEpoch(start, DATE_FORMATS);
			if (epochStart == 0) // could not be parsed
			{
				String msg = String.format(BAD_DATE_FROMAT_MSG, START_QUERY_PARAM, start);
				LOGGER.info(msg);
				throw new RestApiException(msg, ErrorCode.UNPARSEABLE_DATE_ARGUMENT,
						Response.Status.BAD_REQUEST);
			}
		}

		long epochEnd = 0;
		if (end.isEmpty() == false)
		{
			epochEnd = paramToEpoch(end, DATE_FORMATS);
			if (epochEnd == 0) // could not be parsed
			{
				String msg = String.format(BAD_DATE_FROMAT_MSG, START_QUERY_PARAM, end);
				LOGGER.info(msg);
				throw new RestApiException(msg, ErrorCode.UNPARSEABLE_DATE_ARGUMENT,
						Response.Status.BAD_REQUEST);
			}
		}


		Pagination<Alert> alerts = new Pagination<>();
//		AlertManager manager = alertManager();
		//Pagination<Alert> alerts = manager.alerts(skip, take, epochStart, epochEnd);

		// paging
    	if (alerts.isAllResults() == false)
    	{
    		List<ResourceWithJobManager.KeyValue> queryParams = new ArrayList<>();
    		if (epochStart > 0)
    		{
    			queryParams.add(this.new KeyValue(START_QUERY_PARAM, start));
    		}
    		if (epochEnd > 0)
    		{
    			queryParams.add(this.new KeyValue(END_QUERY_PARAM, end));
    		}

    		queryParams.add(this.new KeyValue(SEVERITY_QUERY_PARAM, severity));

    		setPagingUrls(ENDPOINT, alerts, queryParams);
    	}

		LOGGER.debug(String.format("Return %d alerts", alerts.getDocuments().size()));

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
			@DefaultValue("0.0") @QueryParam(ANOMALY_SCORE_QUERY_PARAM) double anomalyScore)
	throws UnknownJobException
    {
    	boolean expand = true;

		LOGGER.debug(String.format("Get %s alerts for job %s, skip = %d, take = %d"
				+ " start = '%s', end='%s'",
				expand?"expanded ":"", jobId, skip, take, start, end));

		long epochStart = 0;
		if (start.isEmpty() == false)
		{
			epochStart = paramToEpoch(start, DATE_FORMATS);
			if (epochStart == 0) // could not be parsed
			{
				String msg = String.format(BAD_DATE_FROMAT_MSG, START_QUERY_PARAM, start);
				LOGGER.info(msg);
				throw new RestApiException(msg, ErrorCode.UNPARSEABLE_DATE_ARGUMENT,
						Response.Status.BAD_REQUEST);
			}
		}

		long epochEnd = 0;
		if (end.isEmpty() == false)
		{
			epochEnd = paramToEpoch(end, DATE_FORMATS);
			if (epochEnd == 0) // could not be parsed
			{
				String msg = String.format(BAD_DATE_FROMAT_MSG, START_QUERY_PARAM, end);
				LOGGER.info(msg);
				throw new RestApiException(msg, ErrorCode.UNPARSEABLE_DATE_ARGUMENT,
						Response.Status.BAD_REQUEST);
			}
		}

		Pagination<Alert> alerts = new Pagination<>();
//		AlertManager manager = alertManager();
//		Pagination<Alert>  alerts = manager.jobAlerts(jobId, skip, take,
//				epochStart, epochEnd);

		// paging
    	if (alerts.isAllResults() == false)
    	{
    		String path = new StringBuilder()
    							.append(ENDPOINT)
    							.append("/")
    							.append(jobId)
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

    		queryParams.add(this.new KeyValue(SEVERITY_QUERY_PARAM, severity));

    		setPagingUrls(path, alerts, queryParams);
    	}

		LOGGER.debug(String.format("Return %d alerts for job %s",
				alerts.getDocumentCount(), jobId));

		return alerts;
    }

}
