
package org.elasticsearch.xpack.prelert.job.persistence;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.logging.Loggers;

import java.util.Objects;

/**
 * A factory for the entire family of Elasticsearch-based classes
 */
public abstract class ElasticsearchFactory implements AutoCloseable
{
    private static final Logger LOGGER = Loggers.getLogger(ElasticsearchFactory.class);

    protected static final String CLUSTER_NAME_KEY = "cluster.name";

    private static final String ES_INDEX_NUMBER_OF_REPLICAS = "es.index.number_of_replicas";
    private static final Integer DEFAULT_NUMBER_OF_REPLICAS = 0;
    private static final int MIN_NUMBER_OF_REPLICAS = 0;
    private static final int MAX_NUMBER_OF_REPLICAS = 10;

    private final Client client;

    protected ElasticsearchFactory(Client client)
    {
        this.client = Objects.requireNonNull(client);
    }

    public abstract JobProvider newJobProvider();

    protected Client getClient()
    {
        return client;
    }


    /**
     * Closes the Elasticsearch client
     */
    @Override
    public void close()
    {
        client.close();
    }
}
