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

package com.prelert.data;

import java.util.List;


/**
 * Class encapsulating all the data of a time series i.e. its configuration 
 * properties together with the data points.
 * @author Pete Harverson
 */
public class TimeSeriesData
{
	private TimeSeriesConfig			m_Config;
	private List<TimeSeriesDataPoint>	m_DataPoints;
	
	
	/**
	 * Creates a new, empty TimeSeries data object.
	 */
	public TimeSeriesData()
	{
		
	}
	
	
	/**
	 * Creates a new object for time series data with the specified configuration
	 * and data points.
	 * @param config TimeSeriesConfig holding the configuration properties of the
	 * 			time series e.g. data type, metric, source.
	 * @param dataPoints the data points.
	 */
	public TimeSeriesData(TimeSeriesConfig config, List<TimeSeriesDataPoint> dataPoints)
	{
		m_Config = config;
		m_DataPoints = dataPoints;
	}


	/**
	 * Returns the configuration of this time series.
     * @return TimeSeriesConfig object holding the configuration properties of 
	 * 			the time series e.g. data type, metric, source.
     */
    public TimeSeriesConfig getConfig()
    {
    	return m_Config;
    }


	/**
	 * Sets the configuration of this time series.
     * @param config TimeSeriesConfig object holding the configuration 
	 * 			properties of  the time series e.g. data type, metric, source.
     */
    public void setConfig(TimeSeriesConfig config)
    {
    	m_Config = config;
    }


	/**
	 * Returns the list of data points in this time series.
     * @return the list of data points (time/value pairs).
     */
    public List<TimeSeriesDataPoint> getDataPoints()
    {
    	return m_DataPoints;
    }


	/**
	 * Sets the list of data points in this time series.
     * @param dataPoints the list of data points (time/value pairs).
     */
    public void setDataPoints(List<TimeSeriesDataPoint> dataPoints)
    {
    	m_DataPoints = dataPoints;
    }
	
	
	
}
