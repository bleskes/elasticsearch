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

package com.prelert.service;

import java.util.Date;
import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;
import com.prelert.data.MetricPath;
import com.prelert.data.TimeSeriesConfig;
import com.prelert.data.TimeSeriesDataPoint;


/**
 * Defines the methods for the interface to the Time Series Query service.
 * @author Pete Harverson
 */
public interface TimeSeriesGXTPagingService extends RemoteService
{
	
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
	 * Returns the data points for the specified time series. The data is aggregated 
	 * according to the span between the minimum and maximum times in the supplied 
	 * <code>TimeSeriesConfig</code> so that a maximum of around 750 points will be returned.
	 * @param config TimeSeriesConfig object encapsulating the properties of the
	 * time series such as the type, source, metric, start and end times.
	 * @param includeFeatures flag indicating whether time series features should
	 * 	be included in the returned data points.
	 * @return list of time series data points.
	 */
	public List<TimeSeriesDataPoint> getDataPoints(TimeSeriesConfig config, 
			boolean includeFeatures);
		
	
	/**
	 * Returns the date/time of the latest record in the database for the
	 * specified time series.
	 * @param config TimeSeriesConfig object encapsulating the properties of the
	 * time series such as the type, source and metric.
	 * @return date/time of latest record.
	 */
	public Date getLatestTime(TimeSeriesConfig config);
	
	
	/**
	 * Returns the configuration data for the time series containing the feature
	 * with the specified id.
	 * @param id the unique evidence ID for the time series feature.
	 * @return configuration of the corresponding time series, or <code>null</code>
	 * 	if no time series is found containing a feature with the specified id.
	 */
	public TimeSeriesConfig getConfigurationForFeature(int id);
	
	
	/**
	 * Returns the unique metric path for the time series identified by the 
	 * specified time series id. 
	 * @param id unique time series id.
	 * @return the <code>MetricPath</code> or <code>null</code> if no time series
	 * 	exists with the specified id.
	 */
	public MetricPath getMetricPathFromTimeSeriesId(int id);
	
	
	/**
	 * Returns the unique metric path for the time series containing the feature
	 * with the specified id. 
	 * @param evidenceId feature evidence id.
	 * @return the <code>MetricPath</code> or <code>null</code> if no time series
	 * 	exists with the specified id.
	 */
	public MetricPath getMetricPathFromFeatureId(int evidenceId);
}
