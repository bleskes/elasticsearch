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
package org.elasticsearch.xpack.prelert.job.metadata;

import org.elasticsearch.cluster.ClusterChangedEvent;
import org.elasticsearch.cluster.ClusterStateListener;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.settings.Settings;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class JobLifeCycleService extends AbstractComponent implements ClusterStateListener {

    volatile Set<String> localAllocatedJobs = Collections.emptySet();

    public JobLifeCycleService(Settings settings, ClusterService clusterService) {
        super(settings);
        clusterService.add(this);
    }

    void startJob(Job job) {
        logger.info("Starting job [" + job.getJobDetails().getId() + "]");
        // noop now, but should delegate to a task / ProcessManager that actually starts the job

        // update which jobs are now allocated locally
        Set<String> newSet = new HashSet<>(localAllocatedJobs);
        newSet.add(job.getJobDetails().getId());
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
            if (localNode.getId().equals(allocation.getNodeId()) &&
                    localAllocatedJobs.contains(allocation.getJobId()) == false) {
                startJob(prelertMetadata.getJobs().get(allocation.getJobId()));
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
}
