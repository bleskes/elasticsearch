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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.TimeoutHandler;

import org.apache.log4j.Logger;

import com.prelert.job.UnknownJobException;
import com.prelert.job.alert.Alert;
import com.prelert.job.alert.persistence.AlertPersister;
import com.prelert.job.manager.JobManager;
import com.prelert.job.process.ClosedJobException;
import com.prelert.rs.data.Bucket;
import com.prelert.rs.data.parsing.AlertObserver;


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
	static public final Logger s_Logger = Logger.getLogger(AlertManager.class);

	private Map<AsyncResponse, AlertListener> m_AsyncRepsonses;
	private AlertPersister m_AlertPersister;
	private AtomicLong m_IdSequence;

	public JobManager m_JobManager;

	public class AlertListener extends AlertObserver
	{
		private AsyncResponse m_Response;
		private AlertManager m_Manager;
		private String m_JobId;

		private AlertListener(AsyncResponse response, AlertManager manager, String jobId,
				double anomalyScoreThreshold, double normalizedProbabiltyThreshold)
		{
			super(normalizedProbabiltyThreshold, anomalyScoreThreshold);

			m_Response = response;
			m_Manager = manager;
			m_JobId = jobId;
		}

		@Override
		public void fire(Bucket bucket)
		{
			m_Manager.deregisterResponse(m_Response);
			m_Response.resume(createAlert(bucket, m_JobId));
		}

		public AsyncResponse getResponse()
		{
			return m_Response;
		}
	}

	/**
	 *
	 * @param alertPersister Knows how to save alerts
	 */
	public AlertManager(AlertPersister alertPersister, JobManager jobManager)
	{
		m_JobManager = jobManager;
		m_AlertPersister = alertPersister;
		m_AsyncRepsonses = new HashMap<>();

		String lastAlertId = m_AlertPersister.lastAlertId();
		try
		{
			long seq = Long.parseLong(lastAlertId);
			m_IdSequence = new AtomicLong(seq);

			s_Logger.info("Starting Alert Id sequence with value " + lastAlertId);
		}
		catch (NumberFormatException nfe)
		{
			s_Logger.info("New alert id sequence");
			m_IdSequence = new AtomicLong();
		}
	}

	/**
	 * Non blocking asynchronous request for alerts by job
	 *
	 * @param response
	 * @param jobId
	 * @param timeout_secs
	 * @param anomalyScoreThreshold
	 * @param normalizedProbabiltyThreshold
	 * @throws UnknownJobException
	 */
	public void registerRequest(AsyncResponse response, String jobId,
			long timeout_secs, double anomalyScoreThreshold, double normalizedProbabiltyThreshold)
	throws UnknownJobException
	{
		m_JobManager.jobExists(jobId);

		response.setTimeout(timeout_secs, TimeUnit.SECONDS);
		response.setTimeoutHandler(this);

		AlertListener listener = this.new AlertListener(response, this, jobId,
				anomalyScoreThreshold, normalizedProbabiltyThreshold);
		registerListener(listener);

		try
		{
			m_JobManager.addAlertObserver(jobId, listener);
		}
		catch (ClosedJobException e)
		{
			s_Logger.info("Error alerting on closed job " + jobId);
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

	private Alert createAlert(Bucket bucket, String jobId)
	{
		Alert alert = new Alert();
		alert.setId(Long.toString(m_IdSequence.incrementAndGet()));
		alert.setJobId(jobId);
		alert.setAnomalyScore(bucket.getAnomalyScore());
		alert.setNormalizedProbability(bucket.getMaxNormalizedProbability());

//    	URI uri = UriBuilder.fromPath("results")
//				.path(jobId)
//				.path(Buckets.ENDPOINT)
//				.path(bucket.getId())
//				.build();
//
//    	alert.setUri(uri);

		return alert;
	}

}
