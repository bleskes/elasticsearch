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

package com.prelert.data;

/**
 * Class encapsulates external time series details.
 * 
 * TimeSeriesId and TimeSeriesType need to be know when 
 * a time series is converted to Xml and passed to the backend
 * C++ processes. 
 */
public class ExternalTimeSeriesDetails 
{
	private int m_TimeSeriesId;
	private int m_TimeSeriesTypeId;
	private String m_Type;
	private String m_Metric;
	private String m_ExternalKey;
	private boolean m_Active;
	

	/**
	 * Time series id in the database
	 * @return
	 */	
	public int getTimeSeriesId()
	{
		return m_TimeSeriesId;
	}
	
	public void setTimeSeriesId(int timeSeriesId)
	{
		m_TimeSeriesId = timeSeriesId;
	}
	
	
	/**
	 * Time series type id in the database
	 * @return
	 */
	public int getTimeSeriesTypeId()
	{
		return m_TimeSeriesTypeId;
	}
	
	public void setTimeSeriesTypeId(int timeSeriesTypeId)
	{
		m_TimeSeriesTypeId = timeSeriesTypeId;
	}
	
	
	/**
	 * Type of the time series data e.g CA-APM
	 * @return
	 */
	public String getType() 
	{
		return m_Type;
	}
	
	public void setType(String type) 
	{
		this.m_Type = type;
	}
			
	
	/**
	 * The time series metric
	 * @return
	 */
	public String getMetric()
	{
		return m_Metric;
	}
	
	public void setMetric(String metric)
	{
		m_Metric = metric;
	}
	
	
	/**
	 * The external key for this time series.
	 * This is unique for the each time series. 
	 * @return
	 */
	public String getExternalKey()
	{
		return m_ExternalKey;
	}
	
	public void setExternalKey(String externalKey)
	{
		m_ExternalKey = externalKey;
	}
	
	
	/**
	 * Is this time series marked as being actively in use.
	 * In terms of the CA API isActive means that the metric is 
	 * currently set to be under analysis. 
	 * 
	 * @return
	 */
	public boolean isActive()
	{
		return m_Active;
	}
	
	public void setActive(boolean active)
	{
		m_Active = active;
	}
	
	
	

	@Override
    public String toString()
    {
		StringBuilder strRep = new StringBuilder();
		strRep.append('{');
		strRep.append("TimeSeriesId = " + m_TimeSeriesId + "; ");
		strRep.append("TimeSeriesTypeId = " + m_TimeSeriesTypeId + "; ");
		strRep.append("Type = " + m_Type + "; ");
		strRep.append("Metric = " + m_Metric + "; ");
		strRep.append("ExternalKey = " + m_ExternalKey + "; ");
		strRep.append("IsActive = " + m_Active + "; ");
		strRep.append('}');
		
		return strRep.toString();
    }
}

