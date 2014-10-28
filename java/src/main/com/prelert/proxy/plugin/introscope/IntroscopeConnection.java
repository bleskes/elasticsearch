/************************************************************
 *                                                          *
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2012     *
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

import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.prelert.data.Notification;
import com.prelert.data.TimeSeriesData;
import com.prelert.proxy.data.SourceConnectionConfig;


/**
 * Abstract class representing the functionality that should be implemented 
 * by a class integrating into Wily Introscope.
 */

public abstract class IntroscopeConnection 
{
	/**
	 * Introscope Connection exception. 
	 */
	public class ConnectionException extends Exception
	{
		private static final long serialVersionUID = -5844136616117348536L;
		
		public ConnectionException(String message)
		{
			super(message);
		}
	}
	
	static public final int DEFAULT_INTERVAL = 15;
	
	protected SourceConnectionConfig m_ConnectionConfig;
	protected volatile boolean m_Connected; 
	
	private String m_AlertDatatype;
	private String m_MetricDatatype;
	
	public IntroscopeConnection()
	{
		m_Connected = false;
	}

	public IntroscopeConnection(SourceConnectionConfig connectionConfig)
	{
		m_ConnectionConfig = connectionConfig;
		m_Connected = false;
	}
	

	/**
	 * Open a connection
	 * @param connectConfig
	 */
	public abstract void connect(SourceConnectionConfig connectConfig) throws Exception;
	
	/**
	 * Disconnect from Introscope.
	 */
	public abstract void logoff();

	
	/**
	 * Returns true if the connection is valid and connected.
	 * @return
	 */
	public abstract boolean isConnected();
	
	/**
	 * Returns the connection details.
	 * @return
	 */
	public SourceConnectionConfig getConnectionConfig()
	{
		return m_ConnectionConfig;
	}
	
	/**
	 * Get all alerts that for module and agent that have changed 
	 * status between the dates start and end.
	 * 
	 * @param start
	 * @param end
	 * @param module
	 * @param agent Can be a null value in which case all the alerts
	 *              for the module are checked.
	 * @param alert
	 * @return
	 * @throws ConnectionException - if the connection errors or the thread is 
	 * 								 interrupted.
	 */
	public abstract List<Notification> getAlerts(Date start, Date end, 
								String module, String agent, String alert)
	throws ConnectionException;
	
	/**
	 * Returns a list of agents matching <code>agentRegex</code>
	 * @param agentRegex
	 * 
	 * @return
	 * @throws ConnectionException - if the connection errors or the thread is 
	 * 								 interrupted.
	 */
	public abstract List<String> listAgents(String agentRegex)
	throws ConnectionException;

	/**
	 * List metrics for agents matching <code>agentRegex</code> and
	 * metrics matching <code>metricRegex</code>.
	 * 
	 * @param agentRegex
	 * @param metricRegex
	 * @return
	 * @throws ConnectionException - if the connection errors or the thread is 
	 * 								 interrupted.
	 */
	public abstract List<String> listMetrics(String agentRegex, String metricRegex)
	throws ConnectionException;
	
	/**
	 * Get the metric data for agents matching <code>agentRegex</code> and metrics
	 * matching <code>metricRegex</code> between the dates <code>start</code> and 
	 * <code>stop</code> at intervals of <code>intervalSecs</code> seconds.
	 * 
	 * @param agentRegex - Can be a non regex string which exactly specifies an agent.
	 * @param metricRegex - Can be a non regex string which exactly specifies an agent.
	 * @param start
	 * @param stop
	 * @param intervalSecs
	 * @return
	 * @throws ConnectionException - if the connection errors or the thread is 
	 * 								 interrupted.
	 */
	public abstract Collection<TimeSeriesData> getMetricData(String agentRegex, String metricRegex,
							Date start, Date stop, int intervalSecs)
	throws ConnectionException;
	

