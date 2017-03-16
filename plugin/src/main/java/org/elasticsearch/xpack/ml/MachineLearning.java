/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
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
package org.elasticsearch.xpack.ml;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.cluster.NamedDiff;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.IndexScopedSettings;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Setting.Property;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsFilter;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.env.Environment;
import org.elasticsearch.license.XPackLicenseState;
import org.elasticsearch.plugins.ActionPlugin;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestHandler;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.threadpool.ExecutorBuilder;
import org.elasticsearch.threadpool.FixedExecutorBuilder;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.watcher.ResourceWatcherService;
import org.elasticsearch.xpack.XPackPlugin;
import org.elasticsearch.xpack.XPackSettings;
import org.elasticsearch.xpack.ml.action.CloseJobAction;
import org.elasticsearch.xpack.ml.action.DeleteDatafeedAction;
import org.elasticsearch.xpack.ml.action.DeleteExpiredDataAction;
import org.elasticsearch.xpack.ml.action.DeleteFilterAction;
import org.elasticsearch.xpack.ml.action.DeleteJobAction;
import org.elasticsearch.xpack.ml.action.DeleteModelSnapshotAction;
import org.elasticsearch.xpack.ml.action.FinalizeJobExecutionAction;
import org.elasticsearch.xpack.ml.action.FlushJobAction;
import org.elasticsearch.xpack.ml.action.GetBucketsAction;
import org.elasticsearch.xpack.ml.action.GetCategoriesAction;
import org.elasticsearch.xpack.ml.action.GetDatafeedsAction;
import org.elasticsearch.xpack.ml.action.GetDatafeedsStatsAction;
import org.elasticsearch.xpack.ml.action.GetFiltersAction;
import org.elasticsearch.xpack.ml.action.GetInfluencersAction;
import org.elasticsearch.xpack.ml.action.GetJobsAction;
import org.elasticsearch.xpack.ml.action.GetJobsStatsAction;
import org.elasticsearch.xpack.ml.action.GetModelSnapshotsAction;
import org.elasticsearch.xpack.ml.action.GetRecordsAction;
import org.elasticsearch.xpack.ml.action.MlDeleteByQueryAction;
import org.elasticsearch.xpack.ml.action.OpenJobAction;
import org.elasticsearch.xpack.ml.action.PostDataAction;
import org.elasticsearch.xpack.ml.action.PreviewDatafeedAction;
import org.elasticsearch.xpack.ml.action.PutDatafeedAction;
import org.elasticsearch.xpack.ml.action.PutFilterAction;
import org.elasticsearch.xpack.ml.action.PutJobAction;
import org.elasticsearch.xpack.ml.action.RevertModelSnapshotAction;
import org.elasticsearch.xpack.ml.action.StartDatafeedAction;
import org.elasticsearch.xpack.ml.action.StopDatafeedAction;
import org.elasticsearch.xpack.ml.action.UpdateDatafeedAction;
import org.elasticsearch.xpack.ml.action.UpdateJobAction;
import org.elasticsearch.xpack.ml.action.UpdateModelSnapshotAction;
import org.elasticsearch.xpack.ml.action.UpdateProcessAction;
import org.elasticsearch.xpack.ml.action.ValidateDetectorAction;
import org.elasticsearch.xpack.ml.action.ValidateJobConfigAction;
import org.elasticsearch.xpack.ml.datafeed.DatafeedJobRunner;
import org.elasticsearch.xpack.ml.datafeed.DatafeedState;
import org.elasticsearch.xpack.ml.job.JobManager;
import org.elasticsearch.xpack.ml.job.UpdateJobProcessNotifier;
import org.elasticsearch.xpack.ml.job.config.JobState;
import org.elasticsearch.xpack.ml.job.persistence.JobDataCountsPersister;
import org.elasticsearch.xpack.ml.job.persistence.JobProvider;
import org.elasticsearch.xpack.ml.job.persistence.JobResultsPersister;
import org.elasticsearch.xpack.ml.job.process.DataCountsReporter;
import org.elasticsearch.xpack.ml.job.process.NativeController;
import org.elasticsearch.xpack.ml.job.process.NativeControllerHolder;
import org.elasticsearch.xpack.ml.job.process.ProcessCtrl;
import org.elasticsearch.xpack.ml.job.process.autodetect.AutodetectProcessFactory;
import org.elasticsearch.xpack.ml.job.process.autodetect.AutodetectProcessManager;
import org.elasticsearch.xpack.ml.job.process.autodetect.BlackHoleAutodetectProcess;
import org.elasticsearch.xpack.ml.job.process.autodetect.NativeAutodetectProcessFactory;
import org.elasticsearch.xpack.ml.job.process.normalizer.MultiplyingNormalizerProcess;
import org.elasticsearch.xpack.ml.job.process.normalizer.NativeNormalizerProcessFactory;
import org.elasticsearch.xpack.ml.job.process.normalizer.NormalizerFactory;
import org.elasticsearch.xpack.ml.job.process.normalizer.NormalizerProcessFactory;
import org.elasticsearch.xpack.ml.notifications.Auditor;
import org.elasticsearch.xpack.ml.rest.RestDeleteExpiredDataAction;
import org.elasticsearch.xpack.ml.rest.datafeeds.RestDeleteDatafeedAction;
import org.elasticsearch.xpack.ml.rest.datafeeds.RestGetDatafeedStatsAction;
import org.elasticsearch.xpack.ml.rest.datafeeds.RestGetDatafeedsAction;
import org.elasticsearch.xpack.ml.rest.datafeeds.RestPreviewDatafeedAction;
import org.elasticsearch.xpack.ml.rest.datafeeds.RestPutDatafeedAction;
import org.elasticsearch.xpack.ml.rest.datafeeds.RestStartDatafeedAction;
import org.elasticsearch.xpack.ml.rest.datafeeds.RestStopDatafeedAction;
import org.elasticsearch.xpack.ml.rest.datafeeds.RestUpdateDatafeedAction;
import org.elasticsearch.xpack.ml.rest.filter.RestDeleteFilterAction;
import org.elasticsearch.xpack.ml.rest.filter.RestGetFiltersAction;
import org.elasticsearch.xpack.ml.rest.filter.RestPutFilterAction;
import org.elasticsearch.xpack.ml.rest.job.RestCloseJobAction;
import org.elasticsearch.xpack.ml.rest.job.RestDeleteJobAction;
import org.elasticsearch.xpack.ml.rest.job.RestFlushJobAction;
import org.elasticsearch.xpack.ml.rest.job.RestGetJobStatsAction;
import org.elasticsearch.xpack.ml.rest.job.RestGetJobsAction;
import org.elasticsearch.xpack.ml.rest.job.RestOpenJobAction;
import org.elasticsearch.xpack.ml.rest.job.RestPostDataAction;
import org.elasticsearch.xpack.ml.rest.job.RestPostJobUpdateAction;
import org.elasticsearch.xpack.ml.rest.job.RestPutJobAction;
import org.elasticsearch.xpack.ml.rest.modelsnapshots.RestDeleteModelSnapshotAction;
import org.elasticsearch.xpack.ml.rest.modelsnapshots.RestGetModelSnapshotsAction;
import org.elasticsearch.xpack.ml.rest.modelsnapshots.RestRevertModelSnapshotAction;
import org.elasticsearch.xpack.ml.rest.modelsnapshots.RestUpdateModelSnapshotAction;
import org.elasticsearch.xpack.ml.rest.results.RestGetBucketsAction;
import org.elasticsearch.xpack.ml.rest.results.RestGetCategoriesAction;
import org.elasticsearch.xpack.ml.rest.results.RestGetInfluencersAction;
import org.elasticsearch.xpack.ml.rest.results.RestGetRecordsAction;
import org.elasticsearch.xpack.ml.rest.validate.RestValidateDetectorAction;
import org.elasticsearch.xpack.ml.rest.validate.RestValidateJobConfigAction;
import org.elasticsearch.xpack.persistent.CompletionPersistentTaskAction;
import org.elasticsearch.xpack.persistent.CreatePersistentTaskAction;
import org.elasticsearch.xpack.persistent.PersistentTaskRequest;
import org.elasticsearch.xpack.persistent.PersistentTasksClusterService;
import org.elasticsearch.xpack.persistent.PersistentTasksCustomMetaData;
import org.elasticsearch.xpack.persistent.PersistentTasksExecutorRegistry;
import org.elasticsearch.xpack.persistent.PersistentTasksNodeService;
import org.elasticsearch.xpack.persistent.PersistentTasksService;
import org.elasticsearch.xpack.persistent.RemovePersistentTaskAction;
import org.elasticsearch.xpack.persistent.StartPersistentTaskAction;
import org.elasticsearch.xpack.persistent.UpdatePersistentTaskStatusAction;
import org.elasticsearch.xpack.security.InternalClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;

