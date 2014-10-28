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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.extjs.gxt.ui.client.data.BaseModelData;

import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;
import com.prelert.data.Severity;


/**
 * Extension of the GXT BaseModelData class for a collection of ProbableCauseModel 
 * objects, aggregated by data source type, time and description.
 * 
 * @author Pete Harverson
 */
public class ProbableCauseModelCollection extends BaseModelData implements Serializable
{
	/**
	 * Sets the data source type of this probable cause collection e.g. p2ps logs or
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
	 * Returns the category of the data source of this probable cause collection
	 * e.g. notification or time series.
	 * @return the category of this probable cause collection data source.
	 */
	public DataSourceCategory getDataSourceCategory()
	{
		String category = get("dataSourceCategory");
		return DataSourceCategory.getValue(category);
	}
	
	
	/**
	 * Returns the name of the data source of this probable cause collection 
	 * e.g. p2ps logs or UDP error data.
	 * @return the data source name.
	 */
	public String getDataSourceName()
	{
		return get("dataSourceName");
	}


	/**
	 * Returns the description of this probable cause collection.
	 * @return the description.
	 */
	public String getDescription()
	{
		return get("description");
	}


	/**
	 * Sets the description for this probable cause collection.
	 * @param description  the description.
	 */
	public void setDescription(String description)
	{
		set("description", description);
	}
	
	
	/**
	 * Returns the start time of this probable cause collection i.e. the time of
	 * the earliest probable cause.
	 * @return the start time. If there is only one probable cause in the collection,
	 * then this will be the time of that single occurrence.
	 */
	public Date getStartTime()
	{
		Date startTime = null;
		ProbableCauseModel firstProbCause = getProbableCause(0);
		if (firstProbCause != null)
		{
			startTime = firstProbCause.getTime();
		}
		
		return startTime;		
	}
	
	
	/**
	 * Returns the end time of this probable cause collection i.e. the time of
	 * the latest probable cause.
	 * @return the end time. If there is only one probable cause in the collection,
	 * then this will be the time of that single occurrence.
	 */
	public Date getEndTime()
	{
		Date endTime = null;
		List<ProbableCauseModel> probCauses = getProbableCauses();
		int listSize = probCauses.size();
		if (listSize > 0)
		{
			endTime = probCauses.get(listSize-1).getTime();
		}
		
		return endTime;
	}
	
	
	/**
	 * Returns the count of this probable cause collection. This is equal to the
	 * sum of the counts of the constituent probable causes.
     * @return the count.
     */
    public int getCount()
    {
    	int count = get("count", new Integer(0));
    	return count;
    }


	/**
	 * Sets the count of this probable cause collection. This is equal to the
	 * sum of the counts of the constituent probable causes.
     * @param count the count.
     */
    public void setCount(int count)
    {
    	set("count", count);
    }
    
    
    /**
     * Returns the count of distinct sources (servers) over which the probable
     * causes occur.
     * @return the source count.
     */
    public int getSourceCount()
    {
    	int sourceCount = get("sourceCount", new Integer(0));
    	return sourceCount;
    }
    
    
    /**
     * Sets the count of distinct sources (servers) over which the probable
     * causes occur.
     * @param count the source count.
     */
    public void setSourceCount(int count)
    {
    	set("sourceCount", count);
    }
    
	
	/**
	 * Returns the list of probable causes which have been aggregated together
	 * by data source type, time and description into this collection.
	 * @return list of probable causes in this collection.
	 */
	public List<ProbableCauseModel> getProbableCauses()
	{
		return get("probableCauses", new ArrayList<ProbableCauseModel>());
	}
	
	
	/**
	 * Records the number of individual probable causes that have been aggregated
	 * together to form this collection.
	 * @param size the number of probable causes that were aggregated together.
	 */
	public void setSize(int size)
	{
		set("size", size);
	}
	
	
	/**
	 * Returns the number of individual probable causes that have been aggregated
	 * together to form this collection.
	 * @return the number of probable causes that were aggregated together.
	 */
	public int getSize()
	{
		int size = get("size", new Integer(0));
    	return size;
	}
	
	
	/**
	 * Sets the list of probable causes which have been aggregated together
	 * by data source type, time and description into this collection.
	 * @param probableCauses list of probable causes in this collection.
	 */
	public void setProbableCauses(List<ProbableCauseModel> probableCauses)
	{
		set("probableCauses", probableCauses);
	}
	
	
	/**
	 * Returns the probable cause at the specified index in the collection.
	 * @param index index of probable cause to return. 
	 * @return the element at the specified position in this collection, or
	 * <code>null</code> if the index is out of range
     *  (<tt>index &lt; 0 || index &gt;= size()</tt>).
	 */
	public ProbableCauseModel getProbableCause(int index)
	{
		ProbableCauseModel model = null;
		List<ProbableCauseModel> probCauses = getProbableCauses();
		if (probCauses != null && probCauses.size() > index)
		{
			model = probCauses.get(index);
		}
		
		return model;
	}
	
	
	/**
	 * Returns the probable cause mapped to the specified evidence id.
	 * @param evidenceId evidence id of probable cause to return. 
	 * @return the probable cause mapped to the specified evidence id, or <code>null</code>
	 * 		if there is no probable cause in the collection with this evidence id.
	 */
	public ProbableCauseModel getProbableCauseById(int evidenceId)
	{
		ProbableCauseModel model = null;
		for (ProbableCauseModel probCause : getProbableCauses())
		{
			if (probCause.getEvidenceId() == evidenceId)
			{
				model = probCause;
				break;
			}
		}
		
		return model;
	}
	
	
	/**
	 * Returns the id of this probable cause collection.
	 * For time series collections this is used to map to a unique index
	 * in the CSSColorChart.
	 * @return the <code>id</code>, or -1 if no id has been set.
	 */
	public int getId()
	{
		Integer idInt = get("id", new Integer(-1));
    	return idInt.intValue();
	}
	
	
	/**
	 * Sets an id for this probable cause collection.
	 * For time series collections this is used to map to a unique index
	 * in the CSSColorChart.
	 * @param id an <code>id</code> for the probable cause collection.
	 */
	public void setId(int id)
	{
		set("id", new Integer(id));
	}
	
	
	/**
	 * Returns the value of the severity property for this collection of probable
	 * cause objects, applicable for NOTIFICATION type data.
	 * @return the <code>severity</code>, such as 'minor', 'major' or 'critical', 
	 * or 'none' if there is no severity set.
	 */
	public Severity getSeverity()
    {
		String severityStr = get("severity", Severity.NONE.toString());
		return Enum.valueOf(Severity.class, severityStr.toUpperCase());
    }


	/**
	 * Sets the severity for this collection of probable cause objects, applicable 
	 * for NOTIFICATION type data.
	 * @param severity tthe <code>severity</code>, such as 'minor', 'major' or 
	 * 'critical', or 'none' if there is no severity set.
	 */
	public void setSeverity(Severity severity)
    {
		// Mar 2010: Store the Severity as a String as otherwise errors may occur
		// when this object is transported via GWT RPC.
    	set("severity", severity.toString());
    }
	
	
	/**
	 * Returns the flag which indicates if this aggregated probable cause is 
	 * being displayed.
	 * @return <code>true</code> if it is being shown, <code>false</code> otherwise.
	 */
	public boolean getDisplay()
	{
		return get("display", false);
	}
	
	
	/**
	 * Sets a flag to indicate whether this aggregated probable cause is 
	 * being displayed.
	 * @param display <code>true</code> if it is being shown, <code>false</code>
	 * 		otherwise.
	 */
	public void setDisplay(boolean display)
	{
		set("display", display);
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
