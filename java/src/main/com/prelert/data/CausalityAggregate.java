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

package com.prelert.data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Class representing aggregated causality data where a set of causality objects
 * have been aggregated by a particular attribute e.g. all causality data in
 * an incident where the service attribute = 'PRISM'. Depending on the attribute by
 * which the causality objects have been aggregated, the object may represent both
 * notifications and time series features.
 * 
 * @author Pete Harverson
 */
public class CausalityAggregate implements Serializable
{
	private static final long serialVersionUID = -9042339779447397685L;

	private String 		m_AggregateValue;
	private boolean		m_AggregateValueIsNull = true;
	private Date 		m_StartTime;
	private Date 		m_EndTime;
	private int 		m_NotificationCount;
	private int 		m_FeatureCount;
	private int 		m_SourceCount;
	private List<String> m_SourceNames;
	private int 		m_TopEvidenceId = -1;
	private CausalityData	m_TopData;


	/**
	 * Returns the value of the attribute by which the causality data have been
	 * aggregated.
	 * @return the value of the aggregated attribute, or <code>null</code> if 
	 * 	the data for several aggregated values have been aggregated together,
	 * 	for example for an 'Others' result.
	 */
	public String getAggregateValue()
	{
		return m_AggregateValue;
	}


	/**
	 * Sets the value of the attribute by which the causality data have been
	 * aggregated.
	 * @param aggregateValue the value of the aggregated attribute.
	 */
	public void setAggregateValue(String aggregateValue)
	{
		m_AggregateValue = aggregateValue;
		m_AggregateValueIsNull = (aggregateValue == null);
	}
	
	
	/**
	 * Returns a flag indicating whether the value of the attribute by which the
	 * causality data was aggregated is <code>null</code> in the source notifications
	 * or time series features.
	 * @return <code>true</code> if the value of the aggregated attribute is <code>null</code> 
	 * in the source notifications or time series features. Will return <code>false</code>
	 * for cases where data from several non-<code>null</code> aggregated 
	 * values have been aggregated together (for example for an 'Others' result).
	 */
	public boolean isAggregateValueNull()
	{
		return m_AggregateValueIsNull;
	}
	
	
	/**
	 * Sets the flag indicating whether the value of the attribute by which the
	 * causality data was aggregated is <code>null</code> in the source notifications
	 * or time series features.
	 * @param isNull <code>true</code> if the value of the aggregated attribute 
	 * is <code>null</code> in the source notifications or time series features. 
	 * Set to <code>false</code> for cases where data from several non-<code>null</code> 
	 * aggregated values have been aggregated together (for example for an 'Others' result).
	 */
	public void setAggregateValueNull(boolean isNull)
	{
		m_AggregateValueIsNull = isNull;
	}


	/**
	 * Returns the start time of the aggregated causality object i.e. the time of
	 * the earliest notification or time series feature.
	 * @return the start time i.e. the time of the earliest notification or time 
	 * 		series feature.
	 */
	public Date getStartTime()
	{
		return m_StartTime;
	}


	/**
	 * Sets the start time of the aggregated causality object i.e. the time of
	 * the earliest notification or time series feature.
	 * @param startTime the start time i.e. the time of the earliest notification 
	 * 		or time series feature.
	 */
	public void setStartTime(Date startTime)
	{
		m_StartTime = startTime;
	}


	/**
	 * Returns the end time of the aggregated causality object i.e. the time of
	 * the latest notification or time series feature.
	 * @return the end time i.e. the time of the latest notification or time 
	 * 		series feature. If there is only one item in the aggregated object,
	 * 		this is the time of that particular notification or feature.
	 */
	public Date getEndTime()
	{
		return m_EndTime;
	}


	/**
	 * Sets the end time of the aggregated causality object i.e. the time of
	 * the latest notification or time series feature.
	 * @param endTime the end time i.e. the time of the latest notification or 
	 * 		time series feature. If there is only one item in the aggregated object,
	 * 		this is the time of that particular notification or feature.
	 */
	public void setEndTime(Date endTime)
	{
		m_EndTime = endTime;
	}


	/**
	 * Returns the count of notifications in the aggregated causality object.
	 * @return the total notification count.
	 */
	public int getNotificationCount()
	{
		return m_NotificationCount;
	}


	/**
	 * Sets the count of notifications in the aggregated causality object.
	 * @param count the total notification count.
	 */
	public void setNotificationCount(int count)
	{
		m_NotificationCount = count;
	}


	/**
	 * Returns the count of time series features in the aggregated causality object.
	 * @return the total feature count.
	 */
	public int getFeatureCount()
	{
		return m_FeatureCount;
	}