public class MachineLearning implements ActionPlugin {
    public static final String NAME = "ml";
    public static final String BASE_PATH = "/_xpack/ml/";
    public static final String DATAFEED_THREAD_POOL_NAME = NAME + "_datafeed";
    public static final String AUTODETECT_THREAD_POOL_NAME = NAME + "_autodetect";
    public static final String NORMALIZER_THREAD_POOL_NAME = NAME + "_normalizer";

    public static final Setting<Boolean> AUTODETECT_PROCESS =
            Setting.boolSetting("xpack.ml.autodetect_process", true, Property.NodeScope);
    public static final Setting<Boolean> ML_ENABLED =
            Setting.boolSetting("node.ml", XPackSettings.MACHINE_LEARNING_ENABLED, Setting.Property.NodeScope);
    public static final Setting<Integer> CONCURRENT_JOB_ALLOCATIONS =
            Setting.intSetting("xpack.ml.node_concurrent_job_allocations", 2, 0, Property.Dynamic, Property.NodeScope);

    private final Settings settings;
    private final Environment env;
    private final XPackLicenseState licenseState;
    private final boolean enabled;
    private final boolean transportClientMode;
    private final boolean tribeNode;
    private final boolean tribeNodeClient;

    public MachineLearning(Settings settings, Environment env, XPackLicenseState licenseState) {
        this.settings = settings;
        this.env = env;
        this.licenseState = licenseState;
        this.enabled = XPackSettings.MACHINE_LEARNING_ENABLED.get(settings);
        this.transportClientMode = XPackPlugin.transportClientMode(settings);
        this.tribeNode = XPackPlugin.isTribeNode(settings);
        this.tribeNodeClient = XPackPlugin.isTribeClientNode(settings);
    }

