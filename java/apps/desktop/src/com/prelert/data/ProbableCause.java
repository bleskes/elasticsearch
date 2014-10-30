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

import java.io.Serializable;
import java.util.Date;

/**
 * Class encapsulating the data for a probable cause.
 * 
 * @author Pete Harverson
 */

@SuppressWarnings("serial")
public class ProbableCause implements Serializable
{
	private DataSourceType 	m_DataSourceType;
	private Date 			m_Time;
	private String 			m_Description;
	private String 			m_Source;
	private String 			m_AttributeName;
	private String 			m_AttributeValue;
	private String			m_AttributeLabel;
	private int 			m_Significance;
	private String 			m_Metric;


	/**
	 * Creates a new blank probable cause.
	 */
	public ProbableCause()
	{

	}


	/**
	 * Returns the data source type of this probable cause e.g. p2ps logs or
	 * UDP error data.
	 * @return the data source type.
	 */
	public DataSourceType getDataSourceType()
	{
		return m_DataSourceType;
	}


	/**
	 * Sets the data source type of this probable cause e.g. p2ps logs or
	 * UDP error data.
	 * @param dataSourceType the data source type.
	 */
	public void setDataSourceType(DataSourceType dataSourceType)
	{
		m_DataSourceType = dataSourceType;
	}


	/**
	 * Returns the time of this probable cause.
	 * @return the start time.
	 */
	public Date getTime()
	{
		return m_Time;
	}


	/**
	 * Sets the value of the time property for this probable cause.
	 * @param time the time.
	 */
	public void setTime(Date time)
	{
		m_Time = time;
	}


	/**
	 * Returns the description of this probable cause.
	 * @return the description.
	 */
	public String getDescription()
	{
		return m_Description;
	}


	/**
	 * Sets the description for this probable cause.
	 * @param description  the description.
	 */
	public void setDescription(String description)
	{
		m_Description = description;
	}


	/**
	 * Returns the source (server) of this probable cause.
	 * @return the source (server).
	 */
	public String getSource()
	{
		return m_Source;
	}


	/**
	 * Sets the source (server) of this probable cause.
	 * @param source the source (server).
	 */
	public void setSource(String source)
	{
		m_Source = source;
	}


	/**
	 * Returns the name of the additional attribute, if any, for this probable
	 * cause. For example, a p2psmon user usage probable cause may have a 'username'
	 * or 'appid' attribute.
	 * @return the attribute name, or <code>null</code> if there is no additional
	 * attribute for this probable cause.
	 */
	public String getAttributeName()
	{
		return m_AttributeName;
	}


	/**
	 * Sets the name of an additional attribute for this probable cause. 
	 * For example, a p2psmon user usage probable cause may have a 'username'
	 * or 'appid' attribute.
	 * @param attributeName  the additional attribute name.
	 */
	public void setAttributeName(String attributeName)
	{
		m_AttributeName = attributeName;
	}


	/**
	 * Returns the value of the additional attribute, if any, for this probable
	 * cause. For example, a p2psmon user usage probable cause may have a 'username'
	 * or 'appid' attribute whose value this method returns.
	 * @return the value of the attribute for this probable cause.
	 */
	public String getAttributeValue()
	{
		return m_AttributeValue;
	}


	/**
	 * Sets the value of the additional attribute, if any, for this probable
	 * cause. For example, a p2psmon user usage probable cause may have a 'username'
	 * or 'appid' attribute whose value this method sets.
	 * @param attributeValue the value of the attribute for this probable cause.
	 */
	public void setAttributeValue(String attributeValue)
	{
		m_AttributeValue = attributeValue;
	}
	
	
	/**
	 * Returns the value of the attribute label property, if any, for this probable
	 * cause. For example, a p2psmon user usage probable cause may have both 'username'
	 * and 'appid' attributes whose values are returned by this method in a format
	 * such as 'app_id=418, username=rjones'.
	 * @return the value of the attribute for this probable cause.
	 */
	public String getAttributeLabel()
	{
		return m_AttributeLabel;
	}


	/**
	 * Sets the value of the attribute label property for this probable
	 * cause. For example, a p2psmon user usage probable cause with both 'username'
	 * and 'appid' attributes would be passed a label in a format such as 
	 * 'app_id=418, username=rjones'
	 * @param attributeLabel the attribute label for this probable cause.
	 */
	public void setAttributeLabel(String attributeLabel)
	{
		m_AttributeLabel = attributeLabel;
	}

	
	/**
	 * Returns the significance of this probable cause, indicating a percentage
	 * between 1 and 100.
	 * @return the significance - a value between 1 and 100, or 0 if no significance
	 * 		property has been set. A value of -1 indicates that this ProbableCause 
	 * 		object refers to the event that has occurred.
	 */
	public int getSignificance()
	{
		return m_Significance;
	}


	/**
	 * Sets the significance of this probable cause, indicating a percentage
	 * between 1 and 100.
	 * @param significance the significance - a value between 1 and 100.
	 * 		A value of -1 should be used to indicate that this ProbableCause 
	 * 		object refers to the event that has occurred.
	 */
	public void setSignificance(int significance)
	{
		m_Significance = significance;
	}
	
	
	/**
	 * For time series type probable causes, returns the time series metric.
	 * @return the time series metric.
	 */
	public String getMetric()
	{
		return m_Metric;
	}
	
	
	/**
	 * For time series type probable causes, sets the time series metric.
	 * @param metric the time series metric.
	 */
	public void setMetric(String metric)
	{
		m_Metric = metric;
	}
	
	
	/**
	 * Returns a summary of this probable cause.
	 * @return String representation of this probable cause.
	 */
	public String toString()
	{
		StringBuilder strRep = new StringBuilder('{');
		strRep.append("dataSourceType=");
		strRep.append(m_DataSourceType);
		strRep.append(", time=");
		strRep.append(m_Time);
		strRep.append(", description=");
		strRep.append(m_Description);
		strRep.append(", source=");
		strRep.append(m_Source);
		
		if (m_AttributeLabel != null)
		{
			strRep.append(", attributes=");
			strRep.append(m_AttributeLabel);
		}
		
		strRep.append('}');
		
		return strRep.toString();
	}

}
