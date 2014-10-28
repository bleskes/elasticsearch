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

package com.prelert.data.gxt;

import java.io.Serializable;
import java.util.Date;

import com.extjs.gxt.ui.client.data.BaseModelData;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;


/**
 * Extension of the GXT BaseModelData class for probable cause data.
 * @author Pete Harverson
 */
public class ProbableCauseModel extends BaseModelData implements Serializable
{
	/**
	 * Creates a new ProbableCauseModel instance.
	 */
	public ProbableCauseModel()
	{

	}


	/**
	 * Sets the data source type of this probable cause e.g. p2ps logs or
	 * UDP error data.
	 * @param dataSourceType the data source type.
	 */
	public void setDataSourceType(DataSourceType dataSourceType)
	{
		set("dataSourceName", dataSourceType.getName());
		
		// Jan 2010: Store the category as a String as otherwise errors may occur
		// when this object is transported via GWT RPC.
		set("dataSourceCategory", dataSourceType.getDataCategory().toString());
	}
	
	
	/**
	 * Returns the category of the data source of this probable cause
	 * e.g. notification or time series.
	 * @return the category of this probable cause's data source.
	 */
	public DataSourceCategory getDataSourceCategory()
	{
		String category = get("dataSourceCategory");
		return Enum.valueOf(DataSourceCategory.class, category);
	}
	
	
	/**
	 * Returns the name of the data source of this probable cause e.g. p2ps logs or
	 * UDP error data.
	 * @return the data source name.
	 */
	public String getDataSourceName()
	{
		return get("dataSourceName");
	}


	/**
	 * Returns the time of this probable cause.
	 * @return the time.
	 */
	public Date getTime()
	{
		return get("time");
	}


	/**
	 * Sets the value of the start time property for this probable cause.
	 * @param startTime the start time.
	 */
	public void setTime(Date time)
	{
		set("time", time);
	}


	/**
	 * Returns the description of this probable cause.
	 * @return the description.
	 */
	public String getDescription()
	{
		return get("description");
	}


	/**
	 * Sets the description for this probable cause.
	 * @param description  the description.
	 */
	public void setDescription(String description)
	{
		set("description", description);
	}


	/**
	 * Returns the source (server) of this probable cause.
	 * @return the source (server).
	 */
	public String getSource()
	{
		return get("source");
	}


	/**
	 * Sets the source (server) of this probable cause.
	 * @param source the source (server).
	 */
	public void setSource(String source)
	{
		set("source", source);
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
		return get("attributeName");
	}


	/**
	 * Sets the name of an additional attribute for this probable cause. 
	 * For example, a p2psmon user usage probable cause may have a 'username'
	 * or 'appid' attribute.
	 * @param attributeName  the additional attribute name.
	 */
	public void setAttributeName(String attributeName)
	{
		set("attributeName", attributeName);
	}


	/**
	 * Returns the value of the additional attribute, if any, for this probable
	 * cause. For example, a p2psmon user usage probable cause may have a 'username'
	 * or 'appid' attribute whose value this method returns.
	 * @return the value of the attribute for this probable cause.
	 */
	public String getAttributeValue()
	{
		return get("attributeValue");
	}


	/**
	 * Sets the value of the additional attribute, if any, for this probable
	 * cause. For example, a p2psmon user usage probable cause may have a 'username'
	 * or 'appid' attribute whose value this method sets.
	 * @param attributeValue the value of the attribute for this probable cause.
	 */
	public void setAttributeValue(String attributeValue)
	{
		set("attributeValue", attributeValue);
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
		return get("attributeLabel");
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
		set("attributeLabel", attributeLabel);
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
    	int significance = 0;
    	Integer significanceInt = (Integer)(get("significance"));
    	if (significanceInt != null)
    	{
    		significance = significanceInt.intValue();
    	}
    	return significance;
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
		set("significance", significance);
	}
	
	
	/**
	 * For time series type probable causes, returns the time series metric.
	 * @return the time series metric.
	 */
	public String getMetric()
	{
		return get("metric");
	}
	
	
	/**
	 * For time series type probable causes, sets the time series metric.
	 * @param metric the time series metric.
	 */
	public void setMetric(String metric)
	{
		set("metric", metric);
	}
	
	
	/**
	 * Returns a summary of this probable cause.
	 * @return String representation of this probable cause.
	 */
	public String toString()
	{
		return getProperties().toString();
	}
}