    public List<Setting<?>> getSettings() {
        return Collections.unmodifiableList(
                Arrays.asList(AUTODETECT_PROCESS,
                        ML_ENABLED,
                        CONCURRENT_JOB_ALLOCATIONS,
                        ProcessCtrl.DONT_PERSIST_MODEL_STATE_SETTING,
                        ProcessCtrl.MAX_ANOMALY_RECORDS_SETTING,
                        DataCountsReporter.ACCEPTABLE_PERCENTAGE_DATE_PARSE_ERRORS_SETTING,
                        DataCountsReporter.ACCEPTABLE_PERCENTAGE_OUT_OF_ORDER_ERRORS_SETTING,
                        AutodetectProcessManager.MAX_RUNNING_JOBS_PER_NODE));
    }

    public Settings additionalSettings() {
        if (enabled == false || this.transportClientMode || this.tribeNode || this.tribeNodeClient) {
            return Settings.EMPTY;
        }

        Settings.Builder additionalSettings = Settings.builder();
        Boolean allocationEnabled = ML_ENABLED.get(settings);
        if (allocationEnabled != null && allocationEnabled) {
            // Copy max_running_jobs setting to node attribute, so that we use this information when assigning job tasks to nodes:
            additionalSettings.put("node.attr." + AutodetectProcessManager.MAX_RUNNING_JOBS_PER_NODE.getKey(),
                    AutodetectProcessManager.MAX_RUNNING_JOBS_PER_NODE.get(settings));
        }
        return additionalSettings.build();
    }

