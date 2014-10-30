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

import java.util.Date;
import java.util.List;

import com.prelert.data.Attribute;
import com.prelert.data.TimeSeriesDataPoint;

/**
 * Interface for the ExternalPlugin.
 * 
 * External plugins implement all the methods required to serve
 * the GUI and pull time series points. All points and other 
 * data such as data sources is stored externally but can be 
 * accessed through an <code>ExternalPlugin</code>.
 */
public interface ExternalPlugin extends ExternalPointsPlugin, DataSourcePlugin, TimeSeriesPlugin
{
	/**
	 * Returns the time series data points for the given datatype, metric
	 * source and attributes.
	 * 
	 * @param datatype time series data type 
	 *        Plugins which only support one data type may ignore this parameter
	 * @param metric time series metric
	 * @param source time series source
	 * @param ttributes which the plugin uses to determine the exact time series.
	 * @param startTime start time of data to include.
	 * @param endTime end time of data to include.
	 * @param intervalSecs The frequency, in seconds, at which data points 
	 * should be returned. 
	 * @return list of time series data points.
	 */
	public List<TimeSeriesDataPoint> getDataPointsForTimeSpan(String datatype, String metric, 
											String source,
											List<Attribute> attributes,
											Date startTime, Date endTime, 
											int intervalSecs);
}
