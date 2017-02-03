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
package org.elasticsearch.xpack.ml.job;

import org.elasticsearch.ResourceAlreadyExistsException;
import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.AckedClusterStateUpdateTask;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.CheckedConsumer;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.xpack.ml.action.DeleteJobAction;
import org.elasticsearch.xpack.ml.action.PutJobAction;
import org.elasticsearch.xpack.ml.action.RevertModelSnapshotAction;
import org.elasticsearch.xpack.ml.action.UpdateJobStateAction;
import org.elasticsearch.xpack.ml.action.util.QueryPage;
import org.elasticsearch.xpack.ml.job.config.IgnoreDowntime;
import org.elasticsearch.xpack.ml.job.config.Job;
import org.elasticsearch.xpack.ml.job.config.JobState;
import org.elasticsearch.xpack.ml.job.messages.Messages;
import org.elasticsearch.xpack.ml.job.metadata.Allocation;
import org.elasticsearch.xpack.ml.job.metadata.MlMetadata;
import org.elasticsearch.xpack.ml.job.persistence.AnomalyDetectorsIndex;
import org.elasticsearch.xpack.ml.job.persistence.JobProvider;
import org.elasticsearch.xpack.ml.job.persistence.JobResultsPersister;
import org.elasticsearch.xpack.ml.job.process.autodetect.state.ModelSnapshot;
import org.elasticsearch.xpack.ml.job.results.AnomalyRecord;
import org.elasticsearch.xpack.ml.notifications.Auditor;
import org.elasticsearch.xpack.ml.utils.ExceptionsHelper;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Allows interactions with jobs. The managed interactions include:
 * <ul>
 * <li>creation</li>
 * <li>deletion</li>
 * <li>updating</li>
 * <li>starting/stopping of datafeed jobs</li>
 * </ul>
 */
public class JobManager extends AbstractComponent {

    /**
     * Field name in which to store the API version in the usage info
     */
    public static final String APP_VER_FIELDNAME = "appVer";

    public static final String DEFAULT_RECORD_SORT_FIELD = AnomalyRecord.PROBABILITY.getPreferredName();

    private final JobProvider jobProvider;
    private final ClusterService clusterService;
    private final JobResultsPersister jobResultsPersister;


    /**
     * Create a JobManager
     */
    public JobManager(Settings settings, JobProvider jobProvider, JobResultsPersister jobResultsPersister,
                      ClusterService clusterService) {
        super(settings);
        this.jobProvider = Objects.requireNonNull(jobProvider);
        this.clusterService = clusterService;
        this.jobResultsPersister = jobResultsPersister;
    }

    /**
     * Get the jobs that match the given {@code jobId}.
     * Note that when the {@code jocId} is {@link Job#ALL} all jobs are returned.
     *
     * @param jobId
     *            the jobId
     * @return A {@link QueryPage} containing the matching {@code Job}s
     */
    public QueryPage<Job> getJob(String jobId, ClusterState clusterState) {
        if (jobId.equals(Job.ALL)) {
            return getJobs(clusterState);
        }
        MlMetadata mlMetadata = clusterState.getMetaData().custom(MlMetadata.TYPE);
        Job job = mlMetadata.getJobs().get(jobId);
        if (job == null) {
            logger.debug(String.format(Locale.ROOT, "Cannot find job '%s'", jobId));
            throw ExceptionsHelper.missingJobException(jobId);
        }

        logger.debug("Returning job [" + jobId + "]");
        return new QueryPage<>(Collections.singletonList(job), 1, Job.RESULTS_FIELD);
    }

    /**
     * Get details of all Jobs.
     *
     * @return A query page object with hitCount set to the total number of jobs
     *         not the only the number returned here as determined by the
     *         <code>size</code> parameter.
     */
    public QueryPage<Job> getJobs(ClusterState clusterState) {
        MlMetadata mlMetadata = clusterState.getMetaData().custom(MlMetadata.TYPE);
        List<Job> jobs = mlMetadata.getJobs().entrySet().stream()
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        return new QueryPage<>(jobs, mlMetadata.getJobs().size(), Job.RESULTS_FIELD);
    }

    /**
     * Returns the non-null {@code Job} object for the given
     * {@code jobId} or throws
     * {@link org.elasticsearch.ResourceNotFoundException}
     *
     * @param jobId
     *            the jobId
     * @return the {@code Job} if a job with the given {@code jobId}
     *         exists
     * @throws org.elasticsearch.ResourceNotFoundException
     *             if there is no job with matching the given {@code jobId}
     */
    public Job getJobOrThrowIfUnknown(String jobId) {
        return getJobOrThrowIfUnknown(clusterService.state(), jobId);
    }

    public Allocation getJobAllocation(String jobId) {
        return getAllocation(clusterService.state(), jobId);
    }

