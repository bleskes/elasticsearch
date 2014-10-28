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

import com.prelert.data.DataSource;
import com.prelert.data.DataSourceType;
import com.prelert.data.TimeSeriesDataPoint;
import com.prelert.proxy.plugin.TimeSeriesPlugin.ExternalKeyPeakValuePair;

/**
 * Interface for the ExternalPointsPlugin.
 *  
 * This interface defines methods to retrieve data using an external
 * key so that time series points can be stored externally to Prelert
 * but retrieved to support the GUI using methods of this interface.
 * 
 * Only points are stored externally information on data sources
 * name metric paths are stored in Prelert.
 */
public interface ExternalPointsPlugin extends InternalPlugin
{
	/**
	 * Returns the time series data points for the externalKey. 
	 * The implementing class knows how to parse and use the 
	 * externalKey parameter to retrieve data.
	 * 
	 * @param externalKey - a key string which helps the plugin locate the required data.
	 * @param startTime - start time of data to include.
	 * @param endTime - end time of data to include.
	 * @param intervalSecs - The frequency, in seconds, at which data points 
	 * should be returned. 
	 * @return list of time series data points.
	 */
	public List<TimeSeriesDataPoint> getDataPointsForTimeSpan(String externalKey,
							Date startTime, Date endTime, 
								int intervalSecs);
	
	
	/**
	 * Calculates and returns the peak value of the time series points for all the 
	 * given keys in the time period between startTime and endTime. As some plugins
	 * may be able to optimise this function for multiple keys a list of 
	 * external keys is taken and a list of <code>ExternalKeyPeakValuePair</code> 
	 * is returned. 
	 * 
	 * @param externalKeys list of key strings which help the plugin locate the 
	 * 	      required data.
	 * @param startTime - start time of data to include.
	 * @param endTime - end time of data to include.
	 * @param intervalSecs The interval between time series points.
	 * @return a list which groups the <code>externalKey</code> parameter 
	 * 			with the peak value. Each peak value is either the peak value 
	 * 			or 0 if it can be calculated.
	 */
	public List<ExternalKeyPeakValuePair> getPeakValueForTimeSpan(List<String> externalKeys, 
									Date startTime, Date endTime,
									int intervalSecs);


	/**
	 * Get the count of data items for this data type (as displayed on the GUI's
	 * "Analysed Data" screen), or -1 if this information is not available.
	 * @param type The data type to get the item count for.
	 * @return The count of data items for this data type.  If this information
	 *         is not available returns -1.
	 */
	public int getDataTypeItemCount(DataSourceType type);
	
	
	/**
	 * Returns the number of points collected from a particular source for
	 * a particular data type (as displayed in GUI data sources in the
	 * explorer view) or -1 if unknown.
	 * 
	 * @param source The datatype and source.
	 * @return The count of data items for this source or -1 if the count is not
	 * 		available.
	 */
	public int getDataSourceItemCount(DataSource source);
	
	
	/**
	 * Does this plugin support time series aggregation for a given data type,
	 * i.e. querying for time series points without specifying a value for every
	 * possible attribute?
	 * @param dataType The name of the data type.
	 * @return true if aggregation is supported; false if it's not.
	 */
	public boolean isAggregationSupported(String dataType);
		

	/**
	 * Returns the date/time of the latest record in the database for the
	 * time series.
	 * @param dataType identifier for the type of evidence data.
	 * 	      Plugins which only support one data type may ignore this parameter
	 * @param source optional source name.
	 * @return date/time of latest record.
	 */
	public Date getLatestTime(String dataType, String source);
	
	
	/**
	 * Returns the string used to separate elements of
	 * the metric path.
	 * @return
	 */
	public String getMetricPathDelimiter();
	
	/**
	 * Retruns the string used to prefix the metric
	 * part of the metric path.
	 * @return
	 */
	public String getMetricPathMetricPrefix();
	
	
	/**
	 * Returns the prefix used before the source 
	 * element of the metric path.
	 * @return
	 */
	public String getMetricPathSourcePrefix();
}
