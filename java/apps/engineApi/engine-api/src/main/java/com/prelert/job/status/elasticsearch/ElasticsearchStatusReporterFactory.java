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
package com.prelert.job.status.elasticsearch;

import org.apache.log4j.Logger;
import org.elasticsearch.client.Client;

import com.prelert.job.DataCounts;
import com.prelert.job.persistence.elasticsearch.ElasticsearchJobDataCountsPersister;
import com.prelert.job.status.StatusReporter;
import com.prelert.job.status.StatusReporterFactory;
import com.prelert.job.usage.UsageReporter;

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
	public StatusReporter newStatusReporter(String jobId, DataCounts counts,
			UsageReporter usageReporter, Logger logger)
	{
        return new StatusReporter(jobId, counts, usageReporter,
                                    new ElasticsearchJobDataCountsPersister(m_Client, logger),
                                    logger);
	}
}
