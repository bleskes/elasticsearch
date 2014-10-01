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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.prelert.rs.data.ErrorCode;

/**
 * Autodetect analysis configuration options describes which fields are 
 * analysed and the functions to use. 
 * <p/>
 * The configuration can contain multiple detectors, a new anomaly detector will 
 * be created for each detector configuration. The other fields 
 * <code>BucketSpan, PartitionField</code> etc apply to all detectors.<p/> 
 * If a value has not been set it will be <code>null</code>
 * Object wrappers are used around integral types & booleans so they can take
 * <code>null</code> values.
 */
public class AnalysisConfig 
{
	/**
	 * Serialisation names
	 */
	static final public String BUCKET_SPAN = "bucketSpan";
	static final public String BATCH_SPAN = "batchSpan";
	static final public String PERIOD = "period";
	static final public String DETECTORS = "detectors";
	
	/**
	 * These values apply to all detectors
	 */
	private Long m_BucketSpan;
	private Long m_BatchSpan;
	private Long m_Period;
	private List<Detector> m_Detectors;
	
	/**
	 * Default constructor
	 */
	public AnalysisConfig()
	{
		m_Detectors = new ArrayList<>();
	}
	
	/**
	 * Construct an AnalysisConfig from a map. Detector objects are 
	 * nested elements in the map.
	 * @param values
	 */
	@SuppressWarnings("unchecked")
	public AnalysisConfig(Map<String, Object> values)
	{	
		this();
		
		if (values.containsKey(BUCKET_SPAN))
		{
			Object obj = values.get(BUCKET_SPAN);
			if (obj != null)
			{
				m_BucketSpan = ((Integer)obj).longValue();
			}
		}
		if (values.containsKey(BATCH_SPAN))
		{
			Object obj = values.get(BATCH_SPAN);
			if (obj != null)
			{
				m_BatchSpan = ((Integer)obj).longValue();
			}
		}
		if (values.containsKey(PERIOD))
		{
			Object obj = values.get(PERIOD);
			if (obj != null)
			{
				m_Period = ((Integer)obj).longValue();
			}
		}				
		if (values.containsKey(DETECTORS))
		{
			Object obj = values.get(DETECTORS);
			if (obj instanceof ArrayList)
			{
				for (Map<String, Object> detectorMap : (ArrayList<Map<String, Object>>)obj)
				{
					Detector detector = new Detector(detectorMap);
					m_Detectors.add(detector);
				}
			}
		
		}
	}
	
	/**
	 * The size of the interval the analysis is aggregated into measured in 
	 * seconds
	 * @return The bucketspan or <code>null</code> if not set
	 */
	public Long getBucketSpan()
	{
		return m_BucketSpan;
	}
	
	public void setBucketSpan(Long span)
	{
		m_BucketSpan = span;
	}	
	
	/**
	 * Interval into which to batch seasonal data measured in seconds
	 * @return The batchspan or <code>null</code> if not set
	 */
	public Long getBatchSpan() 
	{
		return m_BatchSpan;
	}
	
	public void setBatchSpan(Long m_BatchSpan) 
	{
		this.m_BatchSpan = m_BatchSpan;
	}
	
	/**
	 * The repeat interval for periodic data in multiples of
	 * {@linkplain #getBatchSpan()} 
	 * @return The period or <code>null</code> if not set
	 */
	public Long getPeriod() 
	{
		return m_Period;
	}
	
	public void setPeriod(Long m_Period) 
	{
		this.m_Period = m_Period;
	}

	/**
	 * The list of analysis detectors. In a valid configuration the list should
	 * contain at least 1 {@link Detector} 
	 * @return The Detectors used in this job
	 */
	public List<Detector> getDetectors()
	{
		return m_Detectors;
	}
	
	public void setDetectors(List<Detector> detectors)
	{
		m_Detectors = detectors;
	}
	
	/**
	 * Return the list of fields required by the analysis. 
	 * These are the partition, field, by field and over fields.
	 * <code>null</code> and empty strings are filtered from the 
	 * config
	 * 
	 * @return List of required fields.
	 */
	public List<String> analysisFields()
	{
		Set<String> fields = new TreeSet<>();
		
		for (Detector d : getDetectors())
		{
			if (d.getFieldName() != null)
			{
				fields.add(d.getFieldName());
			}
			if (d.getByFieldName() != null)
			{
				fields.add(d.getByFieldName() );
			}
			if (d.getOverFieldName() != null)
			{
				fields.add(d.getOverFieldName());
			}
			if (d.getPartitionFieldName() != null)
			{
				fields.add(d.getPartitionFieldName());
			}
		}
		
		// remove the null and empty strings
		fields.remove("");
		
		return new ArrayList<String>(fields);
	}
	
	
	
