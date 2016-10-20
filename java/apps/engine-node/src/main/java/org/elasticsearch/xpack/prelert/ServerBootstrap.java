/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated. All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elasticsearch Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Elasticsearch Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elasticsearch Incorporated.
 */

package org.elasticsearch.xpack.prelert;

import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.bootstrap.JarHell;
import org.elasticsearch.cli.Terminal;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.internal.InternalSettingsPreparer;
import org.elasticsearch.plugins.ActionPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.rest.RestHandler;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.search.SearchRequestParsers;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.Netty4Plugin;
import org.elasticsearch.watcher.ResourceWatcherService;
import org.elasticsearch.xpack.prelert.action.*;
import org.elasticsearch.xpack.prelert.job.manager.JobManager;
import org.elasticsearch.xpack.prelert.job.manager.actions.Action;
import org.elasticsearch.xpack.prelert.job.manager.actions.ActionGuardian;
import org.elasticsearch.xpack.prelert.job.manager.actions.LocalActionGuardian;
import org.elasticsearch.xpack.prelert.job.manager.actions.ScheduledAction;
import org.elasticsearch.xpack.prelert.job.metadata.JobAllocator;
import org.elasticsearch.xpack.prelert.job.metadata.JobLifeCycleService;
import org.elasticsearch.xpack.prelert.job.metadata.PrelertMetadata;
import org.elasticsearch.xpack.prelert.job.persistence.ElasticsearchBulkDeleter;
import org.elasticsearch.xpack.prelert.job.persistence.ElasticsearchBulkDeleterFactory;
import org.elasticsearch.xpack.prelert.job.persistence.ElasticsearchJobProvider;
import org.elasticsearch.xpack.prelert.rest.RestClearPrelertAction;
import org.elasticsearch.xpack.prelert.rest.data.RestPostDataAction;
import org.elasticsearch.xpack.prelert.rest.data.RestPostDataCloseAction;
import org.elasticsearch.xpack.prelert.rest.data.RestPostDataFlushAction;
import org.elasticsearch.xpack.prelert.rest.influencers.RestGetInfluencersAction;
import org.elasticsearch.xpack.prelert.rest.job.*;
import org.elasticsearch.xpack.prelert.rest.list.RestCreateListAction;
import org.elasticsearch.xpack.prelert.rest.list.RestGetListAction;
import org.elasticsearch.xpack.prelert.rest.modelsnapshots.RestGetModelSnapshotsAction;
import org.elasticsearch.xpack.prelert.rest.modelsnapshots.RestRevertModelSnapshotsAction;
import org.elasticsearch.xpack.prelert.rest.results.*;
import org.elasticsearch.xpack.prelert.rest.validate.RestValidateDetectorAction;
import org.elasticsearch.xpack.prelert.rest.validate.RestValidateTransformAction;
import org.elasticsearch.xpack.prelert.rest.validate.RestValidateTransformsAction;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;

public class ServerBootstrap {

    private static final String JETTY_PORT_PROPERTY = "jetty.port";
    private static final String JETTY_HOME_PROPERTY = "jetty.home";
    private static final String DEFAULT_JETTY_HOME = "cots/jetty";

    public static final int JETTY_PORT = 8080;

