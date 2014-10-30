/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2012     *
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
 ***********************************************************/

package com.prelert.proxy.inputmanager;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.prelert.data.Attribute;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.TimeSeriesConfig;
import com.prelert.data.TimeSeriesData;
import com.prelert.data.TimeSeriesInterpretation;
import com.prelert.proxy.inputmanager.dao.InputManagerDAO;
import com.prelert.proxy.inputmanager.querymonitor.QueryTookTooLongException;
import com.prelert.proxy.plugin.ExternalPointsPlugin;
import com.prelert.proxy.plugin.ErrorGettingDataPointsException;
import com.prelert.proxy.plugin.Plugin;

/**
 * InputManager for ExternalPointsPlugins.  
 */
public class InputManagerExternalPoints extends InputManager
{
	private ExternalPointsPlugin m_ExternalPointsPlugin;
	
	/**
	 * Mapper from a concatenation of Datatype + Metric to a time series type ID
	 */
	private HashMap<String, Integer> m_DataTypeMetricToTimeSeriesTypeId;

	/**
	 * Mapper from an external key to time series type ID and time series ID
	 */
	private HashMap<String, TimeSeriesTypeAndId> m_ExternalStringToTimeSeriesIds;

	
	public InputManagerExternalPoints(InputManagerDAO inputManagerDAO)
	{
		super(inputManagerDAO);
		
		m_DataTypeMetricToTimeSeriesTypeId = new HashMap<String, Integer>();
		m_ExternalStringToTimeSeriesIds = new HashMap<String, TimeSeriesTypeAndId>();
	}
	
