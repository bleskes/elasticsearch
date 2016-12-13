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
package org.elasticsearch.xpack.prelert.job.metadata;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.ClusterChangedEvent;
import org.elasticsearch.cluster.ClusterStateListener;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xpack.prelert.action.UpdateJobStatusAction;
import org.elasticsearch.xpack.prelert.job.JobStatus;
import org.elasticsearch.xpack.prelert.job.data.DataProcessor;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;

public class JobLifeCycleService extends AbstractComponent implements ClusterStateListener {

    volatile Set<String> localAssignedJobs = new HashSet<>();
    private final Client client;
    private final DataProcessor dataProcessor;
    private final Executor executor;

    public JobLifeCycleService(Settings settings, Client client, ClusterService clusterService, DataProcessor dataProcessor,
                               Executor executor) {
        super(settings);
        clusterService.add(this);
        this.client = Objects.requireNonNull(client);
        this.dataProcessor = Objects.requireNonNull(dataProcessor);
        this.executor = Objects.requireNonNull(executor);
    }

    @Override
    public void clusterChanged(ClusterChangedEvent event) {
        PrelertMetadata prelertMetadata = event.state().getMetaData().custom(PrelertMetadata.TYPE);
        if (prelertMetadata == null) {
            logger.debug("Prelert metadata not installed");
            return;
        }

        // Single volatile read:
        Set<String> localAssignedJobs = this.localAssignedJobs;

        DiscoveryNode localNode = event.state().nodes().getLocalNode();
        for (Allocation allocation : prelertMetadata.getAllocations().values()) {
            if (localNode.getId().equals(allocation.getNodeId())) {
                if (localAssignedJobs.contains(allocation.getJobId()) == false) {
                    if (allocation.getStatus() == JobStatus.OPENING) {
                        startJob(allocation);
                    }
                }
            }
        }

        for (String localAllocatedJob : localAssignedJobs) {
            Allocation allocation = prelertMetadata.getAllocations().get(localAllocatedJob);
            if (allocation != null) {
                if (localNode.getId().equals(allocation.getNodeId()) && allocation.getStatus() == JobStatus.CLOSING) {
                    stopJob(localAllocatedJob);
                }
            } else {
                stopJob(localAllocatedJob);
            }
        }
    }

    void startJob(Allocation allocation) {
        logger.info("Starting job [" + allocation.getJobId() + "]");
        executor.execute(() -> {
            try {
                dataProcessor.openJob(allocation.getJobId(), allocation.isIgnoreDowntime());
            } catch (Exception e) {
                logger.error("Failed to close job [" + allocation.getJobId() + "]", e);
                updateJobStatus(allocation.getJobId(), JobStatus.FAILED, "failed to open, " + e.getMessage());
            }
        });

        // update which jobs are now allocated locally
        Set<String> newSet = new HashSet<>(localAssignedJobs);
        newSet.add(allocation.getJobId());
        localAssignedJobs = newSet;
    }

    void stopJob(String jobId) {
        logger.info("Stopping job [" + jobId + "]");
        executor.execute(() -> {
            try {
                dataProcessor.closeJob(jobId);
            } catch (Exception e) {
                logger.error("Failed to close job [" + jobId + "]", e);
                updateJobStatus(jobId, JobStatus.FAILED, "failed to close, " + e.getMessage());
            }
        });

        // update which jobs are now allocated locally
        Set<String> newSet = new HashSet<>(localAssignedJobs);
        newSet.remove(jobId);
        localAssignedJobs = newSet;
    }

    private void updateJobStatus(String jobId, JobStatus status, String reason) {
        UpdateJobStatusAction.Request request = new UpdateJobStatusAction.Request(jobId, status);
        request.setReason(reason);
        client.execute(UpdateJobStatusAction.INSTANCE, request, new ActionListener<UpdateJobStatusAction.Response>() {
            @Override
            public void onResponse(UpdateJobStatusAction.Response response) {
                logger.info("Successfully set job status to [{}] for job [{}]", status, jobId);
            }

            @Override
            public void onFailure(Exception e) {
                logger.error("Could not set job status to [" + status + "] for job [" + jobId +"]", e);
            }
        });
    }
}
