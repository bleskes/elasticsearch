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

import org.elasticsearch.cluster.ClusterChangedEvent;
import org.elasticsearch.cluster.ClusterStateListener;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.SchedulerState;
import org.elasticsearch.xpack.prelert.job.manager.JobScheduledService;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;

public class JobLifeCycleService extends AbstractComponent implements ClusterStateListener {

    volatile Set<String> localAllocatedJobs = Collections.emptySet();
    private final JobScheduledService jobScheduledService;
    private final Executor executor;

    public JobLifeCycleService(Settings settings, ClusterService clusterService, JobScheduledService jobScheduledService,
            Executor executor) {
        super(settings);
        clusterService.add(this);
        this.jobScheduledService = Objects.requireNonNull(jobScheduledService);
        this.executor = Objects.requireNonNull(executor);
    }

    @Override
    public void clusterChanged(ClusterChangedEvent event) {
        // Single volatile read:
        Set<String> localAllocatedJobs = this.localAllocatedJobs;

        PrelertMetadata prelertMetadata = event.state().getMetaData().custom(PrelertMetadata.TYPE);
        if (prelertMetadata == null) {
            // if no prelert metadata then stop any allocated jobs:
            for (String localAllocatedJob : localAllocatedJobs) {
                stopJob(localAllocatedJob);
            }
            return;
        }

        DiscoveryNode localNode = event.state().nodes().getLocalNode();
        for (Allocation allocation : prelertMetadata.getAllocations().values()) {
            if (localNode.getId().equals(allocation.getNodeId())) {
                handleLocallyAllocatedJob(prelertMetadata, allocation, event);
            }
        }

        for (String localAllocatedJob : localAllocatedJobs) {
            Allocation allocation = prelertMetadata.getAllocations().get(localAllocatedJob);
            if (allocation != null) {
                if (localNode.getId().equals(allocation.getNodeId()) == false) {
                    stopJob(localAllocatedJob);
                }
            } else {
                stopJob(localAllocatedJob);
            }
        }
    }

    private void handleLocallyAllocatedJob(PrelertMetadata prelertMetadata, Allocation allocation, ClusterChangedEvent event) {
        JobDetails job = prelertMetadata.getJobs().get(allocation.getJobId());
        if (localAllocatedJobs.contains(allocation.getJobId()) == false) {
            startJob(job);
        }

        SchedulerState schedulerState = job.getSchedulerState();
        if (schedulerState != null) {
            switch (schedulerState.getStatus()) {
            case STARTED:
                jobScheduledService.start(job);
                break;
            case STOPPING:
                executor.execute(() -> jobScheduledService.stop(job.getId()));
                break;
            case STOPPED:
                break;
            default:
                throw new IllegalStateException("Unhandled scheduler state [" + schedulerState.getStatus() + "]");
            }
        }
    }

    void startJob(JobDetails job) {
        logger.info("Starting job [" + job.getId() + "]");
        // noop now, but should delegate to a task / ProcessManager that actually starts the job

        // update which jobs are now allocated locally
        Set<String> newSet = new HashSet<>(localAllocatedJobs);
        newSet.add(job.getId());
        localAllocatedJobs = newSet;
    }

    void stopJob(String jobId) {
        logger.info("Stopping job [" + jobId + "]");
        // noop now, but should delegate to a task / ProcessManager that actually stops the job

        // update which jobs are now allocated locally
        Set<String> newSet = new HashSet<>(localAllocatedJobs);
        newSet.remove(jobId);
        localAllocatedJobs = newSet;
    }
}
