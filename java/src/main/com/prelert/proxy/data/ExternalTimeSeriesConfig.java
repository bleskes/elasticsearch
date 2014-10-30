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

package com.prelert.proxy.data;

import com.prelert.data.DataSourceCategory;
import com.prelert.data.TimeSeriesInterpretation;

/**
 * This class encapsulates an External Time Series. It contains details
 * of the time series's type, metric and also the name of the external
 * plugin used to retrieve time series data and the external key which
 * is required by the plugin to identify the time series.   
 */
public class ExternalTimeSeriesConfig 
{
	private String m_Type;
	private DataSourceCategory m_Category;
	private String m_Metric;
	private int m_UsualInterval;
	private TimeSeriesInterpretation m_Interpretation;
	private String m_ExternalPlugin;
	private String m_ExternalKey;
	
	public ExternalTimeSeriesConfig()
	{
		m_Interpretation = TimeSeriesInterpretation.ABSOLUTE;
	}

	
	public String getType() 
	{
		return m_Type;
	}
	
	public void setType(String type) 
	{
		this.m_Type = type;
	}
	
	public DataSourceCategory getCategory() 
	{
		return m_Category;
	}
	
	public void setCategory(DataSourceCategory category) 
	{
		this.m_Category = category;
	}
		
	public String getMetric()
	{
		return m_Metric;
	}
	
	public void setMetric(String metric)
	{
		m_Metric = metric;
	}
	
	public int getUsualInterval()
	{
		return m_UsualInterval;
	}
	
	public void setUsualInterval(int usualInterval)
	{
		m_UsualInterval = usualInterval;
	}
	
	public TimeSeriesInterpretation getInterpretation()
	{
		return m_Interpretation;
	}
	
	public void setInterpretation(TimeSeriesInterpretation interpretation)
	{
		m_Interpretation = interpretation;
	}
	
	public String getExternalPlugin() 
	{
		return m_ExternalPlugin;
	}
	
	public void setExternalPlugin(String externalPlugin) 
	{
		this.m_ExternalPlugin = externalPlugin;
	}
	
	/**
	 * The string for the external key, may be null.
	 * @return Value could be null.
	 */
	public String getExternalKey()
	{
		return m_ExternalKey;
	}
	
	public void setExternalKey(String externalKey)
	{
		m_ExternalKey = externalKey;
	}
	
	

	@Override
    public String toString()
    {
		StringBuilder strRep = new StringBuilder();
		strRep.append('{');
		strRep.append("Type = " + m_Type + "; ");
		strRep.append("Category = " + m_Category + "; ");
		strRep.append("Metric = " + m_Metric + "; ");
		strRep.append("ExternalPlugin = " + m_ExternalPlugin + "; ");
		strRep.append("ExternalKey = " + m_ExternalKey + "; ");
		strRep.append('}');
		
		return strRep.toString();
    }
}