    public List<NamedWriteableRegistry.Entry> getNamedWriteables() {
        return Arrays.asList(
                // Custom metadata
                new NamedWriteableRegistry.Entry(MetaData.Custom.class, "ml", MlMetadata::new),
                new NamedWriteableRegistry.Entry(NamedDiff.class, "ml", MlMetadata.MlMetadataDiff::new),
                new NamedWriteableRegistry.Entry(MetaData.Custom.class, PersistentTasksCustomMetaData.TYPE,
                        PersistentTasksCustomMetaData::new),
                new NamedWriteableRegistry.Entry(NamedDiff.class, PersistentTasksCustomMetaData.TYPE,
                        PersistentTasksCustomMetaData::readDiffFrom),

                // Persistent action requests
                new NamedWriteableRegistry.Entry(PersistentTaskRequest.class, StartDatafeedAction.NAME, StartDatafeedAction.Request::new),
                new NamedWriteableRegistry.Entry(PersistentTaskRequest.class, OpenJobAction.NAME, OpenJobAction.Request::new),

                // Task statuses
                new NamedWriteableRegistry.Entry(Task.Status.class, PersistentTasksNodeService.Status.NAME,
                        PersistentTasksNodeService.Status::new),
                new NamedWriteableRegistry.Entry(Task.Status.class, JobState.NAME, JobState::fromStream),
                new NamedWriteableRegistry.Entry(Task.Status.class, DatafeedState.NAME, DatafeedState::fromStream)
        );
    }

    public List<NamedXContentRegistry.Entry> getNamedXContent() {
        return Arrays.asList(
                // Custom metadata
                new NamedXContentRegistry.Entry(MetaData.Custom.class, new ParseField("ml"),
                        parser -> MlMetadata.ML_METADATA_PARSER.parse(parser, null).build()),
                new NamedXContentRegistry.Entry(MetaData.Custom.class, new ParseField(PersistentTasksCustomMetaData.TYPE),
                        PersistentTasksCustomMetaData::fromXContent),

                // Persistent action requests
                new NamedXContentRegistry.Entry(PersistentTaskRequest.class, new ParseField(StartDatafeedAction.NAME),
                        StartDatafeedAction.Request::fromXContent),
                new NamedXContentRegistry.Entry(PersistentTaskRequest.class, new ParseField(OpenJobAction.NAME),
                        OpenJobAction.Request::fromXContent),

                // Task statuses
                new NamedXContentRegistry.Entry(Task.Status.class, new ParseField(DatafeedState.NAME), DatafeedState::fromXContent),
                new NamedXContentRegistry.Entry(Task.Status.class, new ParseField(JobState.NAME), JobState::fromXContent)
        );
    }

