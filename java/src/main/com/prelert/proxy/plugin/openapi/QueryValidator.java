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

package com.prelert.proxy.plugin.openapi;

import static com.prelert.proxy.plugin.openapi.OpenApiPlugin.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;


/**
 * Class with static methods for validating the queries used in the
 * OpenApiPlugin.
 * A the valid queries must contain the following:
 * <ul>
 * <li>Both queries must select Source & Datetime fields.</li>
 * <li>The allTimeSeriesQuery must return at least one field starting with {@link OpenApiPlugin#METRIC_COLUMN_ID}.</li>
 * <li>All attributes (start with {@link OpenApiPlugin#ATTRIBUTE_COLUMN_ID}) in 
 * timeSeriesQuery must be a field in allTimeSeriesQuery.</li>
 * <li>All keys (start with {@link OpenApiPlugin#KEY_COLUMN_ID}) in 
 * timeSeriesQuery must be a field in allTimeSeriesQuery.</li> 
 * <li>All metrics (start with {@link OpenApiPlugin#METRIC_COLUMN_ID}) in 
 * timeSeriesQuery must be a field in allTimeSeriesQuery.</li>
 * </ul>
 */
public class QueryValidator 
{
	private static Logger s_Logger = Logger.getLogger(QueryValidator.class);

