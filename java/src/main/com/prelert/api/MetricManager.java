/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2013     *
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

package com.prelert.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;


import com.prelert.api.data.MetricFeed;
import com.prelert.data.ExternalTimeSeriesDetails;
import com.prelert.data.TimeSeriesInterpretation;
import com.prelert.proxy.inputmanager.DataCollectionMode;
import com.prelert.proxy.inputmanager.PrelertBackendTCPClient;
import com.prelert.proxy.inputmanager.dao.InputManagerDAO;


/**
 * Convert metric data to Xml and forward to the Prelert processes.
 * Manage database interactions (time series id etc.)
 */
public class MetricManager 
{
	static final public Logger s_Logger = Logger.getLogger(MetricManager.class);
	
	/**
	 * External Time series need to be added to the database with the
	 * name of the plugin used to retrieve the data. For the API the 
	 * data will never be accessed through the Proxy or GUI so this field
	 * will never be used. Use this value to mark the external time 
	 * series to show it came through the API.
	 */
	static final public String PRELERT_API_PLUGIN_NAME = "PrelertApi";
	
	/**
	 * Time series feature detector port
	 */
	static final public int DEFAULT_FEATURE_DETECTOR_PORT = 49996;
	
	/**
	 * Time series Points are sent in batches of this size.
	 */
	static final private int DEFAULT_BATCH_SIZE = 100;
	
	
	
	private PrelertBackendTCPClient m_BackendClient;
	
	private String m_Host;
	private int m_Port = DEFAULT_FEATURE_DETECTOR_PORT;
	
	
	/**
	 * Database DAO object
	 */
	private InputManagerDAO m_InputManagerDao;
	
	/**
	 * Mapper from a concatenation of Datatype + Metric to a time series type ID
	 */
	private Map<String, Integer> m_DataTypeMetricToTimeSeriesTypeId;

	/**
	 * Mapper from a Metric Path to time series type ID and time series ID
	 */
	private Map<String, TimeSeriesTypeAndId> m_ExternalStringToTimeSeriesIds;
	
	
	
	/**
	 * Default cons.
	 */
	public MetricManager()
	{
		m_DataTypeMetricToTimeSeriesTypeId = new HashMap<String, Integer>();
		
		m_ExternalStringToTimeSeriesIds =  new HashMap<String, TimeSeriesTypeAndId>();
	}
	
	
	/**
	 * Convert the metric data to Xml (time series points format)
	 * and send via the backend client.
	 * 
	 * Update the database with details of any new time series.
	 * 
	 * @param metrics
	 * @return
	 */
	public boolean addMetricFeed(MetricFeed metricData)
	{
		return addMetricFeed(true, metricData);
	}
	
	
	/**
	 * Convert MetricFeed to time series points and send to the back end.
	 * Points are sent in batches of size {@link #DEFAULT_BATCH_SIZE}
	 * 
	 * @param isFirst Set to true if the message to be transferred is the
	 *                     first in a batch. If true then the backlog handler 
	 *                     will be called which clears any unsent messages.
	 * @param metricFeed
	 * @return
	 */
	private boolean addMetricFeed(boolean isFirst, MetricFeed metricFeed)
	{
		s_Logger.info("Transferring " + metricFeed.getMetricData().size() + " metrics");
		
		int i = 0;
		while (i < metricFeed.getMetricData().size())
		{
			StringBuilder builder = new StringBuilder(MetricFeed.TAGGED_POINTS_OPEN_TAG);

			// Send points in batches of 100
			int j = 0;
			while (j < DEFAULT_BATCH_SIZE && (i + j) < metricFeed.getMetricData().size())
			{
				MetricFeed.MetricData metricData = metricFeed.getMetricData().get(i);
				TimeSeriesTypeAndId timeSeriesIds = getTimeSeriesId(metricFeed.getSource(), 
						metricData);
				
				if (timeSeriesIds != null)
				{
					
					builder.append(metricFeed.toXmlStringExternal(metricData,
								timeSeriesIds.m_TimeSeriesId,
								timeSeriesIds.m_TimeSeriesTypeId, false));
				}
				else
				{
					s_Logger.error("no time series id");
				}
				
				i++;
				j++;
			}
			
			builder.append(MetricFeed.TAGGED_POINTS_CLOSE_TAG);

			transferMessage(isFirst, builder.toString());
			isFirst = false;
		}
		
		return true;
	}
	
	
	/**
	 * Transfer a message to the Prelert back end TCP client. This will send
	 * the message in a different thread, to avoid unnecessarily blocking the
	 * threads that pull data from 3rd party systems.
	 * 
	 * @param firstInBatch Set to true if the message to be transferred is the
	 *                     first in a batch. If true then the backlog handler 
	 *                     will be called which clears any unsent messages.
	 * @param message The message to be sent
	 */
	private void transferMessage(boolean firstInBatch, String message)
	{
		if (m_BackendClient == null)
		{
			s_Logger.error("Cannot send message before Prelert TCP client is initialised - message lost : "
							+ message);
			return;
		}

		if (firstInBatch)
		{
			m_BackendClient.backlogHandler(DataCollectionMode.REALTIME);
		}

		m_BackendClient.queueMessage(message);
	}
	