	public List<String> fields()
	{
		Set<String> fields = new HashSet<>();
		
		for (Detector d : getDetectors())
		{
			fields.add(d.getFieldName());
		}
		
		// remove the null and empty strings
		fields.remove("");
		fields.remove(null);
		
		return new ArrayList<String>(fields);
	}
	
	
	
	public List<String> byFields()
	{
		Set<String> fields = new HashSet<>();
		
		for (Detector d : getDetectors())
		{
			fields.add(d.getByFieldName());
		}
		
		// remove the null and empty strings
		fields.remove("");
		fields.remove(null);
		
		return new ArrayList<String>(fields);
	}
	
	public List<String> overFields()
	{
		Set<String> fields = new HashSet<>();
		
		for (Detector d : getDetectors())
		{
			fields.add(d.getOverFieldName());
		}
		
		// remove the null and empty strings
		fields.remove("");
		fields.remove(null);
		
		return new ArrayList<String>(fields);
	}
	
	
	public List<String> partitionFields()
	{
		Set<String> fields = new HashSet<>();
		
		for (Detector d : getDetectors())
		{
			fields.add(d.getPartitionFieldName());
		}
		
		// remove the null and empty strings
		fields.remove("");
		fields.remove(null);
		
		return new ArrayList<String>(fields);
	}
	
	/**
	 * The array of detectors are compared for equality but they are not sorted 
	 * first so this test could fail simply because the detector arrays are in
	 * different orders.
	 */
	@Override 
	public boolean equals(Object other)
	{
		if (this == other)
		{
			return true;
		}
		
		if (other instanceof AnalysisConfig == false)
		{
			return false;
		}
		
		AnalysisConfig that = (AnalysisConfig)other;

		if (this.m_Detectors.size() != that.m_Detectors.size())
		{
			return false;
		}
		
			
		boolean equal = true;
		for (int i=0; i<m_Detectors.size(); i++)
		{
			equal = equal && this.m_Detectors.get(i).equals(that.m_Detectors.get(i));
		}		
		
		if (equal == false)
		{
			return false;
		}
		
		equal = equal && JobDetails.bothNullOrEqual(this.m_BucketSpan, that.m_BucketSpan) &&
				JobDetails.bothNullOrEqual(this.m_BatchSpan, that.m_BatchSpan) &&
				JobDetails.bothNullOrEqual(this.m_Period, that.m_Period);
		
		return equal;
	}
	
	/**
	 * Checks the configuration is valid
	 * <ol>
	 * <li>Check that if non-null BucketSpan, BatchSpan and Period are &gt= 0</li>
	 * <li>Check there is at least one detector configured</li>
	 * <li>Check all the detectors are configured correctly</li>
	 * </ol>
	 * 	
	 * @return true
	 * @throws JobConfigurationException
	 */
	public boolean verify()
	throws JobConfigurationException
	{	
		if (m_BucketSpan != null && m_BucketSpan < 0)
		{
			throw new JobConfigurationException("BucketSpan cannot be < 0."
					+ " Value = " + m_BucketSpan, ErrorCode.INVALID_VALUE);
		}
		
		if (m_BatchSpan != null && m_BatchSpan < 0)
		{
			throw new JobConfigurationException("BatchSpan cannot be < 0."
					+ " Value = " + m_BatchSpan, ErrorCode.INVALID_VALUE);
		}
		
		if (m_Period != null && m_Period < 0)
		{
			throw new JobConfigurationException("Period cannot be < 0."
					+ " Value = " + m_Period, ErrorCode.INVALID_VALUE);
		}
		
		if (m_Detectors.size() == 0)
		{
			throw new JobConfigurationException("No detectors configured",
					ErrorCode.INCOMPLETE_CONFIGURATION);
		}
		
		for (Detector d : m_Detectors)
		{
			d.verify();
		}
		
		return true;
	}
}
