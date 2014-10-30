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

package com.prelert.proxy.plugin.introscope;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.prelert.data.AnalysisDuration;
import com.prelert.data.TimeSeriesData;
import com.prelert.data.TimeSeriesDataPoint;
import com.prelert.data.AnalysisDuration.ErrorState;
import com.prelert.proxy.data.introscope.MetricGroup;
import com.prelert.proxy.plugin.introscope.IntroscopeConnection.ConnectionException;


/***
 * This class finds the optimal length in seconds for a given set
 * of queries. The optimal length returns points at the greatest 
 * throughput rate without causing OutOfMemoryExceptions.
 * 
 * All the queries will be executed for each value in the 
 * <code>QueryDurations</code> list whilst monitoring the EM's 
 * Overall Capacity metric. If the value of <code>MaxOverallCapacity</code>
 * is exceeded then that query length is too long and the previous smaller
 * value is used.
 * 
 * This class protects against OutOfMemoryExceptions on the EM by 
 * monitoring the average Overall Capacity after each set of test
 * queries. If the value of <code>MaxOverallCapacity</code> is 
 * exceeded while querying then queries of that length will not be
 * used. 
 */
public class OptimalUsage 
{
	static final private Logger s_Logger = Logger.getLogger(OptimalUsage.class);
	
	private int m_MaxOverallCapacity;
	private List<Integer> m_QueryDurations;
	private int m_MaxHoursToFindData;
	private int m_MaxTimeToRunEstimateSecs;
	private int m_MaxPointIntervalSecs;
	
	
	public OptimalUsage()
	{
		m_MaxOverallCapacity = 70;
		m_QueryDurations = Arrays.asList(new Integer[]{2,4,8});
		m_MaxHoursToFindData = 6;
		m_MaxTimeToRunEstimateSecs = 120;
		m_MaxPointIntervalSecs = 60;
	}
	
	
	/**
	 * The set of query times in minutes that will be used to find the
	 * optimal query length. 
	 * 
	 * Defaults to [2, 4, 8]
	 * @return
	 */
	public List<Integer> getTestQueryDurations()
	{
		return m_QueryDurations;
	}
	
	public void setTestQueryDurations(List<Integer> value)
	{
		m_QueryDurations = value;
		Collections.sort(m_QueryDurations);
	}
	
	/**
	 * The maximum value of the Enterprise Overall Capacity
	 * metric that should not be exceeded while querying for data. 
	 * @return
	 */
	public int getMaxOverallCapacity()
	{
		return m_MaxOverallCapacity;
	}
	
	public void setMaxOverallCapacity(int value)
	{
		m_MaxOverallCapacity = value;
	}
	
	
	/**
	 * If no data is found at the time passed to the 
	 * {@link #optimalQueryLength} function then it will search for
	 * data at hourly increments going forward until this value 
	 * is reached.
	 * @return
	 */
	public int getMaxHoursToFindData()
	{
		return m_MaxHoursToFindData;
	}
	
	public void setMaxHoursToFindData(int value)
	{
		m_MaxHoursToFindData = value;
	}
	
	/**
	 * Get the maximum amount of time the {@link #optimalQueryLength} 
	 * function should take to execute. The function will try to 
	 * exit if it takes longer than this value.
	 * Defaults to 120 seconds.
	 * @return
	 */
	public int getMaxTimeToRunEstimateSecs()
	{
		return m_MaxTimeToRunEstimateSecs;
	}
	
	public void setMaxTimeToRunEstimateSecs(int value)
	{
		m_MaxTimeToRunEstimateSecs = value;
	}
	
	
	/**
	 * The maximum granularity of data points in seconds.
	 * Defaults to 60.
	 * @return
	 */
	public int getMaxPointIntervalSecs()
	{
		return m_MaxPointIntervalSecs;
	}
	
