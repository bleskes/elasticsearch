/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.prelert.job.persistence;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.Client;

import java.util.Objects;

/**
 * A factory for the entire family of Elasticsearch-based classes
 */
public class ElasticsearchFactories implements AutoCloseable
{
    private final Client client;

    public ElasticsearchFactories(Client client) {
        this.client = Objects.requireNonNull(client);
    }

    public JobDataCountsPersisterFactory newJobDataCountsPersisterFactory() {
        return logger -> new ElasticsearchJobDataCountsPersister(client, logger);
    }

    public UsagePersisterFactory newUsagePersisterFactory() {
        // NORELEASE Go back to ElasticsearchUsagePersister once issue
        // #159 Scripting is not working in the new node project is resolved
        return new UsagePersisterFactory() {
            @Override
            public UsagePersister getInstance(Logger logger) {
                return (jobId, bytesRead, fieldsRead, recordsRead) -> {};

            }
        };
    }

    public JobResultsPeristerFactory newJobResultsPersisterFactory() {
        return jobId -> new ElasticsearchPersister(jobId, client);
    }

    /**
     * Closes the Elasticsearch client
     */
    @Override
    public void close()
    {
        client.close();
    }

    protected Client getClient()
    {
        return client;
    }
}