	/**
	 * Read all the time series Ids from the data base and 
	 * initialise and start the backend client. <br/>
	 * The backend client is responsible for sending data to the
	 * Prelert processes. 
	 */
	public void start()
	{
		loadAllExternalTimeSeriesIds();
		
		m_BackendClient = new PrelertBackendTCPClient(m_Host, m_Port, true, DataCollectionMode.REALTIME);
		m_BackendClient.start();
	}
	
	
	/**
	 * Stop the backend TCP client thread.
	 */
	public void stop()
	{
		if (m_BackendClient != null)
		{
			m_BackendClient.quit();
		}
	}
	
	/**
	 * Stop the backend client.<br/>
	 * This function will block as the client attempts to finish 
	 * sending any unsent messages. 
	 */
	public void finish()
	{
		s_Logger.info("MetricManager about to finish");
		
		if (m_BackendClient != null)
		{
			m_BackendClient.waitUntilAllMessagesSent();
			s_Logger.info("Backend Client finish, now about to quit");
			m_BackendClient.quit();
		}
	}
		
	
	/**
	 * Returns the ids for TimeSeriesType and TimeSeriesId for the time series 
	 * with the metric path equal to <code>metricData.getMetricPath()</code>
	 * 
	 * If a time series with the metric path hasn't been encountered before 
	 * a new external time series (and possibly a time series type) will 
	 * be registered in the database.
	 * 
	 * @param metricData
	 * 
	 * @return May return <code>null</code> if an error occurs.
	 */
	private TimeSeriesTypeAndId getTimeSeriesId(String dataType,  
			MetricFeed.MetricData metricData)
	{
		String metricPath = metricData.getMetricPath();
		if (metricPath == null || metricPath.isEmpty())
		{
			// Invalid config - must have an externalKey.
			s_Logger.error("MetricData has no MetricPath");
			return null;
		}
		
		TimeSeriesTypeAndId ids = m_ExternalStringToTimeSeriesIds.get(metricPath);
		if (ids == null)
		{
			String metric = metricData.getMetric();
			String dataTypeMetric = dataType + '\t' + metric;
			
			// Even though we don't know the time series ID, we may still be
			// able to short-circuit the time series type ID lookup.
			Integer timeSeriesTypeId = m_DataTypeMetricToTimeSeriesTypeId.get(dataTypeMetric);
			if (timeSeriesTypeId == null)
			{
				// External time series need to be created with a plugin name
				// even if only used by the API.
				// See Java doc comment for PRELERT_API_PLUGIN_NAME.
				timeSeriesTypeId = m_InputManagerDao.addExternalTimeSeriesType(dataType,
						metric, TimeSeriesInterpretation.ABSOLUTE,
						"graphName", "graphTitle", "graphYAxisLabel", 15, PRELERT_API_PLUGIN_NAME);
				
				// Error adding this time series type 
				// return error status.
				if (timeSeriesTypeId == -1)
				{
					return null;
				}

				m_DataTypeMetricToTimeSeriesTypeId.put(dataTypeMetric, timeSeriesTypeId);
			}

			int timeSeriesId = addExternalTimeSeries(dataType, metricData, metric);

			ids = new TimeSeriesTypeAndId(timeSeriesTypeId, timeSeriesId);

			TimeSeriesTypeAndId old_id = m_ExternalStringToTimeSeriesIds.put(metricPath, ids);
			if (old_id != null)
			{
				s_Logger.error("duplicate time series id");
			}
		}
		
		return ids;
	}
	
	
	/**
	 * Add the external time series to the database and returns 
	 * id of the new time series.
	 * 
	 * @param dataType
	 * @param metricData
	 * @param metric
	 * 
	 * @return
	 */
	private int addExternalTimeSeries(String dataType, MetricFeed.MetricData metricData,
			String metric)
	{
		int timeSeriesId = m_InputManagerDao.addExternalTimeSeries(dataType,
				metric, metricData.getMetricPath());
		
		return timeSeriesId;
	}
	
	
	/**
	 * Load all the external time series Ids and populate the in memory hash 
	 * table. This should improve start up time by bulk loading the time series
	 * Ids in the case where the database has already been populated. 
	 */
	private void loadAllExternalTimeSeriesIds()
	{
		if (m_InputManagerDao == null)
		{
			s_Logger.error("Cannot load external time series Ids, InputManagerDAO == null");
			return;
		}
		
		List<ExternalTimeSeriesDetails> details = m_InputManagerDao.getExternalTimeSeriesDetails(true);
		
		s_Logger.info(String.format("Got details for %d external time series", details.size()));
		
		List<Integer> timeSeriesIds = new ArrayList<Integer>();
		
		for (ExternalTimeSeriesDetails detail : details)
		{
			String dataTypeMetric = detail.getType() + '\t' + detail.getMetric();
			m_DataTypeMetricToTimeSeriesTypeId.put(dataTypeMetric, detail.getTimeSeriesTypeId());

			TimeSeriesTypeAndId ids = new TimeSeriesTypeAndId(detail.getTimeSeriesTypeId(),
					detail.getTimeSeriesId());
			m_ExternalStringToTimeSeriesIds.put(detail.getExternalKey(), ids);
			
			timeSeriesIds.add(detail.getTimeSeriesId());
		}
		
		m_InputManagerDao.setExternalTimeSeriesActive(timeSeriesIds);
	}
	
	
	/**
	 * Get the port the Prelert feature detector process is running on.
	 * Time series points are sent to this port.
	 * 
	 * Defaults to {@link #DEFAULT_FEATURE_DETECTOR_PORT}.
	 * 
	 * @return
	 */
	public int getPort()
	{
		return m_Port;
	}
	