    /**
     * Returns the non-null {@code Job} object for the given
     * {@code jobId} or throws
     * {@link org.elasticsearch.ResourceNotFoundException}
     *
     * @param jobId
     *            the jobId
     * @return the {@code Job} if a job with the given {@code jobId}
     *         exists
     * @throws org.elasticsearch.ResourceNotFoundException
     *             if there is no job with matching the given {@code jobId}
     */
    Job getJobOrThrowIfUnknown(ClusterState clusterState, String jobId) {
        MlMetadata mlMetadata = clusterState.metaData().custom(MlMetadata.TYPE);
        Job job = mlMetadata.getJobs().get(jobId);
        if (job == null) {
            throw ExceptionsHelper.missingJobException(jobId);
        }
        return job;
    }

    /**
     * Stores a job in the cluster state
     */
    public void putJob(PutJobAction.Request request, ActionListener<PutJobAction.Response> actionListener) {
        Job job = request.getJob();

        ActionListener<Boolean> createResultsIndexListener = ActionListener.wrap(jobSaved ->
                jobProvider.createJobResultIndex(job, new ActionListener<Boolean>() {
            @Override
            public void onResponse(Boolean indicesCreated) {
                audit(job.getId()).info(Messages.getMessage(Messages.JOB_AUDIT_CREATED));

                // Also I wonder if we need to audit log infra
                // structure in ml as when we merge into xpack
                // we can use its audit trailing. See:
                // https://github.com/elastic/prelert-legacy/issues/48
                actionListener.onResponse(new PutJobAction.Response(jobSaved && indicesCreated, job));
            }

            @Override
            public void onFailure(Exception e) {
                actionListener.onFailure(e);
            }
        }), actionListener::onFailure);

        clusterService.submitStateUpdateTask("put-job-" + job.getId(),
                new AckedClusterStateUpdateTask<Boolean>(request, createResultsIndexListener) {
            @Override
            protected Boolean newResponse(boolean acknowledged) {
                return acknowledged;
            }

            @Override
            public ClusterState execute(ClusterState currentState) throws Exception {
                ClusterState cs = updateClusterState(job, false, currentState);
                if (currentState.metaData().index(AnomalyDetectorsIndex.jobResultsIndexName(job.getIndexName())) != null) {
                    throw new ResourceAlreadyExistsException(Messages.getMessage(Messages.JOB_INDEX_ALREADY_EXISTS,
                            AnomalyDetectorsIndex.jobResultsIndexName(job.getIndexName())));
                }
                return cs;
            }
        });
    }

    ClusterState updateClusterState(Job job, boolean overwrite, ClusterState currentState) {
        MlMetadata.Builder builder = createMlMetadataBuilder(currentState);
        builder.putJob(job, overwrite);
        return buildNewClusterState(currentState, builder);
    }


    public void deleteJob(Client client, DeleteJobAction.Request request, ActionListener<DeleteJobAction.Response> actionListener) {
        String jobId = request.getJobId();
        String indexName = AnomalyDetectorsIndex.jobResultsIndexName(jobId);
        logger.debug("Deleting job '" + jobId + "'");

        // Step 3. Listen for the Cluster State job state change
        //         Chain acknowledged state onto original actionListener
        CheckedConsumer<Boolean, Exception> deleteStateConsumer =  jobDeleted -> {
            if (jobDeleted) {
                logger.info("Job [" + jobId + "] deleted.");
                actionListener.onResponse(new DeleteJobAction.Response(true));
                audit(jobId).info(Messages.getMessage(Messages.JOB_AUDIT_DELETED));
            } else {
                actionListener.onResponse(new DeleteJobAction.Response(false));
            }
        };


        // Step 2. Listen for the Deleted Index response
        //         If successful, delete from cluster state and chain onto deleteStateListener
        CheckedConsumer<Boolean, Exception> deleteIndexConsumer = response -> {
            clusterService.submitStateUpdateTask("delete-job-" + jobId,
                new AckedClusterStateUpdateTask<Boolean>(request, ActionListener.wrap(deleteStateConsumer, actionListener::onFailure)) {

                @Override
                protected Boolean newResponse(boolean acknowledged) {
                    return acknowledged && response;
                }

                @Override
                public ClusterState execute(ClusterState currentState) throws Exception {
                    return removeJobFromState(jobId, currentState);
                }
            });

        };

        // Step 1. Update the CS to DELETING
        //         If successful, attempt to delete the physical index and chain
        //         onto deleteIndexConsumer
        CheckedConsumer<UpdateJobStateAction.Response, Exception> updateConsumer = response -> {
            // Sucessfully updated the state to DELETING, begin actually deleting
            if (response.isAcknowledged()) {
                logger.info("Job [" + jobId + "] set to [" + JobState.DELETING + "]");
            } else {
                logger.warn("Job [" + jobId + "] change to [" + JobState.DELETING + "] was not acknowledged.");
            }

            DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(indexName);
            client.admin().indices().delete(deleteIndexRequest, ActionListener.wrap(deleteIndexResponse -> {
                logger.info("Deleting index [" + indexName + "] successful");

                if (deleteIndexResponse.isAcknowledged()) {
                    logger.info("Index deletion acknowledged");
                } else {
                    logger.warn("Index deletion not acknowledged");
                }
                deleteIndexConsumer.accept(deleteIndexResponse.isAcknowledged());
            }, e -> {
                if (e instanceof IndexNotFoundException) {
                    logger.warn("Physical index [" + indexName + "] not found. Continuing to delete job.");
                    try {
                        deleteIndexConsumer.accept(false);
                    } catch (Exception e1) {
                        actionListener.onFailure(e1);
                    }
                } else {
                    // all other exceptions should die
                    actionListener.onFailure(e);
                }
            }));
        };

        UpdateJobStateAction.Request updateStateListener = new UpdateJobStateAction.Request(jobId, JobState.DELETING);
        setJobState(updateStateListener, ActionListener.wrap(updateConsumer, actionListener::onFailure));

    }

