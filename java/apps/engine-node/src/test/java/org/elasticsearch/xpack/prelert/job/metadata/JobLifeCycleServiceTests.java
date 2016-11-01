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

import org.elasticsearch.Version;
import org.elasticsearch.cluster.ClusterChangedEvent;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.LocalTransportAddress;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.JobConfiguration;
import org.elasticsearch.xpack.prelert.job.manager.JobScheduledService;
import org.junit.Before;
import org.mockito.Mockito;

import java.util.HashSet;

public class JobLifeCycleServiceTests extends ESTestCase {

    private ClusterService clusterService;
    private JobScheduledService jobScheduledService;
    private JobLifeCycleService jobLifeCycleService;

    @Before
    public void instantiateJobAllocator() {
        clusterService = Mockito.mock(ClusterService.class);
        jobScheduledService = Mockito.mock(JobScheduledService.class);
        jobLifeCycleService = new JobLifeCycleService(Settings.EMPTY, clusterService, jobScheduledService, Runnable::run);
    }

    public void testStartStop() {
        jobLifeCycleService.startJob(new Job(new JobConfiguration("_job_id").build()));
        assertTrue(jobLifeCycleService.localAllocatedJobs.contains("_job_id"));
        jobLifeCycleService.stopJob("_job_id");
        assertTrue(jobLifeCycleService.localAllocatedJobs.isEmpty());
    }

    public void testClusterChanged() {
        PrelertMetadata.Builder pmBuilder = new PrelertMetadata.Builder();
        pmBuilder.putJob(new Job(new JobConfiguration("_job_id").build()), false);
        pmBuilder.putAllocation("_node_id", "_job_id");
        ClusterState cs1 = ClusterState.builder(new ClusterName("_cluster_name")).metaData(MetaData.builder()
                .putCustom(PrelertMetadata.TYPE, pmBuilder.build()))
                .nodes(DiscoveryNodes.builder()
                        .add(new DiscoveryNode("_node_id", new LocalTransportAddress("_id"), Version.CURRENT))
                        .localNodeId("_node_id"))
                .build();
        jobLifeCycleService.clusterChanged(new ClusterChangedEvent("_source", cs1, cs1));
        assertTrue("Expect allocation, because job allocation says _job_id should be allocated locally",
                jobLifeCycleService.localAllocatedJobs.contains("_job_id"));

        pmBuilder.removeJob("_job_id");
        ClusterState cs2 = ClusterState.builder(new ClusterName("_cluster_name")).metaData(MetaData.builder()
                .putCustom(PrelertMetadata.TYPE, pmBuilder.build()))
                .nodes(DiscoveryNodes.builder()
                        .add(new DiscoveryNode("_node_id", new LocalTransportAddress("_id"), Version.CURRENT))
                        .localNodeId("_node_id"))
                .build();
        jobLifeCycleService.clusterChanged(new ClusterChangedEvent("_source", cs2, cs1));
        assertFalse("Expect no allocation, because the job has been removed", jobLifeCycleService.localAllocatedJobs.contains("_job_id"));
    }

    public void testClusterChanged_prelertMetadataRemoved() {
        jobLifeCycleService.localAllocatedJobs = new HashSet<>(); // default to an empty set, which is readonly
        jobLifeCycleService.localAllocatedJobs.add("_job_id1");
        jobLifeCycleService.localAllocatedJobs.add("_job_id2");

        ClusterState cs1 = ClusterState.builder(new ClusterName("_cluster_name")).build();
        jobLifeCycleService.clusterChanged(new ClusterChangedEvent("_source", cs1, cs1));
        assertTrue("If prelert metadata gets removed then stop any running job", jobLifeCycleService.localAllocatedJobs.isEmpty());
    }

}
