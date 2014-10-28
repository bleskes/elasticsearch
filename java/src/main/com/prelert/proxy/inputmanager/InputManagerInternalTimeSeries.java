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
import java.util.Iterator;
import java.util.List;

import com.prelert.data.Attribute;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.TimeSeriesData;
import com.prelert.proxy.inputmanager.dao.InputManagerDAO;
import com.prelert.proxy.inputmanager.querymonitor.QueryTookTooLongException;
import com.prelert.proxy.plugin.InternalPlugin;
import com.prelert.proxy.plugin.ErrorGettingDataPointsException;
import com.prelert.proxy.plugin.Plugin;

public class InputManagerInternalTimeSeries extends InputManager 
{
	private InternalPlugin m_InternalPlugin;
	
	public InputManagerInternalTimeSeries(InputManagerDAO inputManagerDAO)
	{
		super(inputManagerDAO);
	}

	
	/**
	 * If <code>plugin</code> does not implement the <code>InternalPlugin</code>
	 * interface an UnsupportedOperationException is thrown.
	 * 
	 * @throws UnsupportedOperationException
	 */
	@Override 
	public void setPlugin(Plugin plugin)
	{
		super.setPlugin(plugin);
		
		if (plugin instanceof InternalPlugin)
		{
			m_InternalPlugin = (InternalPlugin)plugin;
		}
		else
		{
			throw new UnsupportedOperationException("Plugin '" + plugin.getName() + 
						"' does not implement the InternalPlugin interface. It cannot" +
						" be set on a Internal InputManager");
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
				m_InternalPlugin.getAllDataPointsForTimeSpan(startTime, endTime,
						m_InternalPlugin.getUsualPointIntervalSecs());
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
		Date timerStart = new Date();
		
		int numberOfTimeSeries = timeSeriesData.size();

		// The TimeSeriesData.toXmlStringInternal() function can produce a very 
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
			
			if (updateDisplayColumns && isFirst)
			{
				List<Attribute> attributes = tsData.getConfig().getAttributes();
				populateGuiDisplayColumns(tsData.getConfig().getDataType(), 
										DataSourceCategory.TIME_SERIES_FEATURE,
										attributes);
			}

			transferMessage(isFirst, tsData.toXmlStringInternal());
			isFirst = false;

			// Make available for garbage collection.
			itr.remove();
		}
		Date timerEnd = new Date();
		long duration = timerEnd.getTime() - timerStart.getTime();

		s_Logger.info(numberOfTimeSeries
				+ " Internal Time Series transferred to the backend TCP client in "
				+ duration + "ms.");
		
		
		return numberOfTimeSeries > 0;
	}

}
