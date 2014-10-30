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

package com.prelert.proxy.plugin.jdbc.netqos;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.log4j.Logger;

import com.prelert.data.TimeSeriesData;
import com.prelert.proxy.inputmanager.querymonitor.QueryTookTooLongException;
import com.prelert.proxy.plugin.jdbc.JdbcPlugin;

/**
 * NetQoS MySQL JDBC plugin.
 * 
 * This plugin is broadly the same as the generic JdbcPlugin except that
 * because the NetQoS query only works as a union of multiple select statements
 * the date parameters have to be set multiple times once for each union.
 * See {@link #getAllDataPointsForTimeSpan} 
 */
public class NetQoSPlugin extends JdbcPlugin 
{
	private static Logger s_Logger = Logger.getLogger(NetQoSPlugin.class);
	
	
	/**
	 * Find the time zone the database is in by comparing 
	 * its local date time to UTC date time. If that fails 
	 * the default timezone (timezone this program is running 
	 * in) is returned.
	 * 
	 *  This function is specific to MySQL databases.
	 * 
	 * @return The TimeZone object representing the server's timezone
	 * 			or the default TimeZone if it cannot be found.
	 */
	public TimeZone getTimeZone()
	{
		final String TIMEZONE_QUERY = "SELECT IF(@@time_zone = 'SYSTEM', @@system_time_zone, @@time_zone);";
		
		try
		{
			PreparedStatement statement = m_Connection.prepareStatement(TIMEZONE_QUERY);	
			ResultSet resultSet = statement.executeQuery();
			
			if (resultSet.next())
			{
				String timeZoneId = resultSet.getString(1);
				TimeZone tz = TimeZone.getTimeZone(timeZoneId);
				
				s_Logger.info("SQL Server Timezone = " + tz.getID());

				return tz;
			}
		}
		catch (SQLException e) 
		{
			s_Logger.error("Error finding the SQL Server's timezone: " + e);
		}
		
		return TimeZone.getDefault();
	}
	
	
	/**
	 * The NetQoS query only works as a union of multiple select statements.
	 * For this to work the date parameters have to be set multiple times 
	 * once for each union.
	 * 
	 * @param minTime - Query start time
	 * @param maxTime - Query end time
	 * @param intervalSecs - This parameter is unused.
	 * @return
	 */
	@Override
	public Collection<TimeSeriesData> getAllDataPointsForTimeSpan(Date minTime,
													Date maxTime, int intervalSecs) 
	throws QueryTookTooLongException 
	{
		s_Logger.info(String.format("getAllDataPointsForTimeSpan(%s, %s, %d)", 
								minTime, maxTime, intervalSecs));

		List<TimeSeriesData> timeSeriesData = new ArrayList<TimeSeriesData>();
		try 
		{

			if (getAllTimeSeriesDataQuery() != null)
			{
				java.sql.Timestamp sqlStart = new java.sql.Timestamp(minTime.getTime());
				java.sql.Timestamp sqlEnd = new java.sql.Timestamp(maxTime.getTime());

				PreparedStatement statement = m_Connection.prepareStatement(getAllTimeSeriesDataQuery());
				
				// Hack to get it working with the netqos queries where the same 2
				// parameters must be repeated.
				int index = 1;
				for (int i=0; i<15; i++)
				{
					statement.setTimestamp(index++, sqlStart, getServerTimeZoneCalendar());
					statement.setTimestamp(index++, sqlEnd, getServerTimeZoneCalendar());
				}
				
				ResultSet resultSet = statement.executeQuery();
				timeSeriesData.addAll(processTimeSeriesQueryResults(resultSet, getServerTimeZoneCalendar()));
			}
		}
		catch (SQLException e) 
		{
			s_Logger.error("Exception in getAllDataPointsForTimeSpan(): " + e);
		}
	
		s_Logger.info("getAllDataPointsForTimeSpan returned " + timeSeriesData.size() + " time series.");
		
		return timeSeriesData;
	}
	
}