	/**
	 * Sets the count of time series features in the aggregated causality object.
	 * @param count the total feature count.
	 */
	public void setFeatureCount(int count)
	{
		m_FeatureCount = count;
	}


	/**
	 * Returns the number of different sources (servers) on which notifications or
	 * time series features in the aggregated set have occurred.
	 * @return the number of sources.
	 */
	public int getSourceCount()
	{
		return m_SourceCount;
	}


	/**
	 * Sets the number of different sources (servers) on which notifications or
	 * time series features in the aggregated set have occurred.
	 * @param count the number of sources.
	 */
	public void setSourceCount(int count)
	{
		m_SourceCount = count;
	}


	/**
	 * Returns the complete list of source (server) names on which notifications or
	 * time series features in the aggregated set have occurred.
	 * @return the list of source names.
	 */
	public List<String> getSourceNames()
	{
		return m_SourceNames;
	}


	/**
	 * Sets the complete list of source (server) names on which notifications or
	 * time series features in the aggregated set have occurred.
	 * @param sourceNames the list of source names.
	 */
	public void setSourceNames(List<String> sourceNames)
	{
		m_SourceNames = sourceNames;
	}
	
	
	/**
	 * Returns the id of the 'top' (often measured by significance) notification
	 * or time series features in the aggregated set.
	 * @return the id of the 'top' notification or time series feature, 
	 * 	or -1 if no evidence id has been set.
	 */
	public int getTopEvidenceId()
	{
		return m_TopEvidenceId;
	}


	/**
	 * Sets the id of the 'top' (often measured by significance) notification
	 * or time series features in the aggregated set.
	 * @param evidenceId the id of the 'top' notification or time series feature.
	 */
	public void setTopEvidenceId(int evidenceId)
	{
		m_TopEvidenceId = evidenceId;
	}
	
	
	/**
	 * Returns the 'top' (often measured by significance) item of causality data
	 * in the aggregated set.
	 * @return CausalityData encapsulating the 'top' notification(s) or time series feature.
	 * 	Note that all the fields in the object may not have been populated.
	 */
	public CausalityData getTopCausalityData()
	{
		return m_TopData;
	}
	
	
	/**
	 * Sets the 'top' (often measured by significance) item of causality data
	 * in the aggregated set.
	 * @param causalityData CausalityData encapsulating the 'top' notification(s)
	 * 	or time series feature.
	 */
	public void setTopCausalityData(CausalityData causalityData)
	{
		m_TopData = causalityData;
	}

	
    @Override
    public boolean equals(Object obj)
    {
		if (obj == null)
		{
			return false;
		}
		
		if (this == obj)
		{
			return true;
		}
		
		if ((obj instanceof CausalityAggregate) == false)
		{
			return false;
		}
		
		CausalityAggregate other = (CausalityAggregate)obj;

		// Compare each of the fields for equality.
		// Compare top id, but no need to compare top CausalityData at this stage.
		boolean result = m_AggregateValue.equals(other.getAggregateValue());
		result = result && (m_TopEvidenceId == other.getTopEvidenceId());
		result = result && (m_StartTime.equals(other.getStartTime()));
		result = result && (m_EndTime.equals(other.getEndTime()));
		result = result && (m_NotificationCount == other.getNotificationCount());
		result = result && (m_FeatureCount == other.getFeatureCount());
		result = result && (m_SourceCount == other.getSourceCount());
		
		boolean bothNull = (m_SourceNames == null && other.getSourceNames() == null);
		if (!bothNull)
		{
			boolean bothNonNull = (m_SourceNames != null && other.getSourceNames() != null);
			result = result && bothNonNull && (m_SourceNames.equals(other.getSourceNames()));
		}

		return result;
    }


	@Override
    public String toString()
    {
    	StringBuilder strRep = new StringBuilder('{');
    	
    	strRep.append("value=");
    	strRep.append(m_AggregateValue);
    	strRep.append(", evidence id=");
    	strRep.append(m_TopEvidenceId);
    	strRep.append(", start=");
    	strRep.append(m_StartTime);
    	strRep.append(", end=");
    	strRep.append(m_EndTime);
    	strRep.append(", notification count=");
    	strRep.append(m_NotificationCount);
    	strRep.append(", feature count=");
    	strRep.append(m_FeatureCount);
    	strRep.append(", source count=");
    	strRep.append(m_SourceCount);
    	strRep.append(", sources=");
    	strRep.append(m_SourceNames);
	    
	    strRep.append('}');
		
		return strRep.toString();
    }
}
