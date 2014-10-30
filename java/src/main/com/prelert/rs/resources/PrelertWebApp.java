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

package com.prelert.rs.resources;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import com.prelert.job.alert.manager.AlertManager;
import com.prelert.job.alert.persistence.elasticsearch.ElasticsearchAlertPersister;
import com.prelert.job.manager.JobManager;
import com.prelert.job.persistence.DataPersisterFactory;
import com.prelert.job.persistence.elasticsearch.ElasticsearchDataPersisterFactory;
import com.prelert.job.persistence.elasticsearch.ElasticsearchJobProvider;
import com.prelert.job.persistence.elasticsearch.ElasticsearchResultsReaderFactory;
import com.prelert.job.persistence.none.NoneJobDataPersisterFactory;
import com.prelert.job.status.elasticsearch.ElasticsearchStatusReporterFactory;
import com.prelert.job.usage.elasticsearch.ElasticsearchUsageReporterFactory;
import com.prelert.rs.provider.AlertMessageBodyWriter;
import com.prelert.rs.provider.ElasticsearchExceptionMapper;
import com.prelert.rs.provider.HighProportionOfBadTimestampsExceptionMapper;
import com.prelert.rs.provider.JobIdAlreadyExistsExceptionMapper;
import com.prelert.rs.provider.JobConfigurationExceptionMapper;
import com.prelert.rs.provider.JobConfigurationMessageBodyReader;
import com.prelert.rs.provider.JobInUseExceptionMapper;
import com.prelert.rs.provider.MissingFieldExceptionMapper;
import com.prelert.rs.provider.NativeProcessRunExceptionMapper;
import com.prelert.rs.provider.OutOfOrderRecordsExceptionMapper;
import com.prelert.rs.provider.PaginationWriter;
import com.prelert.rs.provider.SingleDocumentWriter;
import com.prelert.rs.provider.TooManyJobsExceptionMapper;
import com.prelert.rs.provider.UnknownJobExceptionMapper;

/**
 * Web application class contains the singleton objects accessed by the
 * resource classes 
 */

public class PrelertWebApp extends Application
{
	/**
	 * The default Elasticsearch Cluster name
	 */
	public static final String DEFAULT_CLUSTER_NAME = "prelert";
	
	public static final String ES_CLUSTER_NAME_PROP = "es.cluster.name";
	
	public static final String PERSIST_RECORDS = "persist.records";
	
	private Set<Class<?>> m_ResourceClasses;
	private Set<Object> m_Singletons;
	
	private JobManager m_JobManager;
	private AlertManager m_AlertManager;
	
	public PrelertWebApp()
	{
		m_ResourceClasses = new HashSet<>();	    
		m_ResourceClasses.add(ApiBase.class);
		m_ResourceClasses.add(Alerts.class);
		m_ResourceClasses.add(AlertsLongPoll.class);
		m_ResourceClasses.add(Jobs.class);
		m_ResourceClasses.add(Data.class);
		m_ResourceClasses.add(Buckets.class);	   
		m_ResourceClasses.add(Records.class);	   
		m_ResourceClasses.add(Logs.class);
		
		// Message body writers
		m_ResourceClasses.add(AlertMessageBodyWriter.class);
		m_ResourceClasses.add(PaginationWriter.class);
		m_ResourceClasses.add(SingleDocumentWriter.class);	   
		m_ResourceClasses.add(JobConfigurationMessageBodyReader.class);	  
		
		// Exception mappers
		m_ResourceClasses.add(ElasticsearchExceptionMapper.class);
		m_ResourceClasses.add(HighProportionOfBadTimestampsExceptionMapper.class);
		m_ResourceClasses.add(JobIdAlreadyExistsExceptionMapper.class);
		m_ResourceClasses.add(JobConfigurationExceptionMapper.class);
		m_ResourceClasses.add(JobInUseExceptionMapper.class);
		m_ResourceClasses.add(MissingFieldExceptionMapper.class);
		m_ResourceClasses.add(NativeProcessRunExceptionMapper.class);
		m_ResourceClasses.add(OutOfOrderRecordsExceptionMapper.class);
		m_ResourceClasses.add(TooManyJobsExceptionMapper.class);
		m_ResourceClasses.add(UnknownJobExceptionMapper.class);
		
		String elasticSearchClusterName = System.getProperty(ES_CLUSTER_NAME_PROP);
		if (elasticSearchClusterName == null)
		{
			elasticSearchClusterName = DEFAULT_CLUSTER_NAME;
		}
		
		ElasticsearchJobProvider esJob = new ElasticsearchJobProvider(
				elasticSearchClusterName);
		
		DataPersisterFactory dataPersisterFactory = new NoneJobDataPersisterFactory();
		if (System.getProperty(PERSIST_RECORDS) != null)
		{
			dataPersisterFactory = new ElasticsearchDataPersisterFactory(esJob.getClient());
		}
		
		m_JobManager = new JobManager(esJob, 
				new ElasticsearchResultsReaderFactory(esJob),
				new ElasticsearchStatusReporterFactory(esJob.getClient()),
				new ElasticsearchUsageReporterFactory(esJob.getClient()),
				dataPersisterFactory
			);

		
		m_AlertManager = new AlertManager(
				new ElasticsearchAlertPersister(esJob.getClient()));
				
		m_Singletons = new HashSet<>();
		m_Singletons.add(m_JobManager);	
		m_Singletons.add(m_AlertManager);	
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
