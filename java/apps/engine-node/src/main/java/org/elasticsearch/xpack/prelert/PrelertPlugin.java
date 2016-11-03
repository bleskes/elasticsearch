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
package org.elasticsearch.xpack.prelert;

import org.apache.lucene.util.Constants;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Setting.Property;
import org.elasticsearch.common.util.concurrent.EsExecutors;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.plugins.ActionPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.rest.RestHandler;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.search.SearchRequestParsers;
import org.elasticsearch.threadpool.ExecutorBuilder;
import org.elasticsearch.threadpool.FixedExecutorBuilder;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.watcher.ResourceWatcherService;
import org.elasticsearch.xpack.prelert.action.ClearPrelertAction;
import org.elasticsearch.xpack.prelert.action.CreateListAction;
import org.elasticsearch.xpack.prelert.action.DeleteJobAction;
import org.elasticsearch.xpack.prelert.action.DeleteModelSnapshotAction;
import org.elasticsearch.xpack.prelert.action.GetBucketAction;
import org.elasticsearch.xpack.prelert.action.GetBucketsAction;
import org.elasticsearch.xpack.prelert.action.GetCategoryDefinitionAction;
import org.elasticsearch.xpack.prelert.action.GetCategoryDefinitionsAction;
import org.elasticsearch.xpack.prelert.action.GetInfluencersAction;
import org.elasticsearch.xpack.prelert.action.GetJobAction;
import org.elasticsearch.xpack.prelert.action.GetJobsAction;
import org.elasticsearch.xpack.prelert.action.GetListAction;
import org.elasticsearch.xpack.prelert.action.GetModelSnapshotsAction;
import org.elasticsearch.xpack.prelert.action.GetRecordsAction;
import org.elasticsearch.xpack.prelert.action.PostDataAction;
import org.elasticsearch.xpack.prelert.action.PostDataCloseAction;
import org.elasticsearch.xpack.prelert.action.PostDataFlushAction;
import org.elasticsearch.xpack.prelert.action.PutJobAction;
import org.elasticsearch.xpack.prelert.action.PutModelSnapshotDescriptionAction;
import org.elasticsearch.xpack.prelert.action.RevertModelSnapshotAction;
import org.elasticsearch.xpack.prelert.action.StartJobSchedulerAction;
import org.elasticsearch.xpack.prelert.action.StopJobSchedulerAction;
import org.elasticsearch.xpack.prelert.action.ValidateDetectorAction;
import org.elasticsearch.xpack.prelert.action.ValidateTransformAction;
import org.elasticsearch.xpack.prelert.action.ValidateTransformsAction;
import org.elasticsearch.xpack.prelert.job.data.DataProcessor;
import org.elasticsearch.xpack.prelert.job.logs.JobLogs;
import org.elasticsearch.xpack.prelert.job.manager.AutodetectProcessManager;
import org.elasticsearch.xpack.prelert.job.manager.JobManager;
import org.elasticsearch.xpack.prelert.job.manager.JobScheduledService;
import org.elasticsearch.xpack.prelert.job.manager.actions.Action;
import org.elasticsearch.xpack.prelert.job.manager.actions.ActionGuardian;
import org.elasticsearch.xpack.prelert.job.manager.actions.LocalActionGuardian;
import org.elasticsearch.xpack.prelert.job.metadata.JobAllocator;
import org.elasticsearch.xpack.prelert.job.metadata.JobLifeCycleService;
import org.elasticsearch.xpack.prelert.job.metadata.PrelertMetadata;
import org.elasticsearch.xpack.prelert.job.persistence.ElasticsearchBulkDeleterFactory;
import org.elasticsearch.xpack.prelert.job.persistence.ElasticsearchFactories;
import org.elasticsearch.xpack.prelert.job.persistence.ElasticsearchJobProvider;
import org.elasticsearch.xpack.prelert.job.process.ProcessCtrl;
import org.elasticsearch.xpack.prelert.job.process.autodetect.AutodetectCommunicatorFactory;
import org.elasticsearch.xpack.prelert.job.process.autodetect.AutodetectProcessFactory;
import org.elasticsearch.xpack.prelert.job.process.autodetect.BlackHoleAutodetectProcess;
import org.elasticsearch.xpack.prelert.job.process.autodetect.NativeAutodetectProcessFactory;
import org.elasticsearch.xpack.prelert.job.scheduler.http.HttpDataExtractorFactory;
import org.elasticsearch.xpack.prelert.job.status.StatusReporter;
import org.elasticsearch.xpack.prelert.job.usage.UsageReporter;
import org.elasticsearch.xpack.prelert.rest.RestClearPrelertAction;
import org.elasticsearch.xpack.prelert.rest.data.RestPostDataAction;
import org.elasticsearch.xpack.prelert.rest.data.RestPostDataCloseAction;
import org.elasticsearch.xpack.prelert.rest.data.RestPostDataFlushAction;
import org.elasticsearch.xpack.prelert.rest.influencers.RestGetInfluencersAction;
import org.elasticsearch.xpack.prelert.rest.job.RestDeleteJobAction;
import org.elasticsearch.xpack.prelert.rest.job.RestGetJobAction;
import org.elasticsearch.xpack.prelert.rest.job.RestGetJobsAction;
import org.elasticsearch.xpack.prelert.rest.job.RestPutJobsAction;
import org.elasticsearch.xpack.prelert.rest.list.RestCreateListAction;
import org.elasticsearch.xpack.prelert.rest.list.RestGetListAction;
import org.elasticsearch.xpack.prelert.rest.modelsnapshots.RestDeleteModelSnapshotAction;
import org.elasticsearch.xpack.prelert.rest.modelsnapshots.RestGetModelSnapshotsAction;
import org.elasticsearch.xpack.prelert.rest.modelsnapshots.RestPutModelSnapshotDescriptionAction;
import org.elasticsearch.xpack.prelert.rest.modelsnapshots.RestRevertModelSnapshotAction;
import org.elasticsearch.xpack.prelert.rest.results.RestGetBucketAction;
import org.elasticsearch.xpack.prelert.rest.results.RestGetBucketsAction;
import org.elasticsearch.xpack.prelert.rest.results.RestGetCategoriesAction;
import org.elasticsearch.xpack.prelert.rest.results.RestGetCategoryAction;
import org.elasticsearch.xpack.prelert.rest.results.RestGetRecordsAction;
import org.elasticsearch.xpack.prelert.rest.schedulers.RestStartJobSchedulerAction;
import org.elasticsearch.xpack.prelert.rest.schedulers.RestStopJobSchedulerAction;
import org.elasticsearch.xpack.prelert.rest.validate.RestValidateDetectorAction;
import org.elasticsearch.xpack.prelert.rest.validate.RestValidateTransformAction;
import org.elasticsearch.xpack.prelert.rest.validate.RestValidateTransformsAction;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class PrelertPlugin extends Plugin implements ActionPlugin {

    public static final String NAME = "prelert";
    public static final String THREAD_POOL_NAME = NAME;

    // NORELEASE: eventually we won't run programs directly so won't need to know this in this class
    private static final String PLATFORM_NAME = makePlatformName();

    // NORELEASE - temporary solution
    static final Setting<Boolean> USE_NATIVE_PROCESS_OPTION = Setting.boolSetting("useNativeProcess", false, Property.NodeScope,
            Property.Deprecated);

    private final Settings settings;
    private final Environment env;

    static {
        MetaData.registerPrototype(PrelertMetadata.TYPE, PrelertMetadata.PROTO);
    }

    public PrelertPlugin(Settings settings) {
        this.settings = settings;
        this.env = new Environment(settings);
    }

    @Override
    public List<Setting<?>> getSettings() {
        return Collections.unmodifiableList(
                Arrays.asList(USE_NATIVE_PROCESS_OPTION,
                        JobLogs.DONT_DELETE_LOGS_SETTING,
                        ProcessCtrl.DONT_PERSIST_MODEL_STATE_SETTING,
                        ProcessCtrl.MAX_ANOMALY_RECORDS_SETTING,
                        StatusReporter.ACCEPTABLE_PERCENTAGE_DATE_PARSE_ERRORS_SETTING,
                        StatusReporter.ACCEPTABLE_PERCENTAGE_OUT_OF_ORDER_ERRORS_SETTING,
                        UsageReporter.UPDATE_INTERVAL_SETTING));
    }

    @Override
    public Collection<Object> createComponents(Client client, ClusterService clusterService, ThreadPool threadPool,
            ResourceWatcherService resourceWatcherService, ScriptService scriptService,
            SearchRequestParsers searchRequestParsers) {

        ActionGuardian<Action> processActionGuardian =
                new LocalActionGuardian<>(Action.startingState());
        // All components get binded in the guice context to the instances returned here
        // and interfaces are not bound to their concrete classes.
        // instead of `bind(Interface.class).to(Implementation.class);` this happens:
        //  `bind(Implementation.class).toInstance(INSTANCE);`
        // For this reason we can't use interfaces in the constructor of transport actions.
        // This ok for now as we will remove Guice soon
        ElasticsearchJobProvider jobProvider = new ElasticsearchJobProvider(null, client, 0, ParseFieldMatcher.STRICT);
        ElasticsearchFactories elasticsearchFactories = new ElasticsearchFactories(client);
        AutodetectCommunicatorFactory autodetectCommunicatorFactory =
                createAutodetectCommunicatorFactory(elasticsearchFactories, jobProvider);

        JobManager jobManager = new JobManager(env, settings, jobProvider, clusterService, processActionGuardian);
        DataProcessor dataProcessor = new AutodetectProcessManager(autodetectCommunicatorFactory, jobManager);
        JobScheduledService jobScheduledService = new JobScheduledService(
                jobProvider, jobManager, dataProcessor, new HttpDataExtractorFactory(), jobId -> Loggers.getLogger(jobId));
        return Arrays.asList(
                jobProvider,
                jobManager,
                new JobAllocator(settings, clusterService, threadPool),
                new JobLifeCycleService(settings, clusterService, jobScheduledService, threadPool.generic()),
                new ElasticsearchBulkDeleterFactory(client), //NORELEASE: this should use Delete-by-query
                dataProcessor
                );
    }

    private AutodetectCommunicatorFactory createAutodetectCommunicatorFactory(ElasticsearchFactories esFactory,
            ElasticsearchJobProvider jobProvider) {

        AutodetectProcessFactory processFactory = USE_NATIVE_PROCESS_OPTION.get(settings)
                ? new NativeAutodetectProcessFactory(jobProvider, env, settings)
                        : (JobDetails, ingnoreDowntime) -> new BlackHoleAutodetectProcess();
                        return new AutodetectCommunicatorFactory(env, settings,
                                processFactory,
                                esFactory.newJobResultsPersisterFactory(),
                                esFactory.newJobDataCountsPersisterFactory(),
                                esFactory.newUsagePersisterFactory(),
                                jobId -> Loggers.getLogger(jobId));
    }

    @Override
    public List<Class<? extends RestHandler>> getRestHandlers() {
        return Arrays.asList(
                RestGetJobAction.class,
                RestGetJobsAction.class,
                RestPutJobsAction.class,
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
                RestRevertModelSnapshotAction.class,
                RestPutModelSnapshotDescriptionAction.class,
                RestStartJobSchedulerAction.class,
                RestStopJobSchedulerAction.class,
                RestDeleteModelSnapshotAction.class);
    }

    @Override
    public List<ActionHandler<? extends ActionRequest<?>, ? extends ActionResponse>> getActions() {
        return Arrays.asList(
                new ActionHandler<>(GetJobAction.INSTANCE, GetJobAction.TransportAction.class),
                new ActionHandler<>(GetJobsAction.INSTANCE, GetJobsAction.TransportAction.class),
                new ActionHandler<>(PutJobAction.INSTANCE, PutJobAction.TransportAction.class),
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
                new ActionHandler<>(RevertModelSnapshotAction.INSTANCE, RevertModelSnapshotAction.TransportAction.class),
                new ActionHandler<>(PutModelSnapshotDescriptionAction.INSTANCE, PutModelSnapshotDescriptionAction.TransportAction.class),
                new ActionHandler<>(StartJobSchedulerAction.INSTANCE, StartJobSchedulerAction.TransportAction.class),
                new ActionHandler<>(StopJobSchedulerAction.INSTANCE, StopJobSchedulerAction.TransportAction.class),
                new ActionHandler<>(DeleteModelSnapshotAction.INSTANCE, DeleteModelSnapshotAction.TransportAction.class));
    }

    public static Path resolveConfigFile(Environment env, String name) {
        return env.configFile().resolve(NAME).resolve(name);
    }

    public static Path resolveLogFile(Environment env, String name) {
        return env.logsFile().resolve(NAME).resolve(name);
    }

    // NORELEASE: eventually we won't run programs directly
    public static Path resolveBinFile(Environment env, String name) {
        return env.pluginsFile().resolve(NAME).resolve("platform").resolve(PLATFORM_NAME).resolve("bin").resolve(name);
    }

    @Override
    public List<ExecutorBuilder<?>> getExecutorBuilders(Settings settings) {
        final FixedExecutorBuilder builder = new FixedExecutorBuilder(settings, THREAD_POOL_NAME,
                5 * EsExecutors.boundedNumberOfProcessors(settings), 1000, "xpack.prelert.thread_pool");
        return Collections.singletonList(builder);
    }

    /**
     * Make the platform name in the format used in Kibana downloads, for example:
     * - darwin-x86_64
     * - linux-x86-64
     * - windows-x86_64
     * For *nix platforms this is more-or-less `uname -s`-`uname -m` converted to lower case.
     * However, for consistency between different operating systems on the same architecture
     * "amd64" is replaced with "x86_64" and "i386" with "x86".
     * For Windows it's "windows-" followed by either "x86" or "x86_64".
     */
    static String makePlatformName() {
        String os = Constants.OS_NAME.toLowerCase(Locale.ROOT);
        if (os.startsWith("windows")) {
            os = "windows";
        } else if (os.equals("mac os x")) {
            os = "darwin";
        }
        String cpu = Constants.OS_ARCH.toLowerCase(Locale.ROOT);
        if (cpu.equals("amd64")) {
            cpu = "x86_64";
        } else if (cpu.equals("i386")) {
            cpu = "x86";
        }
        return os + "-" + cpu;
    }
}
