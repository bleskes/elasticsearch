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


package com.prelert.proxy.plugin;

import java.util.Collection;
import java.util.Date;

import com.prelert.data.TimeSeriesData;
import com.prelert.proxy.inputmanager.querymonitor.QueryTookTooLongException;

/**
 * Interface for Internal Plugins.
 * Internal Plugins only have methods for pulling time series data 
 * which is then stored by Prelert. These plugins cannot re-retrieve
 * service a GUI their only purpose is to pull data. 
 */
public interface InternalPlugin 
{
	/**
	 * Returns all the Time Series points for all the Time Series available 
	 * for this plugin in the given time period. 
	 * 
	 * An ErrorGettingDataPointsException is thrown if no data is returned and an 
	 * error occurred.
	 * 
	 * If the plugin has an {@link com.prelert.proxy.inputmanager.querymonitor.QueryMonitorPolicy}
	 * to monitor the duration of the query then a QueryTookTooLongException might be
	 * thrown. 
	 * 
	 * @param minTime
	 * @param maxTime
	 * @param intervalSecs
	 * @return Collection of <code>TimeSeriesData</code> 
	 * @throws QueryTookTooLongException
	 * @throws ErrorGettingDataPointsException 
	 */
	public Collection<TimeSeriesData> getAllDataPointsForTimeSpan(Date minTime, Date maxTime, 
															int intervalSecs)
	throws QueryTookTooLongException, ErrorGettingDataPointsException;
	
	
	/**
	 * Get the default interval in seconds between points for
	 * all time series returned by this plugin. 
	 * 
	 * @return the default interval in seconds between points in the 
	 * 			time series for all types and metrics
	 */
	public int getUsualPointIntervalSecs();
	
	
	/**
	 * Set the default interval in seconds between points for
	 * all the time series types and metrics supported by the 
	 * plugin. 
	 * @param value
	 */
	public void setUsualPointIntervalSecs(int value);
}
