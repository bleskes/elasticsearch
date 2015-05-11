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

package com.prelert.job.alert.manager;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.TimeoutHandler;
import javax.ws.rs.core.UriBuilder;

import org.apache.log4j.Logger;

import com.prelert.job.alert.Alert;
import com.prelert.job.alert.AlertObserver;
import com.prelert.job.exceptions.UnknownJobException;
import com.prelert.job.manager.JobManager;
import com.prelert.job.persistence.JobProvider;
import com.prelert.job.process.exceptions.ClosedJobException;
import com.prelert.rs.data.AnomalyRecord;
import com.prelert.rs.data.Bucket;
import com.prelert.rs.data.Detector;
import com.prelert.rs.resources.Buckets;


/**
 * Alerts are channelled through this object interested
 * parties register for alert notification using the
 * observer pattern.
 *
 * Handles Asynchronous HTTP requests
 *
 * Alert Ids are a sequence shared between all jobs starting at 1
 * each alert has a unique id. The function {@linkplain #alertsAfterCursor(String)}
 * returns a list of alerts in the sequence after the alert Id (cursor) parameter
 */
public class AlertManager implements TimeoutHandler
{
	private static final Logger LOGGER = Logger.getLogger(AlertManager.class);

	private Map<AsyncResponse, AlertListener> m_AsyncRepsonses;

	private JobProvider m_JobProvider;
	private JobManager m_JobManager;

	public class AlertListener extends AlertObserver
	{
		private AsyncResponse m_Response;
		private AlertManager m_Manager;
		private String m_JobId;
		private URI m_BaseUri;

		private AlertListener(AsyncResponse response, AlertManager manager, String jobId,
				double anomalyScoreThreshold, double normalizedProbabiltyThreshold,
				URI baseUri)
		{
			super(normalizedProbabiltyThreshold, anomalyScoreThreshold);

			m_Response = response;
			m_Manager = manager;
			m_JobId = jobId;
			m_BaseUri = baseUri;
		}

		@Override
		public void fire(Bucket bucket)
		{
		    LOGGER.info(String.format("Alert fired in bucket %s, probablilty = %f, anomaly score = %f",
		                                bucket.getTimestamp(),
		                                bucket.getMaxNormalizedProbability(),
		                                bucket.getAnomalyScore()));

			m_Manager.deregisterResponse(m_Response);
			m_Response.resume(createAlert(bucket, this));
		}

		public AsyncResponse getResponse()
		{
			return m_Response;
		}

		public String getJobId()
		{
			return m_JobId;
		}

		public URI getBaseUri()
		{
			return m_BaseUri;
		}
	}

	public AlertManager(JobProvider jobProvider, JobManager jobManager)
	{
		m_JobProvider = jobProvider;
		m_JobManager = jobManager;
		m_AsyncRepsonses = new HashMap<>();
	}

	/**
	 * Non blocking asynchronous request for alerts by job
	 *
	 * @param response
	 * @param jobId
	 * @param timeoutSecs
	 * @param anomalyScoreThreshold
	 * @param normalizedProbabiltyThreshold
	 * @throws UnknownJobException
	 */
	public void registerRequest(AsyncResponse response, String jobId, URI baseUri,
			long timeoutSecs, double anomalyScoreThreshold, double normalizedProbabiltyThreshold)
	throws UnknownJobException
	{
		m_JobProvider.jobExists(jobId);

		response.setTimeout(timeoutSecs, TimeUnit.SECONDS);
		response.setTimeoutHandler(this);

		AlertListener listener = this.new AlertListener(response, this, jobId,
				anomalyScoreThreshold, normalizedProbabiltyThreshold,
				baseUri);
		registerListener(listener);

		try
		{
			m_JobManager.addAlertObserver(jobId, listener);
		}
		catch (ClosedJobException e)
		{
			LOGGER.warn("Alerting on closed job " + jobId);
			deregisterResponse(response);
			response.resume(e);
		}
	}


	/**
	 * AysncResponse timeout handler
	 */
	@Override
	public void handleTimeout(AsyncResponse response)
	{

		Alert alert = new Alert();
		alert.setTimeout(true);

		AlertListener listener = getListener(response);
		if (listener != null)
		{
			alert.setJobId(listener.m_JobId);
			deregisterResponse(response);
		}

		response.resume(alert);
	}


	private AlertListener getListener(AsyncResponse response)
	{
		synchronized(m_AsyncRepsonses)
		{
			return m_AsyncRepsonses.get(response);
		}
	}

	private void registerListener(final AlertListener listener)
	{
		synchronized(m_AsyncRepsonses)
		{
			m_AsyncRepsonses.put(listener.getResponse(), listener);
		}
	}

	private void deregisterResponse(AsyncResponse response)
	{
		synchronized(m_AsyncRepsonses)
		{
			m_AsyncRepsonses.remove(response);
		}
	}

	private Alert createAlert(Bucket bucket, AlertListener listener)
	{
		Alert alert = new Alert();
		alert.setTimestamp(new Date());
		alert.setJobId(listener.getJobId());
		alert.setAnomalyScore(bucket.getAnomalyScore());
		alert.setMaxNormalizedProbability(bucket.getMaxNormalizedProbability());

		UriBuilder uriBuilder = UriBuilder.fromUri(listener.getBaseUri())
				    			.path("results")
								.path(listener.getJobId())
								.path(Buckets.ENDPOINT)
								.path(bucket.getId())
								.queryParam(Buckets.EXPAND_QUERY_PARAM, true);

		List<AnomalyRecord> records = new ArrayList<>();
		for (Detector detector : bucket.getDetectors())
		{
			for (AnomalyRecord r : detector.getRecords())
			{
				if (r.getNormalizedProbability() > listener.getNormalisedProbThreshold())
				{
					records.add(r);
				}
			}
		}


    	if (listener.isAnomalyScoreAlert(bucket.getAnomalyScore()))
    	{
    		bucket.setRecords(records);
    		alert.setBucket(bucket);
    	}
    	else
    	{
    		alert.setRecords(records);
    	}


    	alert.setUri(uriBuilder.build());

    	return alert;
	}

}