	/**
	 * If <code>plugin</code> does not implement the <code>ExternalPointsPlugin</code> 
	 * interface an UnsupportedOperationException is thrown.
	 * 
	 * @throws UnsupportedOperationException
	 */
	@Override 
	public void setPlugin(Plugin plugin)
	{
		super.setPlugin(plugin);
		
		if (plugin instanceof ExternalPointsPlugin)
		{
			m_ExternalPointsPlugin = (ExternalPointsPlugin)plugin;
		}
		else
		{
			throw new UnsupportedOperationException("Plugin '" + plugin.getName() + 
					"' does not implement the m_ExternalPointsPlugin interface. It cannot" +
					" be set on a External Points InputManager");
		}
	}
	
	
	@Override
	protected boolean collectAndSendData(Date startTime, Date endTime, boolean updateDisplayColumns)
	{
		Collection<TimeSeriesData> timeSeriesData;
		
		int queryTookTooLongWaitTimeMs = 0;
		try
		{
			// Get the internal time series data points
			timeSeriesData = 
				m_ExternalPointsPlugin.getAllDataPointsForTimeSpan(startTime, endTime, 
						m_ExternalPointsPlugin.getUsualPointIntervalSecs());
		}
		catch (ErrorGettingDataPointsException ndp)
		{
			String msg = String.format("Error getting data points from plugin type '%s'.  Error = %s", 
												getPlugin().getDataSourceType().getName(), ndp.getMessage());
			s_Logger.error(msg);
			
			return false;
		}
		catch (QueryTookTooLongException e)
		{
			// May be overloading the server so back of for.
			queryTookTooLongWaitTimeMs += e.getWaitMs();
			
			try 
			{
				Thread.sleep(queryTookTooLongWaitTimeMs);
			}
			catch (InterruptedException e1) 
			{
				s_Logger.info("Interrupted whilst sleeping after a QueryTookTooLongException");
			}

			timeSeriesData = e.getTimeSeriesPartialResults();
			s_Logger.warn(e);
		}

		// and send them to the time series feature detector
		s_Logger.debug("Sending External Time Series data");
		
		int numberOfTimeSeries = timeSeriesData.size();
		Date timerStart = new Date();

		// The TimeSeriesData.toXmlStringExternal() function can produce a very 
		// large string. In order to keep memory utilisation down remove each 
		// time series from the collection after it has been sent. This makes 
		// memory available for garbage collection. This will only work with an
		// iterator it cannot be done in a for-each loop.
		Iterator<TimeSeriesData> itr = timeSeriesData.iterator();

		boolean isFirst = true;
		while (itr.hasNext())
		{
			if (m_Quit)
			{
				break;
			}

			TimeSeriesData tsData = itr.next();

			TimeSeriesTypeAndId timeSeriesIds = getTimeSeriesId(tsData.getConfig(),
														getPluginName(), 
														m_ExternalPointsPlugin.getUsualPointIntervalSecs());

			if (updateDisplayColumns && isFirst)
			{
				List<Attribute> attributes = tsData.getConfig().getAttributes();
				populateGuiDisplayColumns(tsData.getConfig().getDataType(), 
										DataSourceCategory.TIME_SERIES_FEATURE,
										attributes);
			}
			
			
			if (timeSeriesIds.isValidId())
			{
				transferMessage(isFirst,
						tsData.toXmlStringExternal(timeSeriesIds.getTimeSeriesId(),
								timeSeriesIds.getTimeSeriesTypeId()));
			}
			
			isFirst = false;

			// Make available for garbage collection.
			itr.remove();
		}
		

		Date timerEnd = new Date();
		long duration = timerEnd.getTime() - timerStart.getTime();

		s_Logger.info(numberOfTimeSeries
					+ " Time Series transferred to the backend TCP client in "
					+ duration + "ms.");
		
		return numberOfTimeSeries > 0;
	}
	
	
	/**
	 * Returns the ids for TimeSeriesType and TimeSeriesId for the given <code>externalKey</code>
	 * from the {@link com.prelet.data.TimeSeriesConfig} parameter.
	 * If <code>externalKey</code> hasn't been encountered before a new external time series and 
	 * time series type will be registered with the database.
	 * 
	 * @param config - The configuration details for the time series.
	 * @param pluginName
	 * @param usualInterval - The usual interval of the data points in the time series. 
	 * 
	 * @return
	 */
	protected TimeSeriesTypeAndId getTimeSeriesId(TimeSeriesConfig config,
													String pluginName,
													int usualInterval)
	{
		String externalKey = config.getExternalKey();
		if (externalKey == null)
		{
			// Invalid config - must have an externalKey.
			return new TimeSeriesTypeAndId();
		}
		
		TimeSeriesTypeAndId ids = m_ExternalStringToTimeSeriesIds.get(externalKey);
		if (ids == null)
		{
			String dataTypeMetric = config.getDataType() + '\t' + config.getMetric();
			// Even though we don't know the time series ID, we may still be
			// able to short-circuit the time series type ID lookup.
			Integer timeSeriesTypeId = m_DataTypeMetricToTimeSeriesTypeId.get(dataTypeMetric);
			if (timeSeriesTypeId == null)
			{
				timeSeriesTypeId = m_InputManagerDAO.addExternalTimeSeriesType(config.getDataType(),
												config.getMetric(),
												TimeSeriesInterpretation.ABSOLUTE,
												"graphName", "graphTitle", "graphYAxisLabel",
												usualInterval,	pluginName);
				
				// Error adding this time series type 
				// return error status.
				if (timeSeriesTypeId == -1)
				{
					return new TimeSeriesTypeAndId();
				}

				m_DataTypeMetricToTimeSeriesTypeId.put(dataTypeMetric, timeSeriesTypeId);
			}

			int timeSeriesId = addExternalTimeSeries(config);

			ids = new TimeSeriesTypeAndId(timeSeriesTypeId, timeSeriesId);

			m_ExternalStringToTimeSeriesIds.put(externalKey, ids);
		}
		
		return ids;
	}
	
	
	/**
	 * Add the external time series to the database. 
	 * This calls the setExternalTimeSeriesDetails function
	 * as only points will be stored externally.
	 * 
	 * @param config
	 * @param metric
	 * @param externalKey
	 * @param attributes
	 * @return
	 */
	protected int addExternalTimeSeries(TimeSeriesConfig config)
	{
		int timeSeriesId = m_InputManagerDAO.addExternalTimeSeries(config.getDataType(),
															config.getMetric(),
															config.getExternalKey());
		
		m_InputManagerDAO.setExternalTimeSeriesDetails(timeSeriesId, config.getSource(), 
													config.getAttributes(),
													m_ExternalPointsPlugin.getMetricPathSourcePrefix(),
													config.getSourcePosition(),
													m_ExternalPointsPlugin.getMetricPathMetricPrefix(),
													m_ExternalPointsPlugin.getMetricPathDelimiter());
		
		return timeSeriesId;
	}
	
	
	/**
	 * Utility class to hold external time series ids.
	 * <code>TimeSeriesID</code> should always be -ve for an external time series.
	 * 
	 * If <code>isValidId()</code> returns true then the object
	 * is in an error state where there are no valid time series type
	 * or time series ids. 
	 */
	protected class TimeSeriesTypeAndId
	{
		private int m_TimeSeriesTypeId;
		private int m_TimeSeriesId;
		boolean m_ValidId;
		
		public TimeSeriesTypeAndId()
		{
			m_ValidId = false;
		}
		public TimeSeriesTypeAndId(int timeSeriesType, int timeSeriesId)
		{
			m_TimeSeriesTypeId = timeSeriesType;
			m_TimeSeriesId = timeSeriesId;
			m_ValidId = true;
		}
		
		public boolean isValidId()
		{
			return m_ValidId;
		}
		
		public int getTimeSeriesId()
		{
			return m_TimeSeriesId;
		}
		
		public int getTimeSeriesTypeId()
		{
			return m_TimeSeriesTypeId;
		}
	}

}