    public Collection<Object> createComponents(InternalClient internalClient, ClusterService clusterService, ThreadPool threadPool,
                                               ResourceWatcherService resourceWatcherService, ScriptService scriptService,
                                               NamedXContentRegistry xContentRegistry) {
        if (this.transportClientMode || this.tribeNodeClient) {
            return emptyList();
        }

        MlLifeCycleService mlLifeCycleService = new MlLifeCycleService(settings, clusterService);

        // Even when ML is disabled the native controller will be running if it's installed, and it
        // prevents graceful shutdown on Windows unless we tell it to stop.  Hence when disabled we
        // still return a lifecycle service that will tell the native controller to stop.
        if (false == enabled || this.tribeNode) {
            return Collections.singletonList(mlLifeCycleService);
        }

        JobResultsPersister jobResultsPersister = new JobResultsPersister(settings, internalClient);
        JobProvider jobProvider = new JobProvider(internalClient, settings);
        JobDataCountsPersister jobDataCountsPersister = new JobDataCountsPersister(settings, internalClient);

        Auditor auditor = new Auditor(internalClient, clusterService);
        UpdateJobProcessNotifier notifier = new UpdateJobProcessNotifier(settings, internalClient, clusterService, threadPool);
        JobManager jobManager = new JobManager(settings, jobProvider, jobResultsPersister, clusterService, auditor, internalClient, notifier);
        AutodetectProcessFactory autodetectProcessFactory;
        NormalizerProcessFactory normalizerProcessFactory;
        if (AUTODETECT_PROCESS.get(settings)) {
            try {
                NativeController nativeController = NativeControllerHolder.getNativeController(settings);
                if (nativeController == null) {
                    // This will only only happen when path.home is not set, which is disallowed in production
                    throw new ElasticsearchException("Failed to create native process controller for Machine Learning");
                }
                autodetectProcessFactory = new NativeAutodetectProcessFactory(jobProvider, env, settings, nativeController, internalClient);
                normalizerProcessFactory = new NativeNormalizerProcessFactory(env, settings, nativeController);
            } catch (IOException e) {
                // This also should not happen in production, as the MachineLearningFeatureSet should have
                // hit the same error first and brought down the node with a friendlier error message
                throw new ElasticsearchException("Failed to create native process factories for Machine Learning", e);
            }
        } else {
            autodetectProcessFactory = (jobDetails, modelSnapshot, quantiles, filters,
                                        ignoreDowntime, executorService, onProcessCrash) ->
                    new BlackHoleAutodetectProcess();
            // factor of 1.0 makes renormalization a no-op
            normalizerProcessFactory = (jobId, quantilesState, bucketSpan, perPartitionNormalization,
                                        executorService) -> new MultiplyingNormalizerProcess(settings, 1.0);
        }
        NormalizerFactory normalizerFactory = new NormalizerFactory(normalizerProcessFactory,
                threadPool.executor(MachineLearning.NORMALIZER_THREAD_POOL_NAME));
        AutodetectProcessManager autodetectProcessManager = new AutodetectProcessManager(settings, internalClient, threadPool,
                jobManager, jobProvider, jobResultsPersister, jobDataCountsPersister, autodetectProcessFactory,
                normalizerFactory, xContentRegistry);
        DatafeedJobRunner datafeedJobRunner = new DatafeedJobRunner(threadPool, internalClient, clusterService, jobProvider,
                System::currentTimeMillis, auditor);
        InvalidLicenseEnforcer invalidLicenseEnforcer =
                new InvalidLicenseEnforcer(settings, licenseState, threadPool, datafeedJobRunner, autodetectProcessManager);

        PersistentTasksService persistentTasksService = new PersistentTasksService(Settings.EMPTY, clusterService, internalClient);
        PersistentTasksExecutorRegistry persistentTasksExecutorRegistry = new PersistentTasksExecutorRegistry(Settings.EMPTY, Arrays.asList(
                new OpenJobAction.OpenJobPersistentTasksExecutor(settings, threadPool, licenseState, persistentTasksService, clusterService,
                        autodetectProcessManager, auditor),
                new StartDatafeedAction.StartDatafeedPersistentTasksExecutor(settings, threadPool, licenseState, persistentTasksService,
                        datafeedJobRunner, auditor)
        ));

        return Arrays.asList(
                mlLifeCycleService,
                jobProvider,
                jobManager,
                autodetectProcessManager,
                new MachineLearningTemplateRegistry(settings, clusterService, internalClient, threadPool),
                new MlInitializationService(settings, threadPool, clusterService, internalClient),
                jobDataCountsPersister,
                datafeedJobRunner,
                persistentTasksService,
                persistentTasksExecutorRegistry,
                new PersistentTasksClusterService(Settings.EMPTY, persistentTasksExecutorRegistry, clusterService),
                auditor,
                invalidLicenseEnforcer
        );
    }

    public Collection<Module> nodeModules() {
        List<Module> modules = new ArrayList<>();

        if (transportClientMode) {
            return modules;
        }

        modules.add(b -> {
            XPackPlugin.bindFeatureSet(b, MachineLearningFeatureSet.class);
        });

        return modules;
    }

