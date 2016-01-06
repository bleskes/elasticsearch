/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2016     *
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

package com.prelert.rs.persistence;

import org.apache.log4j.Logger;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import com.prelert.job.persistence.DataPersisterFactory;
import com.prelert.job.persistence.JobDataCountsPersisterFactory;
import com.prelert.job.persistence.JobProvider;
import com.prelert.job.persistence.JobResultsDeleterFactory;
import com.prelert.job.persistence.UsagePersisterFactory;
import com.prelert.job.persistence.elasticsearch.ElasticsearchBulkDeleter;
import com.prelert.job.persistence.elasticsearch.ElasticsearchJobDataCountsPersister;
import com.prelert.job.persistence.elasticsearch.ElasticsearchJobDataPersister;
import com.prelert.job.persistence.elasticsearch.ElasticsearchJobProvider;
import com.prelert.job.persistence.elasticsearch.ElasticsearchPersister;
import com.prelert.job.persistence.elasticsearch.ElasticsearchUsagePersister;
import com.prelert.job.process.normaliser.BlockingQueueRenormaliser;
import com.prelert.job.process.output.parsing.ResultsReaderFactory;
import com.prelert.server.info.ServerInfoFactory;
import com.prelert.server.info.elasticsearch.ElasticsearchServerInfo;

/**
 * A factory for the entire family of Elasticsearch-based classes
 */
public class ElasticsearchFactory
{
    private static final Logger LOGGER = Logger.getLogger(ElasticsearchFactory.class);

    private final Node m_Node;
    private final Client m_Client;

    public ElasticsearchFactory(String elasticSearchHost, String elasticSearchClusterName,
            String portRange, String numProcessors)
    {
        m_Node = NodeBuilder.nodeBuilder()
                .settings(buildSettings(elasticSearchHost, portRange, numProcessors))
                .client(true)
                .clusterName(elasticSearchClusterName).node();
        m_Client = m_Node.client();
    }

    /**
     * Elasticsearch settings that instruct the node not to accept HTTP, not to
     * attempt multicast discovery and to only look for another node to connect
     * to on the given host.
     */
    private static Settings buildSettings(String host, String portRange, String numProcessors)
    {
        // Multicast discovery is expected to be disabled on the Elasticsearch
        // data node, so disable it for this embedded node too and tell it to
        // expect the data node to be on the same machine
        Builder builder = Settings.builder()
                .put("http.enabled", "false")
                .put("discovery.zen.ping.unicast.hosts", host);

        if (portRange != null && portRange.isEmpty() == false)
        {
            LOGGER.info("Using TCP port range " + portRange + " to connect to Elasticsearch");
            builder.put("transport.tcp.port", portRange);
        }
        if (numProcessors != null && numProcessors.isEmpty() == false)
        {
            LOGGER.info("Telling Elasticsearch there are " + numProcessors
                    + " processors on this machine");
            builder.put("processors", numProcessors);
        }

        return builder.build();
    }

    public JobProvider newJobProvider()
    {
        return new ElasticsearchJobProvider(m_Node, m_Client);
    }

    public ResultsReaderFactory newResultsReaderFactory(JobProvider jobProvider)
    {
        return new ResultsReaderFactory(
                jobId -> new ElasticsearchPersister(jobId, m_Client),
                jobId -> new BlockingQueueRenormaliser(jobId, jobProvider,
                        new ElasticsearchPersister(jobId, m_Client)));
    }

    public JobDataCountsPersisterFactory newJobDataCountsPersisterFactory()
    {
        return logger -> new ElasticsearchJobDataCountsPersister(m_Client, logger);
    }

    public UsagePersisterFactory newUsagePersisterFactory()
    {
        return logger -> new ElasticsearchUsagePersister(m_Client, logger);
    }

    public DataPersisterFactory newDataPersisterFactory()
    {
        return jobId -> new ElasticsearchJobDataPersister(jobId, m_Client);
    }

    public ServerInfoFactory newServerInfoFactory()
    {
        return new ElasticsearchServerInfo(m_Client);
    }

    public JobResultsDeleterFactory newJobResultsDeleterFactory()
    {
        return jobId -> new ElasticsearchBulkDeleter(m_Client, jobId);
    }
}
