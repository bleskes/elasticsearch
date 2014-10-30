/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2013     *
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

package com.prelert.proxy.dao;

import java.rmi.RemoteException;
import java.util.Date;
import java.util.List;

import com.prelert.data.Attribute;
import com.prelert.data.DataSourceType;
import com.prelert.data.Evidence;
import com.prelert.data.MetricPath;
import com.prelert.data.TimeSeriesConfig;
import com.prelert.data.TimeSeriesDataPoint;

/**
 * Interface defining the remote methods that can be called via RMI.
 * This interface is a remote version of TimeSeriesDAO and exactly matches 
 * that interface.
 * 
 * See TimeSeriesDAO for documentation.
 */
public interface RemoteTimeSeriesDAO extends java.rmi.Remote
{

	/**
	 * Is time series aggregation supported for the given data type, i.e.
	 * querying for time series points without specifying a value for every
	 * possible attribute?
	 * @param dataType The name of the data type.
	 * @return true if aggregation is supported; false if it's not.
	 */
	public boolean isAggregationSupported(String dataType) throws RemoteException;


	public List<String> getSourcesOrderByName(DataSourceType dataSourceType) throws RemoteException;

	public List<String> getSourcesOrderByCount(DataSourceType dataSourceType) throws RemoteException;
	
	public List<String> getAttributeNames(String dataType) throws RemoteException;

	public List<String>	getAttributeValues(String dataType, String attributeName, String source) throws RemoteException;

	public List<String> getMetrics(String dataType) throws RemoteException;

	public List<TimeSeriesDataPoint> getDataPoints(String dataType, String metric, 
			Date minTime, Date maxTime, String source, 
			List<Attribute> attributes, boolean includeFeatures) throws RemoteException;

	public List<TimeSeriesDataPoint> getDataPointsForDay(String dataType, String metric, 
			Date minTime, Date maxTime, String source, List<Attribute> attributes) throws RemoteException;

	public List<TimeSeriesDataPoint> getDataPointsForWeek(String dataType, String metric, 
			Date minTime, Date maxTime, String source, List<Attribute> attributes) throws RemoteException;

	public List<TimeSeriesDataPoint> getDataPointsForTimeSpan(String dataType, String metric, 
			Date minTime, Date maxTime, String source, 
			List<Attribute> attributes, boolean includeFeatures) throws RemoteException;
	
	public List<TimeSeriesDataPoint> getDataPointsRaw(
			int timeSeriesId, Date minTime, Date maxTime) throws RemoteException; 
	
	public Date getLatestTime(String dataType, String source) throws RemoteException;


	/**
	 * Returns the data model for a time series feature with the specified id.
	 * @param id the unique identifier for the time series feature.
	 * @return full data model for the evidence item with the specified id.
	 */
	public Evidence getFeature(int id) throws RemoteException;


	/**
	 * Returns the config of the time series corresponding to a specified
	 * feature id.
	 * @param id the unique identifier for the time series feature.
	 * @return config of the corresponding time series (or null if the
	 *         input id wasn't found).
	 */
	public TimeSeriesConfig getTimeSeriesFromFeature(int id) throws RemoteException;
	
	
	public MetricPath getMetricPathFromTimeSeriesId(int id) throws RemoteException;


	/**
	 * Returns the time series ID corresponding to a given external key.
	 * If no such time series ID is found <code>null</code> is returned.
	 *
	 * @param dataType The data type to which the external key belongs.
	 * @param externalKey The external key to be looked up.
	 * @return Time series ID or <code>null</code>
	 */
	public Integer getTimeSeriesIdFromExternalKey(String dataType,
												String externalKey) throws RemoteException;


	/**
	 * Returns the external key corresponding to a given time series ID.
	 * If the time series ID does not correspond to an external time series,
	 * <code>null</code> is returned.
	 *
	 * @param timeSeriesId The time series ID to look up.
	 * @return An external key or <code>null</code>
	 */
	public String getExternalKeyFromTimeSeriesId(int timeSeriesId) throws RemoteException;

}