	/**
	 * Overloaded method uses the default interval of 15 seconds.
	 * @param agentRegex
	 * @param metricRegex
	 * @param start
	 * @param stop
	 * @return
	 * @throws ConnectionException - if the connection errors or the thread is 
	 * 								 interrupted.
	 */
	public Collection<TimeSeriesData> getMetricData(String agentRegex, String metricRegex,
													Date start, Date stop)
	throws ConnectionException
	{
		return getMetricData(agentRegex, metricRegex, start, stop, DEFAULT_INTERVAL);
	}
	
	
	/**
	 * Returns metric data for the last N minutes from now at the 
	 * given interval.
	 * 
	 * @param agentRegex
	 * @param metricRegex
	 * @param lastNMinutes - Number of minutes to pull data for.
	 * @param intervalSecs - The point interval
	 * @return
	 * @throws ConnectionException
	 */
	public abstract Collection<TimeSeriesData> getMetricDataForLastNMinutes(String agentRegex, String metricRegex,
									int lastNMinutes, int intervalSecs)
	throws ConnectionException;
	
	
	/**
	 * Overloaded call to {@link #getMetricDataForLastNMinutes(String agentRegex, String metricRegex,int lastNMinutes, int intervalSecs)}
	 * using the default value for intervalSecs of 15.
	 * 
	 * @param agentRegex
	 * @param metricRegex
	 * @param lastNMinutes - Number of minutes to pull data for.
	 * @return
	 * @throws ConnectionException
	 */
	public Collection<TimeSeriesData> getMetricDataForLastNMinutes(String agentRegex, String metricRegex,
													int lastNMinutes)
	throws ConnectionException
	{
		return getMetricDataForLastNMinutes(agentRegex, metricRegex, lastNMinutes, DEFAULT_INTERVAL);
	}
	
	

	/**
	 * Returns a list of all the Management Modules in Introscope.
	 * 
	 * @param moduleRegex - Optional regex to list management modules. If 
	 * 			<code>null</code> the default of '.*' is used.
	 * @return A list of management modules matching the regex argument.
	 * @throws ConnectionException - if the connection errors or the thread is 
	 * 								 interrupted.
	 */
	public abstract List<String> listManagementModules(String moduleRegex)
	throws ConnectionException;
	
	
	/**
	 * The name that will be given to Introscope Alerts (Prelert Notifications).
	 * Alerts will appear in the GUI under the element in the tree with this name.
	 * @return
	 */
	public String getAlertDataType()
	{
		return m_AlertDatatype;
	}
	
	
	public void setAlertDataType(String datatype)
	{
		m_AlertDatatype = datatype;
	}
	
	
	/**
	 * The name that will be given to Introscope Metric Data.
	 * Time series data will appear in the GUI under the element in the tree with this name.
	 * @return
	 */
	public String getMetricDataType()
	{
		return m_MetricDatatype;
	}
	
	
	public void setMetricDataType(String datatype)
	{
		m_MetricDatatype = datatype;
	}
	
	
	/**
	 * Returns string of the login name and host name in the form
	 * 		"[username@host]"
	 * @return
	 */
	protected String getHostUsernameString()
	{
		return "[" + m_ConnectionConfig.getUsername() + "@" + m_ConnectionConfig.getHost() + "]";		
	}
	
	
	/**
	 * Map Introscope Alert status to prelert severity status.
	 * @param status
	 * @return
	 */
	protected int mapStatusToPrelertSeverity(int status)
	{
		// Introscope alert status codes:
		// 0 = No Data
		// 1 = OK
		// 2 = Caution
		// 3 = Danger

		int result;
		switch (status)
		{
			case 0:
				result = 2; // Unknown
				break;

			case 1:
				result = 1; // Clear
				break;

			case 2:
				result = 4; // Minor
				break;

			case 3:
				result = 6; // Critical
				break;

			default:
				result = 2;
				break;
		}

		return result;
	}
	
	
	/**
	 * Convert an Introscope alert status value into a human readable string.
	 * @param status
	 * @return
	 */
	protected String mapStatusToSeverityString(int status)
	{
		String result;
		switch (status)
		{
			case 1:
				result = "Ok";
				break;

			case 2:
				result = "Caution";
				break;

			case 3:
				result = "Danger";
				break;

			default:
				result = "No Data";
				break;
		}
		return result;
	}
	

	/**
	 * This function calculates the adjustment for the CLW daylight
	 * savings bug.
	 * 
	 * Calculates the time difference between the two epochs and 
	 * returns it rounded to the nearest hour.
	 *  
	 * As the points may not lie on exact boundaries a fudge factor 
	 * of 15 minutes is used. 15 minutes was chosen because the oldest 
	 * data in Introscope (older than a month) is typically at 15 minute 
	 * intervals. 
	 * 
	 * @param requestedEpoch - the epoch time of the requested data.
	 * @param returnedEpoch - the actual epoch time of the returned data.
	 * @return 
	 */
	static public int calcDstAdjustment(long requestedEpoch, long returnedEpoch)
	{
		final int TIME_FUDGE_FACTOR_MINS = 15;
		
		// Points are sorted in time order
		int diffMins = (int) (((requestedEpoch - returnedEpoch) / 1000) / 60);
		int timeDifferenceHours = (Math.abs(diffMins) + TIME_FUDGE_FACTOR_MINS) / 60;

		if (diffMins < 0)
		{
			timeDifferenceHours = -timeDifferenceHours;
		}	
		
		return timeDifferenceHours;
	}
}
