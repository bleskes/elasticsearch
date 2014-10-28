/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2010     *
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

package com.prelert.server;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.prelert.dao.TimeSeriesDAO;
import com.prelert.data.Attribute;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;
import com.prelert.data.TimeFrame;
import com.prelert.data.TimeSeriesConfig;
import com.prelert.data.TimeSeriesDataPoint;
import com.prelert.service.TimeSeriesGXTPagingService;


/**
 * Server-side implementation of the service for GXT paging loaders for
 * retrieving time series data.
 * @author Pete Harverson
 */
public class TimeSeriesGXTPagingServiceImpl extends RemoteServiceServlet
        implements TimeSeriesGXTPagingService
{
	
	static Logger logger = Logger.getLogger(TimeSeriesGXTPagingServiceImpl.class);
	
	private TimeSeriesDAO	m_TimeSeriesDAO;
	
	
	/**
	 * Returns the TimeSeriesDAO being used by the query service.
	 * @return the data access object for time series data.
	 */
	public TimeSeriesDAO getTimeSeriesDAO()
    {
    	return m_TimeSeriesDAO;
    }


	/**
	 * Sets the TimeSeriesDAO to be used by the query service.
	 * @param timeSeriesDAO the data access object for time series data.
	 */
	public void setTimeSeriesDAO(TimeSeriesDAO timeSeriesDAO)
    {
		m_TimeSeriesDAO = timeSeriesDAO;
    }
	
	
	/**
	 * Returns the list of available sources of time series data,
	 * ordered by name, for the given data type.
	 * @param dataType type of time series data e.g. system_udp or p2psmon_servers.
	 * @return the list of available sources.
	 */
	@Override
	public List<String> getSourcesOrderByName(String dataType)
	{
		return m_TimeSeriesDAO.getSourcesOrderByName(
				new DataSourceType(dataType, DataSourceCategory.TIME_SERIES));
	}
	
	
	/**
	 * Returns a list of the distinct values for the attribute with the given name
	 * for the specified data type e.g. the values of the 'username' attribute
	 * for p2psmon_users data.
	 * @param dataType type of time series data e.g. system_udp or p2psmon_servers.
	 * @param attributeName name of attribute for which to return the values.
	 * @param source optional source name.
	 * @return a list of the distinct values for the attribute.
	 */
	@Override
	public List<String> getAttributeValues(String dataType,
	        String attributeName, String source)
	{
		return m_TimeSeriesDAO.getAttributeValues(dataType, attributeName, source);
	}

	
	/**
	 * Returns the data points for the specified time series.
	 * @param config TimeSeriesConfig object encapsulating the properties of the
	 * time series such as the type, source, metric, start and end times.
	 * @param includeFeatures flag indicating whether time series features should
	 * 	be included in the returned data points.
	 * @return list of time series data points.
	 */
	public List<TimeSeriesDataPoint> getDataPoints(TimeSeriesConfig config, 
			boolean includeFeatures)
	{
		logger.debug("getDataPoints() for " + config);

		// Need to get the date from the request config.
		Date minTime = config.getMinTime();
		Date maxTime = config.getMaxTime();
		
		String dataType = config.getDataType();
		String metric = config.getMetric();
		String source = config.getSource();
		List<Attribute> attributes = config.getAttributes();
		
		List<TimeSeriesDataPoint> dataPoints = null;
		
		// If no max time is supplied, for example, on opening the time series view from
		// the desktop for the first time, show a week's worth of data, with the
		// last day being the latest usage in the DB.
		if (maxTime == null)
		{
			maxTime = m_TimeSeriesDAO.getLatestTime(dataType, source);
			
			if (maxTime != null)
			{				
				if (minTime == null)
				{
					// If no min time is supplied, set it to be the first day
					// of the most recent week's worth of data.
					GregorianCalendar calendar = new GregorianCalendar();
					calendar.setTime(maxTime);
					calendar.add(Calendar.DAY_OF_MONTH, -6);
					minTime = calendar.getTime();
				}
			}
		}
		
		if (minTime != null && maxTime != null)
		{
			dataPoints = m_TimeSeriesDAO.getDataPointsForTimeSpan(
					dataType, metric, minTime, maxTime, 
					source, attributes, includeFeatures);
		}
		
		return dataPoints;
	}
	
	
	/**
	 * Returns the date/time of the earliest record in the database for the
	 * specified time series.
	 * @param config TimeSeriesConfig object encapsulating the properties of the
	 * time series such as the type, source and metric.
	 * @return date/time of earliest record.
	 */
	public Date getEarliestTime(TimeSeriesConfig config)
	{
		return m_TimeSeriesDAO.getEarliestTime(config.getDataType(), config.getSource());
	}
	
	
	/**
	 * Returns the date/time of the latest record in the database for the
	 * specified time series.
	 * @param config TimeSeriesConfig object encapsulating the properties of the
	 * time series such as the type, source and metric.
	 * @return date/time of latest record.
	 */
	public Date getLatestTime(TimeSeriesConfig config)
	{
		return m_TimeSeriesDAO.getLatestTime(config.getDataType(), config.getSource());
	}
	
	
	/**
	 * Calculates the minimum time for time series queries based on the date
	 * which was supplied in the DatePagingLoadConfig.
	 * @param timeFrame time frame of this load config e.g. week, day or hour.
	 * @param loadDate the date for the results to be displayed in the page.
	 * @return minimum date/time for time series query.
	 */
	protected Date getQueryMinimumTime(TimeFrame timeFrame, Date loadDate)
	{		
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.setTime(loadDate);
		
		GregorianCalendar minCalendar = new GregorianCalendar(
				calendar.get(Calendar.YEAR),
				calendar.get(Calendar.MONTH), 
				calendar.get(Calendar.DAY_OF_MONTH));

		switch (timeFrame)
		{
			case ALL:
			case WEEK:
				// Rule is to have the load config date as the first day of the week.
				//minCalendar.add(Calendar.DAY_OF_MONTH, -6);
				break;
				
			case DAY:
				// Rule is to use the start of load config day.
				break;
				
			case HOUR:
			case MINUTE:
			case SECOND:
				// Rule is to use the start of load config hour.
				minCalendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY));
				break;
		
		}
		
		return minCalendar.getTime();
	}
	
	
	/**
	 * Calculates the maximum time for time series queries based on the date
	 * which was supplied in the DatePagingLoadConfig.
	 * @param timeFrame time frame of this load config e.g. week, day or hour.
	 * @param loadDate the date for the results to be displayed in the page.
	 * @return minimum date/time for time series query.
	 */
	protected Date getQueryMaximumTime(TimeFrame timeFrame, Date loadDate)
	{		
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.setTime(loadDate);
		
		GregorianCalendar maxCalendar = new GregorianCalendar(
				calendar.get(Calendar.YEAR),
				calendar.get(Calendar.MONTH), 
				calendar.get(Calendar.DAY_OF_MONTH));

		switch (timeFrame)
		{
			case ALL:
			case WEEK:
				// Rule is to have the load config date as the first day of the week.
				maxCalendar.add(Calendar.DAY_OF_MONTH, 7);
				break;
				
			case DAY:
				// Rule is to use the end of load config day.
				maxCalendar.add(Calendar.DAY_OF_MONTH, 1);
				break;
				
			case HOUR:
			case MINUTE:
			case SECOND:
				// Rule is to use the start of load config hour.
				maxCalendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY));
				maxCalendar.add(Calendar.HOUR_OF_DAY, 1);
				break;
		
		}
		
		return maxCalendar.getTime();
	}
	
}
