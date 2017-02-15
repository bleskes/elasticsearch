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
package org.elasticsearch.xpack.ml.action;

import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.Version;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.ml.MachineLearning;
import org.elasticsearch.xpack.ml.job.config.Job;
import org.elasticsearch.xpack.ml.job.config.JobState;
import org.elasticsearch.xpack.ml.job.metadata.MlMetadata;
import org.elasticsearch.xpack.persistent.PersistentTasksInProgress;
import org.elasticsearch.xpack.persistent.PersistentTasksInProgress.PersistentTaskInProgress;

import java.net.InetAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.elasticsearch.xpack.ml.job.config.JobTests.buildJobBuilder;
import static org.elasticsearch.xpack.ml.job.process.autodetect.AutodetectProcessManager.MAX_RUNNING_JOBS_PER_NODE;

public class OpenJobActionTests extends ESTestCase {

    public void testValidate() {
        MlMetadata.Builder mlBuilder = new MlMetadata.Builder();
        mlBuilder.putJob(buildJobBuilder("job_id").build(), false);
        DiscoveryNodes nodes = DiscoveryNodes.builder()
                .add(new DiscoveryNode("_node_name", "_node_id", new InetSocketTransportAddress(InetAddress.getLoopbackAddress(), 9300),
                        Collections.emptyMap(), Collections.emptySet(), Version.CURRENT))
                .build();

        PersistentTaskInProgress<OpenJobAction.Request> task =
                new PersistentTaskInProgress<>(1L, OpenJobAction.NAME, new OpenJobAction.Request("job_id"), "_node_id");
        task = new PersistentTaskInProgress<>(task, randomFrom(JobState.CLOSED, JobState.FAILED));
        PersistentTasksInProgress tasks = new PersistentTasksInProgress(1L, Collections.singletonMap(1L, task));

        OpenJobAction.validate("job_id", mlBuilder.build(), tasks, nodes);
        OpenJobAction.validate("job_id", mlBuilder.build(), new PersistentTasksInProgress(1L, Collections.emptyMap()), nodes);
        OpenJobAction.validate("job_id", mlBuilder.build(), null, nodes);

        task = new PersistentTaskInProgress<>(1L, OpenJobAction.NAME, new OpenJobAction.Request("job_id"), "_other_node_id");
        task = new PersistentTaskInProgress<>(task, JobState.OPENED);
        tasks = new PersistentTasksInProgress(1L, Collections.singletonMap(1L, task));
        OpenJobAction.validate("job_id", mlBuilder.build(), tasks, nodes);
    }

    public void testValidate_jobMissing() {
        MlMetadata.Builder mlBuilder = new MlMetadata.Builder();
        mlBuilder.putJob(buildJobBuilder("job_id1").build(), false);
        expectThrows(ResourceNotFoundException.class, () -> OpenJobAction.validate("job_id2", mlBuilder.build(), null, null));
    }

    public void testValidate_jobMarkedAsDeleted() {
        MlMetadata.Builder mlBuilder = new MlMetadata.Builder();
        Job.Builder jobBuilder = buildJobBuilder("job_id");
        jobBuilder.setDeleted(true);
        mlBuilder.putJob(jobBuilder.build(), false);
        Exception e = expectThrows(ElasticsearchStatusException.class,
                () -> OpenJobAction.validate("job_id", mlBuilder.build(), null, null));
        assertEquals("Cannot open job [job_id] because it has been marked as deleted", e.getMessage());
    }

    public void testValidate_unexpectedState() {
        MlMetadata.Builder mlBuilder = new MlMetadata.Builder();
        mlBuilder.putJob(buildJobBuilder("job_id").build(), false);
        DiscoveryNodes nodes = DiscoveryNodes.builder()
                .add(new DiscoveryNode("_node_name", "_node_id", new InetSocketTransportAddress(InetAddress.getLoopbackAddress(), 9300),
                        Collections.emptyMap(), Collections.emptySet(), Version.CURRENT))
                .build();

        PersistentTaskInProgress<OpenJobAction.Request> task =
                new PersistentTaskInProgress<>(1L, OpenJobAction.NAME, new OpenJobAction.Request("job_id"), "_node_id");
        JobState jobState = randomFrom(JobState.OPENING, JobState.OPENED, JobState.CLOSING);
        task = new PersistentTaskInProgress<>(task, jobState);
        PersistentTasksInProgress tasks1 = new PersistentTasksInProgress(1L, Collections.singletonMap(1L, task));

        Exception e = expectThrows(ElasticsearchStatusException.class,
                () -> OpenJobAction.validate("job_id", mlBuilder.build(), tasks1, nodes));
        assertEquals("[job_id] expected state [closed] or [failed], but got [" + jobState +"]", e.getMessage());

        task = new PersistentTaskInProgress<>(1L, OpenJobAction.NAME, new OpenJobAction.Request("job_id"), "_other_node_id");
        jobState = randomFrom(JobState.OPENING, JobState.CLOSING);
        task = new PersistentTaskInProgress<>(task, jobState);
        PersistentTasksInProgress tasks2 = new PersistentTasksInProgress(1L, Collections.singletonMap(1L, task));

        e = expectThrows(ElasticsearchStatusException.class,
                () -> OpenJobAction.validate("job_id", mlBuilder.build(), tasks2, nodes));
        assertEquals("[job_id] expected state [closed] or [failed], but got [" + jobState +"]", e.getMessage());
    }

