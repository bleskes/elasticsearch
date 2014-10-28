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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.prelert.dao.TimeSeriesDAO;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;
import com.prelert.data.DatePagingLoadConfig;
import com.prelert.data.DatePagingLoadResult;
import com.prelert.data.TimeFrame;
import com.prelert.data.TimeSeriesDataPoint;
import com.prelert.data.UsageRecord;
import com.prelert.service.TimeSeriesGXTPagingService;


/**
 * Server-side implementation of the service for GXT paging loaders for
 * retrieving time series data.
 * @author Pete Harverson
 */

@SuppressWarnings("serial")
public class TimeSeriesGXTPagingServiceImpl extends RemoteServiceServlet
        implements TimeSeriesGXTPagingService
{
	static Logger logger = Logger.getLogger(TimeSeriesGXTPagingServiceImpl.class);
	
	private TimeSeriesDAO		m_TimeSeriesDAO;
	private TransactionTemplate	m_TxTemplate;
	
	
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
	 * Sets the transaction manager to be used when running queries and updates
	 * to the Prelert database within transactions.
	 * @param txManager Spring PlatformTransactionManager to manage database transactions.
	 */
	public void setTransactionManager(PlatformTransactionManager txManager)
	{
		m_TxTemplate = new TransactionTemplate(txManager);
		m_TxTemplate.setReadOnly(true);
		m_TxTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
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
	 * Returns the time series data points for the given metric between the supplied
	 * start and end times.
	 * @param dataType type of time series data e.g. system_udp or p2psmon_servers.
	 * @param metric the time series metric.
	 * @param minTime minimum date/time to include.
	 * @param maxTime maximum date/time to include.
	 * @param source optional source name.
	 * @param attributeName optional name of attribute on which to filter.
	 * @param attributeValue value of optional attribute on which to filter.
	 * @return list of time series data points.
	 */
	@Override
	public DatePagingLoadResult<UsageRecord> getDataPoints(String dataType,
	        String metric, DatePagingLoadConfig loadConfig, String source,
	        String attributeName, String attributeValue)
	{
		// Run all the DB queries within a transaction.
		final TimeFrame timeFrame = loadConfig.getTimeFrame();
		Date loadDate = loadConfig.getDate();
		
		final String timeSeriesDataType = dataType;
		final String metricName = metric;
		final String sourceName = source;
		final String attrName = attributeName;
		final String attrVal = attributeValue;
		
		// If no date is supplied, for example, on opening the Usage Window from
		// the desktop for the first time, show the latest usage in the DB.
		if (loadDate == null)
		{
			loadDate = m_TimeSeriesDAO.getLatestTime(dataType, sourceName);
			
			if (timeFrame == TimeFrame.WEEK)
			{
				// Set the latest day to be last day of the week shown.
				GregorianCalendar calendar = new GregorianCalendar();
				calendar.setTime(loadDate);
				calendar.add(Calendar.DAY_OF_MONTH, -6);
				loadDate = calendar.getTime();
			}
		}
		
		final Date date = loadDate;
		
    	Object pagingLoadResult = m_TxTemplate.execute(new TransactionCallback(){

            public DatePagingLoadResult<UsageRecord> doInTransaction(TransactionStatus status)
            {
            	List<TimeSeriesDataPoint> dataPoints = null;
            	Date minTime = null;
            	
				if (date != null)
				{
					// Obtain the minimum and maximum times for the time series queries.
					minTime = getQueryMinimumTime(timeFrame, date);
					Date maxTime = getQueryMaximumTime(timeFrame, date);
					logger.debug("getDataPoints() for date: " + date + 
							" min/max times: " + minTime + " / " + maxTime);
					
					switch (timeFrame)
					{
						case ALL:
						case WEEK:
							dataPoints = m_TimeSeriesDAO.getDataPointsForWeek(
									timeSeriesDataType, metricName,  minTime, maxTime, 
									sourceName, attrName, attrVal);
							break;
							
						case DAY:
							dataPoints = m_TimeSeriesDAO.getDataPointsForDay(
									timeSeriesDataType, metricName, minTime, maxTime, 
									sourceName, attrName, attrVal);
							break;
							
						case HOUR:
						case MINUTE:
						case SECOND:
							dataPoints = m_TimeSeriesDAO.getDataPoints(
									timeSeriesDataType, metricName, minTime, maxTime, 
									sourceName, attrName, attrVal);
							break;
					
					}
				}
				
				List<UsageRecord> records = pointsToUsageRecords(dataPoints);
				
				Date startDate = m_TimeSeriesDAO.getEarliestTime(timeSeriesDataType, sourceName);
				Date endDate = m_TimeSeriesDAO.getLatestTime(timeSeriesDataType, sourceName);
				
				// Set the load result time to be the query minimum time
				// i.e. the start of the query week/day/hour.
				Date loadResultTime = minTime;
				
				return new DatePagingLoadResult<UsageRecord>(records, timeFrame,
						loadResultTime, startDate, endDate);
            }
    	});
    	
    	return (DatePagingLoadResult<UsageRecord>)pagingLoadResult;
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
	
	
	/**
	 * Converts a list of TimeSeriesDataPoint objects to a list of UsageRecord objects.
	 * @return list of UsageRecord objects.
	 */
	protected List<UsageRecord> pointsToUsageRecords(List<TimeSeriesDataPoint> dataPoints)
	{
		ArrayList<UsageRecord> usageRecords = new ArrayList<UsageRecord>();
		
		if (dataPoints != null)
		{
			UsageRecord	record;
			
			for (TimeSeriesDataPoint point : dataPoints)
			{
				record = new UsageRecord();
				record.set("time", point.getTime());
				record.set("value", point.getValue());	
				usageRecords.add(record);
			}
		}
		
		return usageRecords;
		
	}
	
}
