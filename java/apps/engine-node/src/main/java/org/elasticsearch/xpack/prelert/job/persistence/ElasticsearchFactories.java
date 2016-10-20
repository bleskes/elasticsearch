
package org.elasticsearch.xpack.prelert.job.persistence;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.logging.Loggers;

import java.util.Objects;

/**
 * A factory for the entire family of Elasticsearch-based classes
 */
public class ElasticsearchFactories implements AutoCloseable
{
    private static final Logger LOGGER = Loggers.getLogger(ElasticsearchFactories.class);

    protected static final String CLUSTER_NAME_KEY = "cluster.name";

    private static final String ES_INDEX_NUMBER_OF_REPLICAS = "es.index.number_of_replicas";
    private static final Integer DEFAULT_NUMBER_OF_REPLICAS = 0;
    private static final int MIN_NUMBER_OF_REPLICAS = 0;
    private static final int MAX_NUMBER_OF_REPLICAS = 10;

    private final Client client;

    public ElasticsearchFactories(Client client) {
        this.client = Objects.requireNonNull(client);
    }

    public JobProvider newJobProvider() {
        return new ElasticsearchJobProvider(null, client, numberOfReplicas());
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

    private static int numberOfReplicas() {
        return 0;
    }
}
