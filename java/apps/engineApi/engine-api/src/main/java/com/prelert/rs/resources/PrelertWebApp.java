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
import com.prelert.job.manager.JobManager;
import com.prelert.job.persistence.elasticsearch.ElasticsearchDataPersisterFactory;
import com.prelert.job.persistence.elasticsearch.ElasticsearchJobProvider;
import com.prelert.job.persistence.elasticsearch.ElasticsearchResultsReaderFactory;
import com.prelert.job.process.ProcessManager;
import com.prelert.job.status.elasticsearch.ElasticsearchStatusReporterFactory;
import com.prelert.job.usage.elasticsearch.ElasticsearchUsageReporterFactory;
import com.prelert.rs.provider.AcknowledgementWriter;
import com.prelert.rs.provider.AlertMessageBodyWriter;
import com.prelert.rs.provider.DataCountsWriter;
import com.prelert.rs.provider.DataUploadExceptionMapper;
import com.prelert.rs.provider.ElasticsearchExceptionMapper;
import com.prelert.rs.provider.JobConfigurationMessageBodyReader;
import com.prelert.rs.provider.JobExceptionMapper;
import com.prelert.rs.provider.NativeProcessRunExceptionMapper;
import com.prelert.rs.provider.PaginationWriter;
import com.prelert.rs.provider.SingleDocumentWriter;

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

	public static final String ES_TRANSPORT_PORT_RANGE = "es.transport.port";

	public static final String PERSIST_RECORDS = "persist.records";

	private Set<Class<?>> m_ResourceClasses;
	private Set<Object> m_Singletons;

	private JobManager m_JobManager;
	private AlertManager m_AlertManager;

	public PrelertWebApp()
	{
		m_ResourceClasses = new HashSet<>();
		m_ResourceClasses.add(ApiBase.class);
		m_ResourceClasses.add(AlertsLongPoll.class);
		m_ResourceClasses.add(Jobs.class);
		m_ResourceClasses.add(Data.class);
		m_ResourceClasses.add(DataLoad.class);
		m_ResourceClasses.add(Preview.class);
		m_ResourceClasses.add(Buckets.class);
		m_ResourceClasses.add(CategoryDefinitions.class);
		m_ResourceClasses.add(Records.class);
		m_ResourceClasses.add(Logs.class);

		// Message body writers
		m_ResourceClasses.add(AcknowledgementWriter.class);
		m_ResourceClasses.add(AlertMessageBodyWriter.class);
		m_ResourceClasses.add(DataCountsWriter.class);
		m_ResourceClasses.add(JobConfigurationMessageBodyReader.class);
		m_ResourceClasses.add(PaginationWriter.class);
		m_ResourceClasses.add(SingleDocumentWriter.class);


		// Exception mappers
		m_ResourceClasses.add(ElasticsearchExceptionMapper.class);
		m_ResourceClasses.add(NativeProcessRunExceptionMapper.class);
		m_ResourceClasses.add(JobExceptionMapper.class);
		m_ResourceClasses.add(DataUploadExceptionMapper.class);


		String elasticSearchClusterName = System.getProperty(ES_CLUSTER_NAME_PROP);
		if (elasticSearchClusterName == null)
		{
			elasticSearchClusterName = DEFAULT_CLUSTER_NAME;
		}
		String portRange = System.getProperty(ES_TRANSPORT_PORT_RANGE);


		ElasticsearchJobProvider esJob = new ElasticsearchJobProvider(
				elasticSearchClusterName, portRange);

		m_JobManager = new JobManager(esJob, createProcessManager(esJob));

		m_AlertManager = new AlertManager(esJob, m_JobManager);


		m_Singletons = new HashSet<>();
		m_Singletons.add(m_JobManager);
		m_Singletons.add(m_AlertManager);
	}

	private static ProcessManager createProcessManager(ElasticsearchJobProvider jobProvider)
	{
	    return new ProcessManager(jobProvider,
                new ElasticsearchResultsReaderFactory(jobProvider),
                new ElasticsearchStatusReporterFactory(jobProvider.getClient()),
                new ElasticsearchUsageReporterFactory(jobProvider.getClient()),
                new ElasticsearchDataPersisterFactory(jobProvider.getClient())
        );
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
