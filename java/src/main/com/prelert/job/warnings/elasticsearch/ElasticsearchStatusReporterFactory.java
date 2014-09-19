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
package com.prelert.job.warnings.elasticsearch;

import org.apache.log4j.Logger;
import org.elasticsearch.client.Client;

import com.prelert.job.JobDetails;
import com.prelert.job.usage.UsageReporter;
import com.prelert.job.warnings.StatusReporter;
import com.prelert.job.warnings.StatusReporterFactory;

public class ElasticsearchStatusReporterFactory implements StatusReporterFactory 
{
	private Client m_Client;
	
	/**
	 * Construct the factory
	 * 
	 * @param node The Elasticsearch node
	 */
	public ElasticsearchStatusReporterFactory(Client client)
	{
		m_Client = client;
	}

	@Override
	public StatusReporter newStatusReporter(String jobId, JobDetails.Counts counts,
			UsageReporter usageReporter, Logger logger) 
	{
		StatusReporter reporter =  new ElasticsearchStatusReporter(m_Client, 
				usageReporter, jobId, counts, logger);
		return reporter;
	}
}
