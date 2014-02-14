/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Inc 2006-2014     *
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
 * on +44 (0)20 7953 7243 or email to legal@prelert.com.    *
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

/**
 * Analysis options 
 */
public class AnalysisOptions 
{
	/**
	 * Serialisation field names
	 */
	static final public String MAX_FIELD_VALUES = "maxFieldValues";
	static final public String MAX_TIME_BUCKETS = "maxTimeBuckets";
	
	/**
	 * Defaults
	 */
	static final public long DEFAULT_MAX_FIELD_VALUES = 50000;
	static final public long DEFAULT_MAX_TIME_BUCKETS = 1000000;
	
	private long m_MaxFieldValues;
	private long m_MaxTimeBuckets;
	
	public AnalysisOptions()
	{
		m_MaxFieldValues = DEFAULT_MAX_FIELD_VALUES;
		m_MaxTimeBuckets = DEFAULT_MAX_TIME_BUCKETS;
	}

	public AnalysisOptions(long maxFieldValues, long maxTimeBuckets)
	{
		m_MaxFieldValues = maxFieldValues;
		m_MaxTimeBuckets = maxTimeBuckets;
	}
	
	public AnalysisOptions(Map<String, Object> values)
	{
		this();
		
		if (values.containsKey(MAX_FIELD_VALUES))
		{
			Object obj = values.get(MAX_FIELD_VALUES);
			if (obj != null)
			{
				m_MaxFieldValues = (Long)obj;
			}
		}	
		if (values.containsKey(MAX_TIME_BUCKETS))
		{
			Object obj = values.get(MAX_TIME_BUCKETS);
			if (obj != null)
			{
				m_MaxTimeBuckets = (Long)obj;
			}
		}		
		
	}
		
	/**
	 * Maximum number of distinct values of a single field before analysis
	 * of that field will be halted
	 * @return
	 */
	public long getMaxFieldValues()
	{
		return m_MaxFieldValues;
	}
	
	/**
	 *  Maximum number of time buckets to process during anomaly detection 
	 *  before ceasing to output results
	 * @return
	 */
	public long getMaxTimeBuckets()
	{
		return m_MaxTimeBuckets;
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
		
		if (other instanceof AnalysisOptions == false)
		{
			return false;
		}
		
		AnalysisOptions that = (AnalysisOptions)other;
		return (this.m_MaxFieldValues == that.m_MaxFieldValues) &&
				(this.m_MaxTimeBuckets == that.m_MaxTimeBuckets);
	}

}
