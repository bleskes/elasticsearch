
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