    public void testSelectLeastLoadedMlNode() {
        Map<String, String> nodeAttr = new HashMap<>();
        nodeAttr.put(MachineLearning.ALLOCATION_ENABLED_ATTR, "true");
        nodeAttr.put(MAX_RUNNING_JOBS_PER_NODE.getKey(), "10");
        DiscoveryNodes nodes = DiscoveryNodes.builder()
                .add(new DiscoveryNode("_node_name1", "_node_id1", new InetSocketTransportAddress(InetAddress.getLoopbackAddress(), 9300),
                        nodeAttr, Collections.emptySet(), Version.CURRENT))
                .add(new DiscoveryNode("_node_name2", "_node_id2", new InetSocketTransportAddress(InetAddress.getLoopbackAddress(), 9301),
                        nodeAttr, Collections.emptySet(), Version.CURRENT))
                .add(new DiscoveryNode("_node_name3", "_node_id3", new InetSocketTransportAddress(InetAddress.getLoopbackAddress(), 9302),
                        nodeAttr, Collections.emptySet(), Version.CURRENT))
                .build();

        Map<Long, PersistentTaskInProgress<?>> taskMap = new HashMap<>();
        taskMap.put(0L, new PersistentTaskInProgress<>(0L, OpenJobAction.NAME, new OpenJobAction.Request("job_id1"), "_node_id1"));
        taskMap.put(1L, new PersistentTaskInProgress<>(1L, OpenJobAction.NAME, new OpenJobAction.Request("job_id2"), "_node_id1"));
        taskMap.put(2L, new PersistentTaskInProgress<>(2L, OpenJobAction.NAME, new OpenJobAction.Request("job_id3"), "_node_id2"));
        PersistentTasksInProgress tasks = new PersistentTasksInProgress(3L, taskMap);

        ClusterState.Builder cs = ClusterState.builder(new ClusterName("_name"));
        cs.nodes(nodes);
        cs.putCustom(PersistentTasksInProgress.TYPE, tasks);
        DiscoveryNode result = OpenJobAction.selectLeastLoadedMlNode("job_id4", cs.build(), logger);
        assertEquals("_node_id3", result.getId());
    }

    public void testSelectLeastLoadedMlNode_maxCapacity() {
        int numNodes = randomIntBetween(1, 10);
        int maxRunningJobsPerNode = randomIntBetween(1, 100);

        Map<String, String> nodeAttr = new HashMap<>();
        nodeAttr.put(MachineLearning.ALLOCATION_ENABLED_ATTR, "true");
        nodeAttr.put(MAX_RUNNING_JOBS_PER_NODE.getKey(), String.valueOf(maxRunningJobsPerNode));
        DiscoveryNodes.Builder nodes = DiscoveryNodes.builder();
        Map<Long, PersistentTaskInProgress<?>> taskMap = new HashMap<>();
        for (int i = 0; i < numNodes; i++) {
            String nodeId = "_node_id" + i;
            InetSocketTransportAddress address = new InetSocketTransportAddress(InetAddress.getLoopbackAddress(), 9300 + i);
            nodes.add(new DiscoveryNode("_node_name" + i, nodeId, address, nodeAttr, Collections.emptySet(), Version.CURRENT));
            for (int j = 0; j < maxRunningJobsPerNode; j++) {
                long id = j + (maxRunningJobsPerNode * i);
                taskMap.put(id, new PersistentTaskInProgress<>(id, OpenJobAction.NAME, new OpenJobAction.Request("job_id" + id), nodeId));
            }
        }
        PersistentTasksInProgress tasks = new PersistentTasksInProgress(numNodes * maxRunningJobsPerNode, taskMap);

        ClusterState.Builder cs = ClusterState.builder(new ClusterName("_name"));
        cs.nodes(nodes);
        cs.putCustom(PersistentTasksInProgress.TYPE, tasks);
        DiscoveryNode result = OpenJobAction.selectLeastLoadedMlNode("job_id2", cs.build(), logger);
        assertNull(result);
    }

    public void testSelectLeastLoadedMlNode_noMlNodes() {
        Map<String, String> nodeAttr = new HashMap<>();
        nodeAttr.put(MachineLearning.ALLOCATION_ENABLED_ATTR, "false");
        nodeAttr.put(MAX_RUNNING_JOBS_PER_NODE.getKey(), "10");
        DiscoveryNodes nodes = DiscoveryNodes.builder()
                .add(new DiscoveryNode("_node_name1", "_node_id1", new InetSocketTransportAddress(InetAddress.getLoopbackAddress(), 9300),
                        nodeAttr, Collections.emptySet(), Version.CURRENT))
                .add(new DiscoveryNode("_node_name2", "_node_id2", new InetSocketTransportAddress(InetAddress.getLoopbackAddress(), 9301),
                        nodeAttr, Collections.emptySet(), Version.CURRENT))
                .build();

        PersistentTaskInProgress<OpenJobAction.Request> task =
                new PersistentTaskInProgress<>(1L, OpenJobAction.NAME, new OpenJobAction.Request("job_id1"), "_node_id1");
        PersistentTasksInProgress tasks = new PersistentTasksInProgress(1L, Collections.singletonMap(1L, task));

        ClusterState.Builder cs = ClusterState.builder(new ClusterName("_name"));
        cs.nodes(nodes);
        cs.putCustom(PersistentTasksInProgress.TYPE, tasks);
        DiscoveryNode result = OpenJobAction.selectLeastLoadedMlNode("job_id2", cs.build(), logger);
        assertNull(result);
    }

}