    @Override
    public List<RestHandler> getRestHandlers(Settings settings, RestController restController, ClusterSettings clusterSettings,
                                             IndexScopedSettings indexScopedSettings, SettingsFilter settingsFilter,
                                             IndexNameExpressionResolver indexNameExpressionResolver,
                                             Supplier<DiscoveryNodes> nodesInCluster) {
        if (false == enabled || tribeNodeClient) {
            return emptyList();
        }
        return Arrays.asList(
            new RestGetJobsAction(settings, restController),
            new RestGetJobStatsAction(settings, restController),
            new RestPutJobAction(settings, restController),
            new RestPostJobUpdateAction(settings, restController),
            new RestDeleteJobAction(settings, restController),
            new RestOpenJobAction(settings, restController),
            new RestGetFiltersAction(settings, restController),
            new RestPutFilterAction(settings, restController),
            new RestDeleteFilterAction(settings, restController),
            new RestGetInfluencersAction(settings, restController),
            new RestGetRecordsAction(settings, restController),
            new RestGetBucketsAction(settings, restController),
            new RestPostDataAction(settings, restController),
            new RestCloseJobAction(settings, restController),
            new RestFlushJobAction(settings, restController),
            new RestValidateDetectorAction(settings, restController),
            new RestValidateJobConfigAction(settings, restController),
            new RestGetCategoriesAction(settings, restController),
            new RestGetModelSnapshotsAction(settings, restController),
            new RestRevertModelSnapshotAction(settings, restController),
            new RestUpdateModelSnapshotAction(settings, restController),
            new RestGetDatafeedsAction(settings, restController),
            new RestGetDatafeedStatsAction(settings, restController),
            new RestPutDatafeedAction(settings, restController),
            new RestUpdateDatafeedAction(settings, restController),
            new RestDeleteDatafeedAction(settings, restController),
            new RestPreviewDatafeedAction(settings, restController),
            new RestStartDatafeedAction(settings, restController),
            new RestStopDatafeedAction(settings, restController),
            new RestDeleteModelSnapshotAction(settings, restController),
            new RestDeleteExpiredDataAction(settings, restController)
        );
    }

