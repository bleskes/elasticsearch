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

package demo.app.service;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import demo.app.data.DatePagingLoadConfig;
import demo.app.data.DatePagingLoadResult;
import demo.app.data.TimeSeriesConfig;
import demo.app.data.TimeSeriesDataPoint;
import demo.app.data.UsageRecord;


/**
 * Defines the methods to be implemented by the asynchronous client interface
 * to the Time Series Query service, for use by Ext GWT paging loaders.
 * @author Pete Harverson
 */
public interface TimeSeriesGXTPagingServiceAsync
{
	/**
	 * Returns the list of available sources of time series data,
	 * ordered by name, for the given data type.
	 * @param dataType type of time series data e.g. system_udp or p2psmon_servers.
	 * @param callback callback object to receive a response from the remote procedure call.
	 */
	public void getSourcesOrderByName(
			String dataType, AsyncCallback<List<String>> callback);
	
	
	/**
	 * Returns a list of the distinct values for the attribute with the given name
	 * for the specified data type e.g. the values of the 'username' attribute
	 * for p2psmon_users data.
	 * @param dataType type of time series data e.g. system_udp or p2psmon_servers.
	 * @param attributeName name of attribute for which to return the values.
	 * @param source optional source name.
	 * @param callback callback object to receive a response from the remote procedure call.
	 */
	public void	getAttributeValues(String dataType, String attributeName, 
			String source, AsyncCallback<List<String>> callback);	
	
	
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
	 * @param callback callback object to receive a response from the remote procedure call.
	 */
	public void getDataPoints(
			String dataType, String metric, DatePagingLoadConfig config, 
			String source, String attributeName, String attributeValue,
			AsyncCallback<DatePagingLoadResult<UsageRecord>> callback);
	
	
	
	/**
	 * Returns the data points for the specified time series.
	 * @param config TimeSeriesConfig object encapsulating the properties of the
	 * time series such as the type, source, metric, start and end times.
	 * @param includeFeatures flag indicating whether time series features should
	 * 	be included in the returned data points.
	 * @param callback callback object to receive a response from the remote procedure call.
	 */
	public void getDataPoints(TimeSeriesConfig config, boolean includeFeatures,
			AsyncCallback<List<TimeSeriesDataPoint>> callback);
}
