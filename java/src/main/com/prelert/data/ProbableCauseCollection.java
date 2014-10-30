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
 * Class holding a collection of ProbableCause objects, aggregated by 
 * data source type and description.
 * 
 * @author Pete Harverson
 */
public class ProbableCauseCollection implements Serializable
{
    private static final long serialVersionUID = -5107931253910990381L;
    
	private List<ProbableCause>		m_ProbableCauses;
	private int						m_Size;
	private int						m_Count;
	private int						m_SourceCount;
	
	
	/**
	 * Sets the list of probable causes which have been aggregated together
	 * into this collection.
	 * @param probableCauses list of probable causes in this collection.
	 */
	public void setProbableCauses(List<ProbableCause> probableCauses)
	{
		m_ProbableCauses = probableCauses;
	}
	
	
	/**
	 * Returns the probable causes held in this collection. Note that for
	 * notifications, not all the individual notifications may be stored in
	 * the collection, but just the first and last occurrences.
	 * @return the list of probable causes.
	 */
	public List<ProbableCause> getProbableCauses()
	{
		return m_ProbableCauses;
	}
	
	
	/**
	 * Returns the probable cause at the specified index in the collection.
	 * @param index index of probable cause to return. 
	 * @return the element at the specified position in this collection or
	 * <code>null</code> if there are no probable causes in the aggregation.
	 * @throws IndexOutOfBoundsException if the index is out of range
     *  (<tt>index &lt; 0 || index &gt;= size()</tt>).
	 */
	public ProbableCause getProbableCause(int index)
	{
		ProbableCause probCause = null;
		if (m_ProbableCauses != null)
		{
			probCause = m_ProbableCauses.get(index);
		}
		
		return probCause;
	}
	
	
	/**
	 * Records the number of individual probable causes that have been aggregated
	 * together to form this collection.
	 * @param size the number of probable causes that were aggregated together.
	 */
	public void setSize(int size)
	{
		m_Size = size;
	}
	
	
	/**
	 * Returns the number of individual probable causes that have been aggregated
	 * together to form this collection.
	 * @return the number of probable causes that were aggregated together.
	 */
	public int getSize()
	{
		return m_Size;
	}
	
	
	/**
	 * Returns the data source type of the probable causes in this collection
	 * e.g. p2ps logs or UDP error data.
	 * @return the data source type.
	 */
	public DataSourceType getDataSourceType()
	{
		DataSourceType dsType = null;
		
		if (m_ProbableCauses != null && m_ProbableCauses.size() > 0)
		{
			dsType = m_ProbableCauses.get(0).getDataSourceType();
		}
		
		return dsType;
	}
	
	
	/**
	 * Returns the category of the data source of this probable causes in this
	 * collection e.g. notification or time series.
	 * @return the category of the probable cause data source.
	 */
	public DataSourceCategory getDataSourceCategory()
	{
		DataSourceCategory category = null;
		DataSourceType dsType = getDataSourceType();
		if (dsType != null)
		{
			category = dsType.getDataCategory();
		}
		
		return category;
	}

	
	/**
	 * Returns the count of this probable cause collection. This is equal to the
	 * sum of the counts of the constituent probable causes.
     * @return the count.
     */
    public int getCount()
    {
    	return m_Count;
    }


	/**
	 * Sets the count of this probable cause collection. This is equal to the
	 * sum of the counts of the constituent probable causes.
     * @param count the count.
     */
    public void setCount(int count)
    {
    	m_Count = count;
    }
    
    
    /**
     * Returns the count of distinct sources (servers) over which the probable
     * causes occur.
     * @return the source count.
     */
    public int getSourceCount()
    {
    	return m_SourceCount;
    }
    
    
    /**
     * Sets the count of distinct sources (servers) over which the probable
     * causes occur.
     * @param count the source count.
     */
    public void setSourceCount(int count)
    {
    	m_SourceCount = count;
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
		ProbableCause firstProbCause = getProbableCause(0);
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
		List<ProbableCause> probCauses = getProbableCauses();
		int listSize = probCauses.size();
		if (listSize > 0)
		{
			endTime = probCauses.get(listSize-1).getTime();
		}
		
		return endTime;
	}

}
