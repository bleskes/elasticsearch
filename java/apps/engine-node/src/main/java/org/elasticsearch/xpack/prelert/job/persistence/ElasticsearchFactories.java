
package org.elasticsearch.xpack.prelert.job.persistence;

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
        return logger -> new ElasticsearchUsagePersister(client, logger);
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
