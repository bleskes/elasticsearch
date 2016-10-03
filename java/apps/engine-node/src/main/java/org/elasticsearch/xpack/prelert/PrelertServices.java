

package org.elasticsearch.xpack.prelert;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.xpack.prelert.job.manager.JobManager;
import org.elasticsearch.xpack.prelert.job.manager.actions.Action;
import org.elasticsearch.xpack.prelert.job.manager.actions.ActionGuardian;
import org.elasticsearch.xpack.prelert.job.manager.actions.LocalActionGuardian;
import org.elasticsearch.xpack.prelert.job.manager.actions.ScheduledAction;
import org.elasticsearch.xpack.prelert.job.persistence.ElasticsearchFactory;
import org.elasticsearch.xpack.prelert.job.persistence.ElasticsearchJobProvider;
import org.elasticsearch.xpack.prelert.job.persistence.JobProvider;

public class PrelertServices {

    public static final String ES_CLUSTER_NAME_PROP = "es.cluster.name";
    public static final String DEFAULT_CLUSTER_NAME = "prelert";

    public static final String ES_TRANSPORT_PORT_RANGE = "es.transport.tcp.port";
    public static final String DEFAULT_ES_TRANSPORT_PORT_RANGE = "9300-9400";

    public static final String ES_NETWORK_PUBLISH_HOST_PROP = "es.network.publish_host";
    private static final String DEFAULT_NETWORK_PUBLISH_HOST = "127.0.0.1";

    private static final String ES_PROCESSORS_PROP = "es.processors";

    private static final String IGNORE_DOWNTIME_ON_STARTUP_PROP = "ignore.downtime.on.startup";
    private static final boolean DEFAULT_IGNORE_DOWNTIME_ON_STARTUP = true;

    private static final String ENCRYPTION_KEY_FILE = "aes.key";
    private static final String ENCRYPTION_TRANSFORMATION = "AES/CBC/PKCS5Padding";
    // The size of this array may need to be changed if the transformation on the line above is changed
    private static final byte[] DEV_KEY_BYTES = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};

    public static final String ZOOKEEPER_CONNECTION_PROP = "zookeeper.connection";

    /**
     * This property specifies the client that should be used to connect
     * to the storage of the results.
     * <p>
     * Available options:
     * <ul>
     * <li> <b>es-node</b> will connect to the es.host via a node client
     * <li> <b>es-transport</b> will create a transport client that is aware of the nodes specified in es.host
     * <li> <b>es-auto</b> will choose es-node if es.host is localhost or localhost6; otherwise es-transport
     * </ul>
     */
    private static final String RESULTS_STORAGE_CLIENT_PROP = "results.storage.client";
    private static final String ES_NODE = "es-node";
    private static final String ES_TRANSPORT = "es-transport";
    private static final String ES_AUTO = "es-auto";

    public static final String PERSIST_RECORDS = "persist.records";

    private static final String SERVER_INFO_FILE = "server.json";

    private static final String ENGINE_API_DIR = "engine_api";

    /**
     * Remove old results at 30 minutes past midnight
     */
    private static final long OLD_RESULTS_REMOVAL_PAST_MIDNIGHT_OFFSET_MINUTES = 30L;

    private Client client;
    private JobProvider jobProvider;
    private volatile JobManager jobManager;

    //This isn't set in the ctor because doing so creates a guice circular
    @Inject(optional=true)
    public void setClient(Client client) {
        this.client = client;
    }

    public JobProvider getJobProvider() {
        initializeIfNeeded();
        return jobProvider;
    }

    public JobManager getJobManager() {
        initializeIfNeeded();
        return jobManager;
    }

    private void initializeIfNeeded() {
        if (jobManager == null) {
            synchronized (this) {
                ElasticsearchFactory esFactory = createPersistenceFactory(client);
                jobProvider = esFactory.newJobProvider();
                ActionGuardian<Action> processActionGuardian =
                        new LocalActionGuardian<>(Action.startingState());
                ActionGuardian<ScheduledAction> schedulerActionGuardian =
                        new LocalActionGuardian<>(ScheduledAction.STOPPED);
                this.jobManager = new JobManager(jobProvider,
                        processActionGuardian, schedulerActionGuardian);
            }
        }
    }

    private static ElasticsearchFactory createPersistenceFactory(Client client) {

        return new ElasticsearchFactory(client) {

            @Override
            public JobProvider newJobProvider() {
                return new ElasticsearchJobProvider(null, client, numberOfReplicas());
            }
        };
    }

    private static int numberOfReplicas() {
        return 0;
    }
}