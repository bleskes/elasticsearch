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

package com.prelert.service;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;
import com.prelert.data.DatePagingLoadConfig;
import com.prelert.data.DatePagingLoadResult;
import com.prelert.data.UsageRecord;


public interface TimeSeriesGXTPagingService extends RemoteService
{
	/**
	 * Returns the list of available sources of time series data,
	 * ordered by name, for the given data type.
	 * @param dataType type of time series data e.g. system_udp or p2psmon_servers.
	 * @return the list of available sources.
	 */
	public List<String> getSourcesOrderByName(String dataType);
	
	
	/**
	 * Returns a list of the distinct values for the attribute with the given name
	 * for the specified data type e.g. the values of the 'username' attribute
	 * for p2psmon_users data.
	 * @param dataType type of time series data e.g. system_udp or p2psmon_servers.
	 * @param attributeName name of attribute for which to return the values.
	 * @param source optional source name.
	 * @return a list of the distinct values for the attribute.
	 */
	public List<String>	getAttributeValues(String dataType, String attributeName, String source);	
	
	
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
	public DatePagingLoadResult<UsageRecord> getDataPoints(
			String dataType, String metric, DatePagingLoadConfig config, 
			String source, String attributeName, String attributeValue);
}
