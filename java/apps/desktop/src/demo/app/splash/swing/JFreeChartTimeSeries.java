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

package demo.app.splash.swing;

import org.jfree.data.time.TimeSeries;

import demo.app.data.TimeSeriesConfig;

public class JFreeChartTimeSeries extends TimeSeries
{
	private TimeSeriesConfig m_Config;
	
	public JFreeChartTimeSeries(TimeSeriesConfig config)
	{
		super(createTimeSeriesKey(config));
		
		m_Config = config;
	}
	
	
	/**
	 * Returns the configuration for the time series.
	 * @return the TimeSeriesConfig encapsulating the properties of the time series.
	 */
	public TimeSeriesConfig getTimeSeriesConfig()
	{
		return m_Config;
	}
	
	
	/**
     * Creates a key for the specified TimeSeriesConfig to distinguish it from
     * other time series in a chart.
     * @param dataSeries config for the time series for which to generate a key.
     * @return a key for the time series.
     */
    protected static String createTimeSeriesKey(TimeSeriesConfig config)
    {
    	String dataType = config.getDataType();
    	String metric = new String(config.getMetric());
    	String source = config.getSource();
    	String attributeName = config.getAttributeName();
    	String attributeValue = config.getAttributeValue();
    	
    	StringBuilder key = new StringBuilder(dataType);
    	key.append(',');
    	key.append(metric);
    	key.append(',');
    	
    	if (source != null)
    	{
    		key.append(source);
    	}
    	else
    	{
    		key.append("all sources");
    	}
    	
    	if ( (attributeName != null) && (attributeValue != null) )
    	{
    		key.append(',');
    		key.append(attributeName);
    		key.append('=');
    		key.append(attributeValue);
    	}
    	
    	return key.toString();
    }
}
