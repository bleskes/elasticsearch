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

package com.prelert.rs.resources;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import com.prelert.job.manager.JobManager;
import com.prelert.rs.provider.ElasticSearchExceptionMapper;
import com.prelert.rs.provider.JobConfigurationExceptionMapper;
import com.prelert.rs.provider.JobInUseExceptionMapper;
import com.prelert.rs.provider.UnknownJobExceptionMapper;
import com.prelert.rs.provider.NativeProcessRunExceptionMapper;
import com.prelert.rs.provider.PaginationWriter;
import com.prelert.rs.provider.JobConfigurationMessageBodyReader;
import com.prelert.rs.provider.SingleDocumentWriter;

/**
 * Web application class contains the singleton objects accessed by the
 * resource classes 
 */

public class PrelertWebApp extends Application
{
	/**
	 * The default ElasticSearch Cluster name
	 */
	public static final String DEFAULT_CLUSTER_NAME = "prelert";
	
	public static final String ES_CLUSTER_NAME_PROP = "es.cluster.name";
	
	private Set<Class<?>> m_ResourceClasses;
	private Set<Object> m_Singletons;
	
	private JobManager m_JobManager;
	
	public PrelertWebApp()
	{
		m_ResourceClasses = new HashSet<>();	    
		m_ResourceClasses.add(ApiBase.class);
		m_ResourceClasses.add(Jobs.class);
		m_ResourceClasses.add(Data.class);
		m_ResourceClasses.add(Results.class);	   
		m_ResourceClasses.add(Detectors.class);
		m_ResourceClasses.add(Logs.class);
		
		// Message body writers
		m_ResourceClasses.add(PaginationWriter.class);	   
		m_ResourceClasses.add(SingleDocumentWriter.class);	   
		m_ResourceClasses.add(JobConfigurationMessageBodyReader.class);	  
		
		// Exception mappers
		m_ResourceClasses.add(NativeProcessRunExceptionMapper.class);
		m_ResourceClasses.add(ElasticSearchExceptionMapper.class);
		m_ResourceClasses.add(JobConfigurationExceptionMapper.class);
		m_ResourceClasses.add(JobInUseExceptionMapper.class);
		m_ResourceClasses.add(UnknownJobExceptionMapper.class);
		
		String elasticSearchClusterName = System.getProperty(ES_CLUSTER_NAME_PROP);
		if (elasticSearchClusterName == null)
		{
			elasticSearchClusterName = DEFAULT_CLUSTER_NAME;
		}
		m_JobManager = new JobManager(elasticSearchClusterName);
				
		m_Singletons = new HashSet<>();
		m_Singletons.add(m_JobManager);	
	}
	
	@Override
	public Set<Class<?>> getClasses() 
	{
	    return m_ResourceClasses;
	}
	
	@Override
	public Set<Object> getSingletons()
	{
		return m_Singletons;
	}
}
