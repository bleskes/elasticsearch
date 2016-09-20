

package org.elasticsearch.xpack.prelert;


        import com.prelert.job.logging.DefaultJobLoggerFactory;
        import com.prelert.job.logging.JobLoggerFactory;
        import com.prelert.job.manager.JobManager;
        import com.prelert.job.manager.actions.Action;
        import com.prelert.job.manager.actions.ActionGuardian;
        import com.prelert.job.manager.actions.LocalActionGuardian;
        import com.prelert.job.manager.actions.ScheduledAction;
        import com.prelert.job.password.PasswordManager;
        import com.prelert.job.persistence.JobProvider;
        import com.prelert.job.persistence.elasticsearch.ElasticsearchJobProvider;
        import com.prelert.job.process.ProcessCtrl;
        import com.prelert.job.process.autodetect.ProcessFactory;
        import com.prelert.job.process.autodetect.ProcessManager;
        import com.prelert.rs.data.extraction.DataExtractorFactoryImpl;
        import com.prelert.rs.persistence.ElasticsearchFactory;
        import com.prelert.rs.persistence.ElasticsearchTransportClientFactory;
        import com.prelert.settings.PrelertSettings;
        import org.elasticsearch.client.Client;
        import org.elasticsearch.common.inject.Inject;

        import java.io.File;
        import java.io.IOException;
        import java.security.NoSuchAlgorithmException;

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
    private volatile JobManager jobManager;

    //This isn't set in the ctor because doing so creates a guice circular
    @Inject(optional=true)
    public void setClient(Client client) {
        this.client = client;
    }

    public JobManager getJobManager() {
        initializeIfNeeded();
        return jobManager;
    }

    private void initializeIfNeeded() {
        if (jobManager == null) {
            synchronized (this) {
                ElasticsearchFactory esFactory = createPersistenceFactory(client);
                JobProvider jobProvider = esFactory.newJobProvider();
                ActionGuardian<Action> processActionGuardian =
                        new LocalActionGuardian<>(Action.startingState());
                ActionGuardian<ScheduledAction> schedulerActionGuardian =
                        new LocalActionGuardian<>(ScheduledAction.STOPPED);
                JobLoggerFactory jobLoggerFactory = new DefaultJobLoggerFactory(ProcessCtrl.LOG_DIR);
                PasswordManager passwordManager = createPasswordManager();
                this.jobManager = new JobManager(jobProvider,
                        createProcessManager(jobProvider, esFactory, jobLoggerFactory),
                        new DataExtractorFactoryImpl(passwordManager), jobLoggerFactory,
                        passwordManager, esFactory.newJobDataDeleterFactory(),
                        processActionGuardian, schedulerActionGuardian, false);
            }
        }
    }

    private static ElasticsearchFactory createPersistenceFactory(Client client) {
        String esHost = PrelertSettings.getSettingOrDefault(ProcessCtrl.ES_HOST_PROP, ProcessCtrl.DEFAULT_ES_HOST);
        String clusterName = PrelertSettings.getSettingOrDefault(ES_CLUSTER_NAME_PROP, DEFAULT_CLUSTER_NAME);
        String portRange = PrelertSettings.getSettingOrDefault(ES_TRANSPORT_PORT_RANGE, DEFAULT_ES_TRANSPORT_PORT_RANGE);

        String resultsStorageClient = PrelertSettings.getSettingOrDefault(RESULTS_STORAGE_CLIENT_PROP, ES_AUTO);
        // Treat any unknown values as though they were es-auto
        if (!(resultsStorageClient.equals(ES_TRANSPORT) || resultsStorageClient.equals(ES_NODE))) {
            // We deliberately DON'T try to detect when es.host is set to the
            // hostname of the current machine, as this scenario is taken to
            // mean that Elasticsearch is running on the current host but is
            // being managed independently of the Engine API
            if ("localhost".equals(esHost) || "localhost6".equals(esHost)) {
                resultsStorageClient = ES_NODE;
            } else {
                resultsStorageClient = ES_TRANSPORT;
            }
        }

        if (resultsStorageClient.equals(ES_TRANSPORT)) {
            String esHostAndPort = esHost + ":" + portRange.split("-", 2)[0];
            return ElasticsearchTransportClientFactory.create(esHostAndPort, clusterName);
        }

        return new ElasticsearchFactory(client) {

            @Override
            public JobProvider newJobProvider() {
                return new ElasticsearchJobProvider(null, client, numberOfReplicas());
            }
        };
    }

    private static ProcessManager createProcessManager(JobProvider jobProvider,
                                                       ElasticsearchFactory esFactory,
                                                       JobLoggerFactory jobLoggerFactory) {
        ProcessFactory processFactory = new ProcessFactory(
                jobProvider,
                esFactory.newResultsReaderFactory(jobProvider),
                esFactory.newJobDataCountsPersisterFactory(),
                esFactory.newUsagePersisterFactory(),
                jobLoggerFactory);
        return new ProcessManager(jobProvider, processFactory, esFactory.newDataPersisterFactory(),
                jobLoggerFactory);
    }

}