    @Override
    public List<ActionHandler<? extends ActionRequest, ? extends ActionResponse>> getActions() {
        if (false == enabled) {
            return emptyList();
        }
        return Arrays.asList(
                new ActionHandler<>(GetJobsAction.INSTANCE, GetJobsAction.TransportAction.class),
                new ActionHandler<>(GetJobsStatsAction.INSTANCE, GetJobsStatsAction.TransportAction.class),
                new ActionHandler<>(PutJobAction.INSTANCE, PutJobAction.TransportAction.class),
                new ActionHandler<>(UpdateJobAction.INSTANCE, UpdateJobAction.TransportAction.class),
                new ActionHandler<>(DeleteJobAction.INSTANCE, DeleteJobAction.TransportAction.class),
                new ActionHandler<>(OpenJobAction.INSTANCE, OpenJobAction.TransportAction.class),
                new ActionHandler<>(GetFiltersAction.INSTANCE, GetFiltersAction.TransportAction.class),
                new ActionHandler<>(PutFilterAction.INSTANCE, PutFilterAction.TransportAction.class),
                new ActionHandler<>(DeleteFilterAction.INSTANCE, DeleteFilterAction.TransportAction.class),
                new ActionHandler<>(GetBucketsAction.INSTANCE, GetBucketsAction.TransportAction.class),
                new ActionHandler<>(GetInfluencersAction.INSTANCE, GetInfluencersAction.TransportAction.class),
                new ActionHandler<>(GetRecordsAction.INSTANCE, GetRecordsAction.TransportAction.class),
                new ActionHandler<>(PostDataAction.INSTANCE, PostDataAction.TransportAction.class),
                new ActionHandler<>(CloseJobAction.INSTANCE, CloseJobAction.TransportAction.class),
                new ActionHandler<>(FinalizeJobExecutionAction.INSTANCE, FinalizeJobExecutionAction.TransportAction.class),
                new ActionHandler<>(FlushJobAction.INSTANCE, FlushJobAction.TransportAction.class),
                new ActionHandler<>(ValidateDetectorAction.INSTANCE, ValidateDetectorAction.TransportAction.class),
                new ActionHandler<>(ValidateJobConfigAction.INSTANCE, ValidateJobConfigAction.TransportAction.class),
                new ActionHandler<>(GetCategoriesAction.INSTANCE, GetCategoriesAction.TransportAction.class),
                new ActionHandler<>(GetModelSnapshotsAction.INSTANCE, GetModelSnapshotsAction.TransportAction.class),
                new ActionHandler<>(RevertModelSnapshotAction.INSTANCE, RevertModelSnapshotAction.TransportAction.class),
                new ActionHandler<>(UpdateModelSnapshotAction.INSTANCE, UpdateModelSnapshotAction.TransportAction.class),
                new ActionHandler<>(GetDatafeedsAction.INSTANCE, GetDatafeedsAction.TransportAction.class),
                new ActionHandler<>(GetDatafeedsStatsAction.INSTANCE, GetDatafeedsStatsAction.TransportAction.class),
                new ActionHandler<>(PutDatafeedAction.INSTANCE, PutDatafeedAction.TransportAction.class),
                new ActionHandler<>(UpdateDatafeedAction.INSTANCE, UpdateDatafeedAction.TransportAction.class),
                new ActionHandler<>(DeleteDatafeedAction.INSTANCE, DeleteDatafeedAction.TransportAction.class),
                new ActionHandler<>(PreviewDatafeedAction.INSTANCE, PreviewDatafeedAction.TransportAction.class),
                new ActionHandler<>(StartDatafeedAction.INSTANCE, StartDatafeedAction.TransportAction.class),
                new ActionHandler<>(StopDatafeedAction.INSTANCE, StopDatafeedAction.TransportAction.class),
                new ActionHandler<>(DeleteModelSnapshotAction.INSTANCE, DeleteModelSnapshotAction.TransportAction.class),
                new ActionHandler<>(CreatePersistentTaskAction.INSTANCE, CreatePersistentTaskAction.TransportAction.class),
                new ActionHandler<>(StartPersistentTaskAction.INSTANCE, StartPersistentTaskAction.TransportAction.class),
                new ActionHandler<>(UpdatePersistentTaskStatusAction.INSTANCE, UpdatePersistentTaskStatusAction.TransportAction.class),
                new ActionHandler<>(CompletionPersistentTaskAction.INSTANCE, CompletionPersistentTaskAction.TransportAction.class),
                new ActionHandler<>(RemovePersistentTaskAction.INSTANCE, RemovePersistentTaskAction.TransportAction.class),
                new ActionHandler<>(MlDeleteByQueryAction.INSTANCE, MlDeleteByQueryAction.TransportAction.class),
                new ActionHandler<>(UpdateProcessAction.INSTANCE, UpdateProcessAction.TransportAction.class),
                new ActionHandler<>(DeleteExpiredDataAction.INSTANCE, DeleteExpiredDataAction.TransportAction.class)
        );
    }

    public List<ExecutorBuilder<?>> getExecutorBuilders(Settings settings) {
        if (false == enabled || tribeNode || tribeNodeClient) {
            return emptyList();
        }
        int maxNumberOfJobs = AutodetectProcessManager.MAX_RUNNING_JOBS_PER_NODE.get(settings);
        // 4 threads: for cpp logging, result processing, state processing and
        // AutodetectProcessManager worker thread:
        FixedExecutorBuilder autoDetect = new FixedExecutorBuilder(settings, AUTODETECT_THREAD_POOL_NAME,
                maxNumberOfJobs * 4, 4, "xpack.ml.autodetect_thread_pool");

        // 3 threads: normalization (cpp logging, result handling) and
        // renormalization (ShortCircuitingRenormalizer):
        FixedExecutorBuilder renormalizer = new FixedExecutorBuilder(settings, NORMALIZER_THREAD_POOL_NAME,
                maxNumberOfJobs * 3, 200, "xpack.ml.normalizer_thread_pool");

        // TODO: if datafeed and non datafeed jobs are considered more equal and the datafeed and
        // autodetect process are created at the same time then these two different TPs can merge.
        FixedExecutorBuilder datafeed = new FixedExecutorBuilder(settings, DATAFEED_THREAD_POOL_NAME,
                maxNumberOfJobs, 200, "xpack.ml.datafeed_thread_pool");
        return Arrays.asList(autoDetect, renormalizer, datafeed);
    }
}
