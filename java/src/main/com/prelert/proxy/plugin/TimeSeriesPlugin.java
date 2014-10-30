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
import java.util.List;

import com.prelert.data.Attribute;
import com.prelert.data.MetricPath;
import com.prelert.data.MetricTreeNode;
import com.prelert.data.TimeSeriesData;
import com.prelert.data.TimeSeriesDataPoint;
import com.prelert.proxy.inputmanager.querymonitor.QueryTookTooLongException;

/**
 * Interface defining the methods exposed by plugins to handle Time Series 
 * related functions.
 * @author dkyle
 *
 */
public interface TimeSeriesPlugin 
{	
	/**
	 * Returns a list of the names of the attributes associated with the given
	 * time series data type.
	 * @param dataType identifier for the type of evidence data.
	 * 		  Plugins which only support one data type may ignore this paramter
	 * @return the list of attribute names associated with the data type, 
	 * 			ordered by name.
	 */
	public List<String> getAttributeNames(String dataType);
	
	/**
	 * Returns a list of the distinct values for the attribute with the given name
	 * for the specified data type e.g. the values of the 'username' attribute
	 * for p2psmon_users data.
	 * @param dataType identifier for the type of evidence data
	 *        Plugins which only support one data type may ignore this parameter
	 * @param attributeName name of attribute for which to return the values.
	 * @param source optional source name.
	 * @return a list of the distinct values for the attribute.
	 */
	public List<String>	getAttributeValues(String dataType, String attributeName, String source);


	/**
	 * Returns a list of all the <code>Attribute</code>s associated with a
	 * specific time series, identified by its external key.
	 * @param externalKey The external key of the time series.
	 * @return List of <code>Attribute</code>s (name, value pairs)
	 */
	public List<Attribute> getAttributesForKey(String externalKey);


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
	 * Returns the list of metrics associated with the time series.
	 * @param dataType identifier for the type of evidence data.
	 *        Plugins which only support one data type may ignore this parameter
	 * @return the list of metrics ordered by name.
	 */
	public List<String> getMetrics(String dataType);
	
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
	 * Returns the time series data points for the given datatype, metric
	 * source and attributes.
	 * 
	 * @param dataType time series data type 
	 *        Plugins which only support one data type may ignore this parameter
	 * @param metric time series metric
	 * @param source time series source
	 * @param ttributes which the plugin uses to determine the exact time series.
	 * @param startTime - start time of data to include.
	 * @param endTime - end time of data to include.
	 * @param intervalSecs The frequency, in seconds, at which data points 
	 * should be returned. 
	 * @return list of time series data points.
	 */
	public List<TimeSeriesDataPoint> getDataPointsForTimeSpan(String dataType, String metric, 
											String source,
											List<Attribute> attributes,
											Date startTime, Date endTime, 
											int intervalSecs);

	/**
	 * Returns all the Time Series points for all the Time Series available for this plugin
	 * in the given time period.  The external key in each returned <code>TimeSeriesData</code>
	 * maps to the time series the point came from.
	 * @param startTime - start time of data to include.
	 * @param endTime - end time of data to include.
	 * @param intervalSecs
	 * @return Collection of <code>TimeSeriesData</code> where the <code>TimeSeriesConfig</code>
	 * has its externalKey property set.
	 * @throws QueryTookTooLongException
	 */
	public Collection<TimeSeriesData> getAllDataPointsForTimeSpan(Date startTime, Date endTime, 
															int intervalSecs)
	throws QueryTookTooLongException;



	/**
	 * Utility class which groups an external key with a peak value.
	 * @see getPeakValueForTimeSpan
	 */
	public class ExternalKeyPeakValuePair implements Comparable<ExternalKeyPeakValuePair>
	{
		final private String m_ExternalKey;
		final private double m_PeakValue;
		
		public ExternalKeyPeakValuePair(String externalKey, double peakValue)
		{
			m_ExternalKey = externalKey;
			m_PeakValue = peakValue;
		}
		
		public String getExternalKey()
		{
			return m_ExternalKey;
		}
		
		public double getPeakValue()
		{
			return m_PeakValue;
		}


		/**
		 * Comparison to another object of this class ONLY considers external
		 * key, NOT the peak value.
		 * @param other Object to compare to.
		 * @return -1 if this object is less than <code>other</code>, 0 if
		 *         it's the same or 1 if it's greater.
		 */
		@Override
		public int compareTo(ExternalKeyPeakValuePair other) 
		{
			return m_ExternalKey.compareTo(other.getExternalKey());
		}


		@Override 
		public String toString()
		{
			return "{" + m_ExternalKey + ", " + m_PeakValue + "}";
		}
		
	}


	/**
	 * Calculates and returns the peak value of the time series points for all the 
	 * given keys in the time period between startTime and endTime. As some plugins
	 * may be able to optimise this function for multiple keys a list of 
	 * external keys is taken and a list of <code>ExternalKeyPeakValuePair</code> 
	 * is returned. 
	 * 
	 * @param externalKeys list of key strings which help the plugin locate the 
	 * 	      required data.
	 * @param startTime
	 * @param endTime
	 * @param intervalSecs The interval between time series points.
	 * @return a list which groups the <code>externalKey</code> parameter 
	 * 			with the peak value. Each peak value is either the peak value 
	 * 			or 0 if it can be calculated.
	 */
	public List<ExternalKeyPeakValuePair> getPeakValueForTimeSpan(List<String> externalKeys, 
											Date startTime, Date endTime,
											int intervalSecs);

	/**
	 * Returns the MetricPath for given external key. The plugin
	 * knows how to parse the external key and produce the
	 * metric path for it.
	 * 
	 * @param datatype
	 * @param externalKey
	 * @return
	 */
	public MetricPath metricPathFromExternalKey(String datatype, String externalKey);


	/**
	 * For the given list of partially populated <code>MetricTreeNode</code>s which have
	 * no members set apart from the <code>externalKey</code> this function
	 * returns a new list of <code>MetricTreeNode</code>s containing 
	 * the name and prefix for each level of that longest metric path, in order 
	 * of their position in the metric path.
	 * 
	 * @param externalKeyNodes List of partially populated
	 *                         <code>MetricTreeNode</code> objects containing
	 *                         external keys.
	 * @return A list of partially populated <code>MetricTreeNode</code> objects
	 *         containing the name and prefix of each constituent of the metric
	 *         path.
	 */
	public List<MetricTreeNode> metricPathNodesFromExternalKeys(List<MetricTreeNode> externalKeyNodes);

}