	public void setMaxPointIntervalSecs(int value)
	{
		m_MaxPointIntervalSecs = value;
	}
	
	
	/**
	 * Returns the optimal query length and the time it takes
	 * to finish those queries. 
	 * 
	 * If no data is returned by the queries then the process is
	 * invalid and the returned <code>TimeAndQueryLength</code> 
	 * object will have a queryDuration of -1 and QueryLengthSecs of 0.
	 * 
	 * @param connection
	 * @param queries
	 * @param timeToPull
	 * @return
	 * @throws ConnectionException 
	 */
	public AnalysisDuration optimalQueryLength(IntroscopeConnection connection, 
														List<MetricGroup> queries, 
														Date timeToPull) 
	throws ConnectionException
	{
		s_Logger.info(String.format("Finding the optimal query length for %d queries at time %s using connection %s", 
						queries.size(), timeToPull, connection.getConnectionConfig()));
		
		if (m_QueryDurations.size() == 0)
		{
			throw new IllegalStateException("There must be at least one candiate Query " + 
					"Duration defined before the optimal period can be found.");
		}
		
		Date estimateStart = new Date();
		
		long queryDuration = -1;
		int longestSuccessfulQueryLength = m_QueryDurations.get(0);
		
		boolean gotSomeData = false;
		boolean dataAtMinInterval = false;
		int dataPointInterval = -1;
		
		Date queryEnd = new Date(timeToPull.getTime() + m_QueryDurations.get(0) * 60 * 1000);

		s_Logger.info("Find query duration for queries with length = " + m_QueryDurations.get(0) + 
						" minutes at time " + timeToPull);
		
		Collection<TimeSeriesData> queryResult = null;

		// First check for data.
		Date timerStart = new Date();
		
		for (MetricGroup group : queries)
		{
			queryResult = connection.getMetricData(
					group.getAgent(), group.getMetric(), 
					timeToPull, queryEnd, IntroscopeConnection.DEFAULT_INTERVAL);
			
			if (queryResult.size() > 0)
			{
				gotSomeData = true;
				dataAtMinInterval = checkPointsAtMinInterval(queryResult.iterator().next());
				dataPointInterval = intervalBetweenTimeSeriesPoints(queryResult.iterator().next());
			}
		}
		
		Date timerEnd = new Date();
		
		
		Date modifiedStartTime = null;
		// No data at time so go searching for it.
		if (gotSomeData == false || dataAtMinInterval == false)
		{
			// Go find some data
			Calendar cal = Calendar.getInstance();
			cal.setTime(timeToPull);

			for (int i=0; i<m_MaxHoursToFindData; i++)
			{
				cal.add(Calendar.HOUR_OF_DAY, 1);

				s_Logger.info("No valid data at previous time. Advancing 1 hour and querying at " + cal.getTime());
				
				queryEnd = new Date(cal.getTime().getTime() + m_QueryDurations.get(0) * 60 * 1000);

				timerStart = new Date();
				
				for (MetricGroup group : queries)
				{
					queryResult = connection.getMetricData(group.getAgent(), group.getMetric(), 
							cal.getTime(), queryEnd, IntroscopeConnection.DEFAULT_INTERVAL);

					if (queryResult.size() > 0)
					{
						gotSomeData = true; 
						
						dataAtMinInterval = checkPointsAtMinInterval(queryResult.iterator().next());
						dataPointInterval = intervalBetweenTimeSeriesPoints(queryResult.iterator().next());
					}
				}

				timerEnd = new Date();
				
				if (gotSomeData)
				{
					s_Logger.info("Found data at time " + cal.getTime());
					
					modifiedStartTime = cal.getTime();
					timeToPull = cal.getTime();
					
					if (dataAtMinInterval)
					{
						break;
					}
					else
					{
						s_Logger.info("Data points at time " + cal.getTime() + " not at the minimum interval.");
					}
				}
			}			
		}
		
		
		// If no data can be found return
		// else calc the duration etc. 
		if (gotSomeData == false) 
		{
			AnalysisDuration result = new AnalysisDuration(-1, -1, null, ErrorState.NO_DATA);
			s_Logger.info("No data found. optimalQueryLength returning = " + result);
			return result; 
		}
		else if (dataAtMinInterval == false)
		{
			AnalysisDuration result = new AnalysisDuration(-1, -1, null, ErrorState.DATA_AT_TOO_LARGE_INTERVAL);
			result.setRequiredDataPointIntervalSecs(m_MaxPointIntervalSecs);
			result.setActualDataPointIntervalSecs(dataPointInterval);

			s_Logger.info("Data Points are not at the minimal interval. " +
					"optimalQueryLength returning = " + result);

			return result; 
		}
		else
		{
			// got some data
			queryDuration = timerEnd.getTime() - timerStart.getTime();
			longestSuccessfulQueryLength = m_QueryDurations.get(0);

			int cap = getAverageOverallCapacity(timerStart.getTime(), timerEnd.getTime(), connection);
			if (cap > getMaxOverallCapacity())
			{
				AnalysisDuration result =  new AnalysisDuration(queryDuration, 
						longestSuccessfulQueryLength * 60,
						modifiedStartTime);
				result.setActualDataPointIntervalSecs(dataPointInterval);
				s_Logger.info("optimalQueryLength returning = " + result);
				
				return result; 
			}
		}
		
	
		// Now time getting data at the remaining intervals
		for (int i=1; i<m_QueryDurations.size(); i++)
		{
			s_Logger.info("Find query duration for queries with length = " + m_QueryDurations.get(i) + 
					" minutes at time " + timeToPull);
			
			if (new Date().getTime() - estimateStart.getTime() > (m_MaxTimeToRunEstimateSecs * 1000))
			{
				s_Logger.info("optimalQueryLength function has been running for longer than " +
						"MaxTimeToRunEstimateSecs = " + m_MaxTimeToRunEstimateSecs + ". " + 
						"Returning now with the last good configuration. ");
				break;
			}
			
			queryEnd = new Date(timeToPull.getTime() + m_QueryDurations.get(i) * 60 * 1000);

			timerStart = new Date();
			for (MetricGroup group : queries)
			{
				queryResult = connection.getMetricData(group.getAgent(), group.getMetric(), 
									timeToPull, queryEnd, IntroscopeConnection.DEFAULT_INTERVAL);
				
				if (queryResult.size() > 0)
				{
					gotSomeData = true;
				}
			}
			timerEnd = new Date();

			int cap = getAverageOverallCapacity(timerStart.getTime(), timerEnd.getTime(), connection);
			if (cap > getMaxOverallCapacity())
			{
				break;
			}
			
			queryDuration = timerEnd.getTime() - timerStart.getTime();
			longestSuccessfulQueryLength = m_QueryDurations.get(i);			
		}
		
		// Queries never returned data so invalid estimate.
		if (gotSomeData == false)
		{
			AnalysisDuration result = new AnalysisDuration(-1, -1, null, ErrorState.NO_DATA);
			s_Logger.info("optimalQueryLength returning = " + result);
			return result;
		}
		
		
		AnalysisDuration result = new AnalysisDuration(queryDuration,
														longestSuccessfulQueryLength * 60,
														modifiedStartTime);
		result.setActualDataPointIntervalSecs(dataPointInterval);
		s_Logger.info("optimalQueryLength returning = " + result);

		return result;
	}
	
	
	/**
	 * Returns the average overall capacity for the given time period.
	 * If no data is available
	 * 
	 * @param start - Epoch start time.
	 * @param end - Epoch end time.
	 * @param connection - Introscope connection object.
	 * @return
	 * @throws ConnectionException
	 */
	private int getAverageOverallCapacity(long start, long end, 
						IntroscopeConnection connection)
	throws ConnectionException
	{
		long diffSecs = (end - start) / 1000;
		long diffMins = ((diffSecs -1) / 60 ) + 1;
		
		Collection<TimeSeriesData> queryResults = connection.getMetricDataForLastNMinutes(
							IntroscopeLoadMonitor.CUSTOM_METRIC_AGENT, IntroscopeLoadMonitor.OVERALL_CAPACITY_METRIC,
							(int)diffMins, IntroscopeConnection.DEFAULT_INTERVAL);
		
		
		if (queryResults.size() == 0)
		{
			return -1;
		}
		
		
		List<TimeSeriesDataPoint> points = queryResults.iterator().next().getDataPoints();
		Collections.sort(points);
		
		double sum = 0.0;
		int count = 0;
		for (TimeSeriesDataPoint pt : points)
		{
			if (pt.getTime() >= start && pt.getTime() <= end)
			{
				sum = sum + pt.getValue();
				count++;
			}
		}
		
		if (count == 0)
		{
			return -1;
		}
		else
		{
			return (int)(sum /count);
		}
		
	}	
	
	
	/**
	 * Checks that the points in the time series are are no more
	 * than MinPointIntervalSecs apart. If the series only has less 
	 * than 2 points false is returned.
	 * 
	 * @param timeSeriesData
	 * @return true if the points are MinPointIntervalSecs apart.
	 */
	private boolean checkPointsAtMinInterval(TimeSeriesData timeSeriesData)
	{
		// must have at least 2 points
		if (timeSeriesData.getDataPoints().size() < 2)
		{
			return false;
		}
		
		Collections.sort(timeSeriesData.getDataPoints());
		long time1 = timeSeriesData.getDataPoints().get(0).getTime();
		long time2 = timeSeriesData.getDataPoints().get(1).getTime();
		
		if (time2 - time1 > m_MaxPointIntervalSecs * 1000)
		{
			return false;
		}
		
		return true;
	}
	
	
	/**
	 * Return the interval between consecutive points. 
	 * If there is only 1 point in <code>timeSeriesData</code>
	 * then return -1.
	 * 
	 * @param timeSeriesData
	 * @return The interval or -1 if it cannot be calculated
	 */
	private int intervalBetweenTimeSeriesPoints(TimeSeriesData timeSeriesData)
	{
		// must have at least 2 points
		if (timeSeriesData.getDataPoints().size() < 2)
		{
			return -1;
		}
		
		Collections.sort(timeSeriesData.getDataPoints());
		long time1 = timeSeriesData.getDataPoints().get(0).getTime();
		long time2 = timeSeriesData.getDataPoints().get(1).getTime();
		
		return (int)((time2 - time1) / 1000);
	}
	
}
