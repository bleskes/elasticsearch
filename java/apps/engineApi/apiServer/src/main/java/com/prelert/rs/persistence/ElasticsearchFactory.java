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

import java.util.Objects;

import org.apache.log4j.Logger;
import org.elasticsearch.client.Client;

import com.prelert.job.persistence.DataPersisterFactory;
import com.prelert.job.persistence.JobDataCountsPersisterFactory;
import com.prelert.job.persistence.JobProvider;
import com.prelert.job.persistence.JobDataDeleterFactory;
import com.prelert.job.persistence.UsagePersisterFactory;
import com.prelert.job.persistence.elasticsearch.ElasticsearchBulkDeleter;
import com.prelert.job.persistence.elasticsearch.ElasticsearchJobDataCountsPersister;
import com.prelert.job.persistence.elasticsearch.ElasticsearchJobDataPersister;
import com.prelert.job.persistence.elasticsearch.ElasticsearchPersister;
import com.prelert.job.persistence.elasticsearch.ElasticsearchUsagePersister;
import com.prelert.job.process.normaliser.BlockingQueueRenormaliser;
import com.prelert.job.process.output.parsing.ResultsReaderFactory;
import com.prelert.server.info.ServerInfoFactory;
import com.prelert.server.info.elasticsearch.ElasticsearchServerInfo;
import com.prelert.settings.PrelertSettings;

/**
 * A factory for the entire family of Elasticsearch-based classes
 */
public abstract class ElasticsearchFactory implements AutoCloseable
{
    private static final Logger LOGGER = Logger.getLogger(ElasticsearchFactory.class);

    protected static final String CLUSTER_NAME_KEY = "cluster.name";

    private static final String ES_INDEX_NUMBER_OF_REPLICAS = "es.index.number_of_replicas";
    private static final Integer DEFAULT_NUMBER_OF_REPLICAS = 0;
    private static final int MIN_NUMBER_OF_REPLICAS = 0;
    private static final int MAX_NUMBER_OF_REPLICAS = 10;

    private final Client m_Client;

    protected ElasticsearchFactory(Client client)
    {
        m_Client = Objects.requireNonNull(client);
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

    public JobDataDeleterFactory newJobDataDeleterFactory()
    {
        return jobId -> new ElasticsearchBulkDeleter(m_Client, jobId);
    }

    public abstract JobProvider newJobProvider();

    protected Client getClient()
    {
        return m_Client;
    }

    protected int numberOfReplicas()
    {
        int numberOfReplicas = PrelertSettings.getSettingOrDefault(ES_INDEX_NUMBER_OF_REPLICAS, DEFAULT_NUMBER_OF_REPLICAS);
        if (numberOfReplicas < MIN_NUMBER_OF_REPLICAS)
        {
            LOGGER.warn(ES_INDEX_NUMBER_OF_REPLICAS + " setting of " + numberOfReplicas
                    + " from config is too low - it will be increased to "
                    + MIN_NUMBER_OF_REPLICAS);
            numberOfReplicas = MIN_NUMBER_OF_REPLICAS;
        }
        else if (numberOfReplicas > MAX_NUMBER_OF_REPLICAS)
        {
            LOGGER.warn(ES_INDEX_NUMBER_OF_REPLICAS + " setting of " + numberOfReplicas
                    + " from config is too high - it will be reduced to "
                    + MAX_NUMBER_OF_REPLICAS);
            numberOfReplicas = MAX_NUMBER_OF_REPLICAS;
        }
        return numberOfReplicas;
    }

    /**
     * Closes the Elasticsearch client
     */
    @Override
    public void close()
    {
        m_Client.close();
    }
}
