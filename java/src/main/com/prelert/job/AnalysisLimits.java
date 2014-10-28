/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2014     *
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
 ************************************************************/

package com.prelert.job;

import java.util.Map;

import com.prelert.rs.data.ErrorCode;

/**
 * Analysis limits for autodetect (max field values, max time buckets). 
 * 
 * If an option has not been set it's value will be 0 in which case it
 * shouldn't be used so the default value is picked up instead.
 */
public class AnalysisLimits 
{
	/**
	 * Serialisation field names
	 */
	static final public String MAX_FIELD_VALUES = "maxFieldValues";
	static final public String MAX_TIME_BUCKETS = "maxTimeBuckets";
	
	private long m_MaxFieldValues;
	private long m_MaxTimeBuckets;
	
	/**
	 * Initialise values to 0.
	 * If the values are 0 they haven't been set 
	 */	
	public AnalysisLimits()
	{
		m_MaxFieldValues = 0;
		m_MaxTimeBuckets = 0;
	}
	
	public AnalysisLimits(long maxFieldValues, long maxTimeBuckets)
	{
		m_MaxFieldValues = maxFieldValues;
		m_MaxTimeBuckets = maxTimeBuckets;
	}
	
	/**
	 * Create and set field values from the Map.
	 * @param values
	 */
	public AnalysisLimits(Map<String, Object> values)
	{
		this();
		
		if (values.containsKey(MAX_FIELD_VALUES))
		{
			Object obj = values.get(MAX_FIELD_VALUES);
			if (obj != null)
			{
				m_MaxFieldValues = ((Number)obj).longValue();
			}
		}	
		if (values.containsKey(MAX_TIME_BUCKETS))
		{
			Object obj = values.get(MAX_TIME_BUCKETS);
			if (obj != null)
			{
				m_MaxTimeBuckets = ((Number)obj).longValue();
			}
		}		
		
	}
		
	/**
	 * Maximum number of distinct values of a single field before analysis
	 * of that field will be halted. If 0 then this is an invalid and the
	 * native process's default will be used. 
	 * @return The max distinct values in a single field
	 */
	public long getMaxFieldValues()
	{
		return m_MaxFieldValues;
	}
	
	public void setMaxFieldValues(long value)
	{
		m_MaxFieldValues = value;
	}
	
	/**
	 *  Maximum number of time buckets to process during anomaly detection 
	 *  before ceasing to output results. If 0 then this is an invalid and the
	 *  native process's default will be used. 
	 *  
	 * @return The max number of buckets to process in the job
	 */
	public long getMaxTimeBuckets()
	{
		return m_MaxTimeBuckets;
	}
	
	public void setMaxTimeBuckets(long value)
	{
		m_MaxTimeBuckets = value;
	}
	
	
	/**
	 * Overridden equality test
	 */
	public boolean equals(Object other)
	{
		if (this == other)
		{
			return true;
		}
		
		if (other instanceof AnalysisLimits == false)
		{
			return false;
		}
		
		AnalysisLimits that = (AnalysisLimits)other;
		return (this.m_MaxFieldValues == that.m_MaxFieldValues) &&
				(this.m_MaxTimeBuckets == that.m_MaxTimeBuckets);
	}
	
	/**
	 * Checks the analysis options and throws an exception if 
	 * any fields are invalid.
	 * 
	 * A value of 0 means use the default in autodetect.
	 * 
	 * @return true
	 * @throws JobConfigurationException
	 */
	public boolean verify()
	throws JobConfigurationException
	{
		if (m_MaxFieldValues != 0 && m_MaxFieldValues < 2)
		{
			throw new JobConfigurationException(
					"Invalid Analysis limit MaxFieldValues must be >= 2",
					ErrorCode.INVALID_VALUE);
		}
		if (m_MaxTimeBuckets != 0 && m_MaxTimeBuckets < 2)
		{
			throw new JobConfigurationException(
					"Invalid Analysis limit MaxTimeBuckets must be >= 2",
					ErrorCode.INVALID_VALUE);		
		}
		
		return true;
	}
	
}
