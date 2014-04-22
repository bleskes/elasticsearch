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

import com.prelert.rs.data.ErrorCodes;

/**
 * This class encapsulates all the data required to create a new job it
 * does not represent the state of a created job (see {@linkplain JobDetails}
 * for that).
 * <p/>
 * If a value has not been set it will be <code>null</code> Object wrappers 
 * are used around integral types & booleans so they can take <code>null</code> 
 * values.
 */
public class JobConfiguration 
{
	
	private AnalysisConfig m_AnalysisConfig;
	private AnalysisOptions m_AnalysisOptions;
	private DataDescription m_DataDescription;
	private String m_ReferenceJobId;
	private Long m_Timeout;
	
	
	public JobConfiguration()
	{
	}
	
	public JobConfiguration(String jobReferenceId)
	{
		this();
		m_ReferenceJobId = jobReferenceId;
	}
	
	public JobConfiguration(AnalysisConfig analysisConfig)
	{
		this();
		m_AnalysisConfig = analysisConfig;
	}
	
	/**
	 * The analysis configuration. A properly configured job must have 
	 * a valid AnalysisConfig
	 * @return AnalysisConfig or null if not set.
	 */
	public AnalysisConfig getAnalysisConfig() 
	{
		return m_AnalysisConfig;
	}
	
	public void setAnalysisConfig(AnalysisConfig config) 
	{
		m_AnalysisConfig = config;
	}
	
	/**
	 * The analysis options 
	 * 
	 * @return AnalysisOptions or null if not set.
	 */
	public AnalysisOptions getAnalysisOptions() 
	{
		return m_AnalysisOptions;
	}

	public void setAnalysisOptions(AnalysisOptions options) 
	{
		m_AnalysisOptions = options;
	}
	
	/**
	 * If the job is to be created with the same configuration as a previously
	 * run job then this is the id of that job. If set then this option
	 * overrides the {@linkplain #getAnalysisConfig()} settings i.e. they will
	 * be ignored.
	 * @return A String or <code>null</code> if not set
	 */
	public String getReferenceJobId() 
	{
		return m_ReferenceJobId;
	}

	public void setReferenceJobId(String refId) 
	{
		m_ReferenceJobId = refId;
	}
	
	/**
	 * The timeout period for the job in seconds
	 * @return The timeout in seconds
	 */
	public Long getTimeout() 
	{
		return m_Timeout;
	}
	
	public void setTimeout(Long timeout) 
	{
		m_Timeout = timeout;
	}
	
	
	/**
	 * If not set the input data is assumed to be csv with a '_time' field 
	 * in epoch format. 
	 * @return A DataDescription or <code>null</code>
	 * @see DataDescription
	 */
	public DataDescription getDataDescription()
	{
		return m_DataDescription;
	}
	
	public void setDataDescription(DataDescription description)
	{
		m_DataDescription = description;
	}
	
	
	/**
	 * Builder for constructing JobConfiguration instances.
	 */
	static public class JobConfigurationBuilder
	{
		private JobConfiguration m_JobConfig;
		
		public JobConfigurationBuilder(String jobReferenceId)
		{
			m_JobConfig = new JobConfiguration(jobReferenceId);
		}
		
		public JobConfigurationBuilder(AnalysisConfig analysisConfig)
		{
			m_JobConfig = new JobConfiguration(analysisConfig);
		}
				
		public JobConfigurationBuilder analysisOptions(AnalysisOptions analysisOptions)
		{		
			m_JobConfig.m_AnalysisOptions = analysisOptions;
			return this;
		}
		
		public JobConfigurationBuilder dataDescription(DataDescription dataDescription)
		{		
			m_JobConfig.m_DataDescription = dataDescription;
			return this;
		}
				
		public JobConfigurationBuilder timeout(Long timeout)
		{
			m_JobConfig.m_Timeout = timeout;
			return this;
		}
		
		public JobConfiguration build()
		{
			return m_JobConfig;
		}
	}
	
	/**
	 * Checks the job configuration settings and throws an exception
	 * if any values are invalid
	 * 
	 * <ol>
	 * <li>Either an AnalysisConfig or Job reference must be set</li>
	 * <li>Verify {@link AnalysisConfig#verify() AnalysisConfig}</li>
	 * <li>Verify {@link AnalysisOptions#verify() AnalysisOptions}</li>
	 * <li>Verify {@link DataDescription#verify() DataDescription}</li>
	 * <li>Check timeout is a +ve number</li>
	 * <li></li>
	 * </ol>
	 *  
	 * @return true
	 * @throws JobConfigurationException
	 */
	public boolean verify()
	throws JobConfigurationException
	{
		if (m_AnalysisConfig == null && m_ReferenceJobId == null)
		{
			throw new JobConfigurationException("Either an an AnalysisConfig or "
					+ " job reference id must be set",
					ErrorCodes.INCOMPLETE_CONFIGURATION);
		}
		
		if (m_AnalysisConfig != null)
		{
			m_AnalysisConfig.verify();
		}
		if (m_AnalysisOptions != null)
		{
			m_AnalysisOptions.verify();
		}
		
		if (m_DataDescription != null)
		{
			m_DataDescription.verify();
		}
		
		if (m_Timeout != null && m_Timeout < 0)
		{
			throw new JobConfigurationException("Timeout can not be a negative "
					+ "number. Value = " + m_Timeout,
					ErrorCodes.INVALID_VALUE);
		}
		
		return true;
	}
}