    public static void main(String[] args) throws Exception {
        JarHell.checkJarHell();
        Settings.Builder settings = Settings.builder();
        settings.put("path.home", System.getProperty(JETTY_HOME_PROPERTY, DEFAULT_JETTY_HOME));
        settings.put("http.port", System.getProperty(JETTY_PORT_PROPERTY, Integer.toString(JETTY_PORT)));
        settings.put("cluster.name", "prelert");
        if (System.getProperty("network-host") != null) {
            settings.put("network.host", System.getProperty("network-host"));
        }

        CountDownLatch latch = new CountDownLatch(1);
        Node node = new PrelertNode(settings.build());
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                try {
                    node.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown();
                }
            }
        });
        node.start();
        // Verifies that we're good to go (at least yellow state and prelert-usage exists.
        // NORELEASE: when moving to be a plugin verifying prelert-usage index exists (and create one if it isn;t there)
        // should be triggered via a cluster state listener.
        node.injector().getInstance(ElasticsearchJobProvider.class).initialize();
        latch.await();
    }

    static class PrelertNode extends Node {

        public PrelertNode(Settings settings) {
            super(InternalSettingsPreparer.prepareEnvironment(settings, Terminal.DEFAULT),
                    Arrays.asList(PrelertPlugin.class, Netty4Plugin.class));
        }
    }

    public static class PrelertPlugin extends Plugin implements ActionPlugin {

        private final Settings settings;

        static {
            MetaData.registerPrototype(PrelertMetadata.TYPE, PrelertMetadata.PROTO);
        }

        public PrelertPlugin(Settings settings) {
            this.settings = settings;
        }

        @Override
        public Collection<Object> createComponents(Client client, ClusterService clusterService, ThreadPool threadPool,
                ResourceWatcherService resourceWatcherService, ScriptService scriptService,
                SearchRequestParsers searchRequestParsers) {

            ActionGuardian<Action> processActionGuardian =
                    new LocalActionGuardian<>(Action.startingState());
            ActionGuardian<ScheduledAction> schedulerActionGuardian =
                    new LocalActionGuardian<>(ScheduledAction.STOPPED);
            // All components get binded in the guice context to the instances returned here
            // and interfaces are not bound to their concrete classes.
            // instead of `bind(Interface.class).to(Implementation.class);` this happens:
            //  `bind(Implementation.class).toInstance(INSTANCE);`
            // For this reason we can't use interfaces in the constructor of transport actions.
            // This ok for now as we will remove Guice soon
            ElasticsearchJobProvider jobProvider = new ElasticsearchJobProvider(null, client, 0);
            return Arrays.asList(
                    jobProvider,
                    new JobManager(jobProvider, clusterService, processActionGuardian, schedulerActionGuardian),
                    new JobAllocator(settings, clusterService, threadPool),
                    new JobLifeCycleService(settings, clusterService),
                    new ElasticsearchBulkDeleterFactory(client) //NORELEASE: this should use Delete-by-query
            );
        }

        @Override
        public List<Class<? extends RestHandler>> getRestHandlers() {
            return Arrays.asList(
                    RestGetJobAction.class,
                    RestGetJobsAction.class,
                    RestPutJobsAction.class,
                    RestUpdateJobAction.class,
                    RestDeleteJobAction.class,
                    RestGetListAction.class,
                    RestCreateListAction.class,
                    RestGetBucketsAction.class,
                    RestGetInfluencersAction.class,
                    RestGetRecordsAction.class,
                    RestGetBucketAction.class,
                    RestPostDataAction.class,
                    RestPostDataCloseAction.class,
                    RestPostDataFlushAction.class,
                    RestValidateDetectorAction.class,
                    RestValidateTransformAction.class,
                    RestValidateTransformsAction.class,
                    RestClearPrelertAction.class,
                    RestGetCategoriesAction.class,
                    RestGetCategoryAction.class,
                    RestGetModelSnapshotsAction.class,
                    RestRevertModelSnapshotsAction.class);
        }

        @Override
        public List<ActionHandler<? extends ActionRequest<?>, ? extends ActionResponse>> getActions() {
            return Arrays.asList(
                    new ActionHandler<>(GetJobAction.INSTANCE, GetJobAction.TransportAction.class),
                    new ActionHandler<>(GetJobsAction.INSTANCE, GetJobsAction.TransportAction.class),
                    new ActionHandler<>(PutJobAction.INSTANCE, PutJobAction.TransportAction.class),
                    new ActionHandler<>(UpdateJobAction.INSTANCE, UpdateJobAction.TransportAction.class),
                    new ActionHandler<>(DeleteJobAction.INSTANCE, DeleteJobAction.TransportAction.class),
                    new ActionHandler<>(GetListAction.INSTANCE, GetListAction.TransportAction.class),
                    new ActionHandler<>(CreateListAction.INSTANCE, CreateListAction.TransportAction.class),
                    new ActionHandler<>(GetBucketsAction.INSTANCE, GetBucketsAction.TransportAction.class),
                    new ActionHandler<>(GetBucketAction.INSTANCE, GetBucketAction.TransportAction.class),
                    new ActionHandler<>(GetInfluencersAction.INSTANCE, GetInfluencersAction.TransportAction.class),
                    new ActionHandler<>(GetRecordsAction.INSTANCE, GetRecordsAction.TransportAction.class),
                    new ActionHandler<>(PostDataAction.INSTANCE, PostDataAction.TransportAction.class),
                    new ActionHandler<>(PostDataCloseAction.INSTANCE, PostDataCloseAction.TransportAction.class),
                    new ActionHandler<>(PostDataFlushAction.INSTANCE, PostDataFlushAction.TransportAction.class),
                    new ActionHandler<>(ValidateDetectorAction.INSTANCE, ValidateDetectorAction.TransportAction.class),
                    new ActionHandler<>(ValidateTransformAction.INSTANCE, ValidateTransformAction.TransportAction.class),
                    new ActionHandler<>(ValidateTransformsAction.INSTANCE, ValidateTransformsAction.TransportAction.class),
                    new ActionHandler<>(ClearPrelertAction.INSTANCE, ClearPrelertAction.TransportAction.class),
                    new ActionHandler<>(GetCategoryDefinitionsAction.INSTANCE, GetCategoryDefinitionsAction.TransportAction.class),
                    new ActionHandler<>(GetCategoryDefinitionAction.INSTANCE, GetCategoryDefinitionAction.TransportAction.class),
                    new ActionHandler<>(GetModelSnapshotsAction.INSTANCE, GetModelSnapshotsAction.TransportAction.class),
                    new ActionHandler<>(RevertModelSnapshotsAction.INSTANCE, RevertModelSnapshotsAction.TransportAction.class));
        }
    }
}