	/**
	 * Validate the pair of queries used by the OpenApiPlugin. 
	 * Error messages are logged. 
	 * 
	 * @param allTimeSeriesQuery
	 * @param timeSeriesQuery
	 * @return true if queries are valid.
	 */
	static public boolean validateOpenApiQueryPair(String allTimeSeriesQuery, String timeSeriesQuery)
	{
		boolean gotDateTokens = checkQueryForDateParams(allTimeSeriesQuery) && 
									checkQueryForDateParams(timeSeriesQuery);
		
		if (gotDateTokens == false)
		{
			return false;
		}
		
		boolean gotSelects = checkSelects(allTimeSeriesQuery, true) && 
									checkSelects(timeSeriesQuery, false);
		
		if (gotSelects == false)
		{
			return false;
		}
		
		return checkAttributesMatch(allTimeSeriesQuery, timeSeriesQuery) &&
				checkKeysMatch(allTimeSeriesQuery, timeSeriesQuery) &&
				checkMetricsMatch(allTimeSeriesQuery, timeSeriesQuery);
	}
	
	
	
	
	/**
	 * A valid query must contain ':StartTime' and ':EndTime' tokens.
	 * If not an error is logged and false is returned. 
	 * 
	 * @param queryString
	 * @return true if the start and end time markers are found in 
	 * 	<code>queryString</code>.
	 */
	static private boolean checkQueryForDateParams(String queryString)
	{
		// Sanity tests on the query.
		String startTimeToken = QUERY_ARG_START_MARKER + START_TIME_TOKEN;
		if (!queryString.contains(startTimeToken))
		{
			String error = String.format("Invalid query must contain a %s token", startTimeToken);
			s_Logger.error(error);
			return false;
		}

		String endTimeToken = QUERY_ARG_START_MARKER + END_TIME_TOKEN;
		if (!queryString.contains(endTimeToken))
		{
			String error = String.format("Invalid query must contain a %s token", endTimeToken);
			s_Logger.error(error);
			return false;
		}
			
		return true;
	}
	
	
	/**
	 * Checks that the <code>query</code> contains the minimum required 
	 * fields in its select statement.
	 * It can contain a {@link OpenApiPlugin#SOURCE_COLUMN} field and must contain
	 * {@link OpenApiPlugin#DATETIME_COLUMN} and at least one metric starting with 
	 * {@link OpenApiPlugin#METRIC_COLUMN_ID}.
	 * 
	 * @param query The SQL query
	 * @param checkForSource If true check for a {@link #SOURCE_COLUMN Source} field.
	 * @return true if the required fields are returned in select statement.
	 */
	static private boolean checkSelects(String query, boolean checkForSource)
	{
		// find the SQL 'FROM' keyword but do a case insensitive search.
		String lowerCaseQuery = query.toLowerCase();
		int fromIndex = lowerCaseQuery.indexOf("from");
		if (fromIndex < 0)
		{
			s_Logger.error("Could not find 'from' keyword in query = " + query);
			return false;
		}
		
		String selectStatement = query.substring(0, fromIndex);
		
		boolean gotSource = !checkForSource;
		if (checkForSource)
		{
			gotSource = selectStatement.contains(SOURCE_COLUMN); 
			if (gotSource == false)
			{
				s_Logger.error(String.format("Could not find field %s in query = %s", SOURCE_COLUMN, query));
			}
		}
		
		boolean gotDatetime = selectStatement.contains(DATETIME_COLUMN);
		if (gotDatetime == false)
		{
			s_Logger.error(String.format("Could not find field %s in query = %s", DATETIME_COLUMN, query));
		}
		
		boolean gotMetric = selectStatement.contains(METRIC_COLUMN_ID);
		if (gotMetric == false)
		{
			s_Logger.error(String.format("Could not find field %s in query = %s", METRIC_COLUMN_ID, query));
		}
		
		return gotSource && gotDatetime && gotMetric;
	}
	
	
	/**
	 * Returns true if every attribute used in <code>oneQuery</code> is 
	 * present in the <code>allQuery</code>.
	 * 
	 * @param allQuery
	 * @param oneQuery
	 * @return
	 */
	static private boolean checkAttributesMatch(String allQuery, String oneQuery)
	{
		// find the SQL 'FROM' keyword but do a case insensitive search.
		String lowerCaseQuery = allQuery.toLowerCase();
		int fromIndex = lowerCaseQuery.indexOf("from");
		if (fromIndex < 0)
		{
			s_Logger.error("Could not find 'from' keyword in query = " + allQuery);
			return false;
		}
		
		
		// Find all the attributes used in the one time series query and 
		// check they are returned as fields in the all query.
		Pattern pattern = Pattern.compile(QUERY_ARG_START_MARKER + ATTRIBUTE_COLUMN_ID + "(\\w+)");
		Matcher matcher = pattern.matcher(oneQuery);
		boolean gotAttributes = true;
		while (matcher.find())
		{
			if (matcher.groupCount() > 0)
			{
				String attribute = matcher.group(1);
				if (allQuery.contains(ATTRIBUTE_COLUMN_ID + attribute) == false)
				{
					String errorMsg = String.format("The attribute %s%s is not a field returned in the all " +
							"time series query", ATTRIBUTE_COLUMN_ID, attribute);
					s_Logger.error(errorMsg);

					gotAttributes = false;
					break;				
				}
			}
		}
		
		return gotAttributes;
	}
	
	
	/**
	 * Returns true if every key used in <code>oneQuery</code> is 
	 * present in the <code>allQuery</code>.
	 * 
	 * @param allQuery
	 * @param oneQuery
	 * @return
	 */
	static private boolean checkKeysMatch(String allQuery, String oneQuery)
	{
		// find the SQL 'FROM' keyword but do a case insensitive search.
		String lowerCaseQuery = allQuery.toLowerCase();
		int fromIndex = lowerCaseQuery.indexOf("from");
		if (fromIndex < 0)
		{
			s_Logger.error("Could not find 'from' keyword in query = " + allQuery);
			return false;
		}
		
		
		// Find all the attributes used in the one time series query and 
		// check they are returned as fields in the all query.
		Pattern pattern = Pattern.compile(QUERY_ARG_START_MARKER + KEY_COLUMN_ID + "(\\w+)");
		Matcher matcher = pattern.matcher(oneQuery);
		boolean gotKeys = true;
		while (matcher.find())
		{
			if (matcher.groupCount() > 0)
			{
				String key = matcher.group(1);
				if (allQuery.contains(KEY_COLUMN_ID + key) == false)
				{
					String errorMsg = String.format("The key %s%s is not a field returned in the all " +
							"time series query", KEY_COLUMN_ID, key);
					s_Logger.error(errorMsg);

					gotKeys = false;
					break;				
				}
			}
		}
		
		return gotKeys;
	}
	
	
	/**
	 * Returns true if every metric used in <code>oneQuery</code> is 
	 * present in the <code>allQuery</code>.
	 * 
	 * @param allQuery
	 * @param oneQuery
	 * @return
	 */
	static private boolean checkMetricsMatch(String allQuery, String oneQuery)
	{
		// find the SQL 'FROM' keyword but do a case insensitive search.
		String lowerCaseQuery = allQuery.toLowerCase();
		int fromIndex = lowerCaseQuery.indexOf("from");
		if (fromIndex < 0)
		{
			s_Logger.error("Could not find 'from' keyword in query = " + allQuery);
			return false;
		}
		
		
		// Find all the attributes used in the one time series query and 
		// check they are returned as fields in the all query.
		Pattern pattern = Pattern.compile(METRIC_COLUMN_ID + "(\\w+)");
		Matcher matcher = pattern.matcher(oneQuery);
		boolean gotMetrics = true;
		while (matcher.find())
		{
			if (matcher.groupCount() > 0)
			{
				String metric = matcher.group(1);
				if (allQuery.contains(METRIC_COLUMN_ID + metric) == false)
				{
					String errorMsg = String.format("The metric %s%s is not a field returned in the all " +
							"time series query", METRIC_COLUMN_ID, metric);
					s_Logger.error(errorMsg);

					gotMetrics = false;
					break;				
				}
			}
		}
		
		return gotMetrics;
	}
}