	/**	
	 * Set the port the Prelert feature detector process is running on.
	 * Time series points are sent to this port.
	 */
	public void setPort(int port)
	{
		m_Port = port;
	}
	
	
	/**
	 * Get the host machine the Prelert processes are running on. 
	 * Time series points will be sent to this host.
	 * 
	 * @return
	 */
	public String getHost()
	{
		return m_Host;
	}
	
	/**
	 * Set the host machine the Prelert processes are running on. 
	 * Time series points will be sent to this host.
	 */
	public void setHost(String host)
	{
		m_Host = host;
	}
	
	
	/**
	 * The InputManager data access object.
	 * 
	 * @return
	 */
	public InputManagerDAO getInputManagerDAO()
	{
		return m_InputManagerDao;
	}
	
	/**
	 * Set the InputManager data access object.
	 * 
	 * @return
	 */	
	public void setInputManagerDAO(InputManagerDAO dao)
	{
		m_InputManagerDao = dao;
	}
	
	
	/**
	 * Helper class to group time series type Id and the
	 * time series Id. 
	 */
	private class TimeSeriesTypeAndId
	{
		private int m_TimeSeriesTypeId;
		private int m_TimeSeriesId;
		
		TimeSeriesTypeAndId(int type, int id)
		{
			m_TimeSeriesTypeId = type;
			m_TimeSeriesId = id;
		}
	}
}