    ClusterState removeJobFromState(String jobId, ClusterState currentState) {
        MlMetadata.Builder builder = createMlMetadataBuilder(currentState);
        builder.deleteJob(jobId);
        return buildNewClusterState(currentState, builder);
    }

    private Allocation getAllocation(ClusterState state, String jobId) {
        MlMetadata mlMetadata = state.metaData().custom(MlMetadata.TYPE);
        Allocation allocation = mlMetadata.getAllocations().get(jobId);
        if (allocation == null) {
            throw new ResourceNotFoundException("No allocation found for job with id [" + jobId + "]");
        }
        return allocation;
    }

    public Auditor audit(String jobId) {
        return jobProvider.audit(jobId);
    }

    public void revertSnapshot(RevertModelSnapshotAction.Request request, ActionListener<RevertModelSnapshotAction.Response> actionListener,
            ModelSnapshot modelSnapshot) {

        clusterService.submitStateUpdateTask("revert-snapshot-" + request.getJobId(),
                new AckedClusterStateUpdateTask<RevertModelSnapshotAction.Response>(request, actionListener) {

            @Override
            protected RevertModelSnapshotAction.Response newResponse(boolean acknowledged) {
                if (acknowledged) {
                    audit(request.getJobId())
                            .info(Messages.getMessage(Messages.JOB_AUDIT_REVERTED, modelSnapshot.getDescription()));
                    return new RevertModelSnapshotAction.Response(modelSnapshot);
                }
                throw new IllegalStateException("Could not revert modelSnapshot on job ["
                        + request.getJobId() + "], not acknowledged by master.");
            }

            @Override
            public ClusterState execute(ClusterState currentState) throws Exception {
                Job job = getJobOrThrowIfUnknown(currentState, request.getJobId());
                Job.Builder builder = new Job.Builder(job);
                builder.setModelSnapshotId(modelSnapshot.getSnapshotId());
                if (request.getDeleteInterveningResults()) {
                    builder.setIgnoreDowntime(IgnoreDowntime.NEVER);
                } else {
                    builder.setIgnoreDowntime(IgnoreDowntime.ONCE);
                }

                return updateClusterState(builder.build(), true, currentState);
            }
        });
    }

    public void setJobState(UpdateJobStateAction.Request request, ActionListener<UpdateJobStateAction.Response> actionListener) {
        clusterService.submitStateUpdateTask("set-job-state-" + request.getState() + "-" + request.getJobId(),
                new AckedClusterStateUpdateTask<UpdateJobStateAction.Response>(request, actionListener) {

                    @Override
                    public ClusterState execute(ClusterState currentState) throws Exception {
                        MlMetadata.Builder builder = new MlMetadata.Builder(currentState.metaData().custom(MlMetadata.TYPE));
                        builder.updateState(request.getJobId(), request.getState(), request.getReason());
                        return ClusterState.builder(currentState)
                                .metaData(MetaData.builder(currentState.metaData()).putCustom(MlMetadata.TYPE, builder.build()))
                                .build();
                    }

                    @Override
                    protected UpdateJobStateAction.Response newResponse(boolean acknowledged) {
                        return new UpdateJobStateAction.Response(acknowledged);
                    }
                });
    }

    /**
     * Update a persisted model snapshot metadata document to match the
     * argument supplied.
     *
     * @param modelSnapshot         the updated model snapshot object to be stored
     */
    public void updateModelSnapshot(ModelSnapshot modelSnapshot, Consumer<Boolean> handler, Consumer<Exception> errorHandler) {
        jobResultsPersister.updateModelSnapshot(modelSnapshot, handler, errorHandler);
    }

    private static MlMetadata.Builder createMlMetadataBuilder(ClusterState currentState) {
        MlMetadata currentMlMetadata = currentState.metaData().custom(MlMetadata.TYPE);
        return new MlMetadata.Builder(currentMlMetadata);
    }

    private static ClusterState buildNewClusterState(ClusterState currentState, MlMetadata.Builder builder) {
        ClusterState.Builder newState = ClusterState.builder(currentState);
        newState.metaData(MetaData.builder(currentState.getMetaData()).putCustom(MlMetadata.TYPE, builder.build()).build());
        return newState.build();
    }

}
