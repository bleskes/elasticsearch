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
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.cluster.routing.IndexRoutingTable;
import org.elasticsearch.cluster.routing.IndexShardRoutingTable;
import org.elasticsearch.cluster.routing.RecoverySource;
import org.elasticsearch.cluster.routing.RoutingTable;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.cluster.routing.UnassignedInfo;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.ml.MachineLearning;
import org.elasticsearch.xpack.ml.MlMetadata;
import org.elasticsearch.xpack.ml.job.config.Job;
import org.elasticsearch.xpack.ml.job.config.JobState;
import org.elasticsearch.xpack.ml.job.config.JobTaskStatus;
import org.elasticsearch.xpack.ml.job.persistence.AnomalyDetectorsIndex;
import org.elasticsearch.xpack.ml.notifications.Auditor;
import org.elasticsearch.xpack.ml.support.BaseMlIntegTestCase;
import org.elasticsearch.xpack.persistent.PersistentTasksCustomMetaData;
import org.elasticsearch.xpack.persistent.PersistentTasksCustomMetaData.Assignment;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.xpack.ml.job.config.JobTests.buildJobBuilder;

public class OpenJobActionTests extends ESTestCase {

    public void testValidate_jobMissing() {
        MlMetadata.Builder mlBuilder = new MlMetadata.Builder();
        mlBuilder.putJob(buildJobBuilder("job_id1").build(), false);
        expectThrows(ResourceNotFoundException.class, () -> OpenJobAction.validate("job_id2", mlBuilder.build()));
    }

    public void testValidate_jobMarkedAsDeleted() {
        MlMetadata.Builder mlBuilder = new MlMetadata.Builder();
        Job.Builder jobBuilder = buildJobBuilder("job_id");
        jobBuilder.setDeleted(true);
        mlBuilder.putJob(jobBuilder.build(), false);
        Exception e = expectThrows(ElasticsearchStatusException.class,
                () -> OpenJobAction.validate("job_id", mlBuilder.build()));
        assertEquals("Cannot open job [job_id] because it has been marked as deleted", e.getMessage());
    }

    public void testSelectLeastLoadedMlNode() {
        Map<String, String> nodeAttr = new HashMap<>();
        nodeAttr.put(MachineLearning.ML_ENABLED_NODE_ATTR, "true");
        DiscoveryNodes nodes = DiscoveryNodes.builder()
                .add(new DiscoveryNode("_node_name1", "_node_id1", new InetSocketTransportAddress(InetAddress.getLoopbackAddress(), 9300),
                        nodeAttr, Collections.emptySet(), Version.CURRENT))
                .add(new DiscoveryNode("_node_name2", "_node_id2", new InetSocketTransportAddress(InetAddress.getLoopbackAddress(), 9301),
                        nodeAttr, Collections.emptySet(), Version.CURRENT))
                .add(new DiscoveryNode("_node_name3", "_node_id3", new InetSocketTransportAddress(InetAddress.getLoopbackAddress(), 9302),
                        nodeAttr, Collections.emptySet(), Version.CURRENT))
                .build();

        PersistentTasksCustomMetaData.Builder tasksBuilder = PersistentTasksCustomMetaData.builder();
        addJobTask("job_id1", "_node_id1", null, tasksBuilder);
        addJobTask("job_id2", "_node_id1", null, tasksBuilder);
        addJobTask("job_id3", "_node_id2", null, tasksBuilder);
        PersistentTasksCustomMetaData tasks = tasksBuilder.build();

        ClusterState.Builder cs = ClusterState.builder(new ClusterName("_name"));
        MetaData.Builder metaData = MetaData.builder();
        RoutingTable.Builder routingTable = RoutingTable.builder();
        addJobAndIndices(metaData, routingTable, "job_id1", "job_id2", "job_id3", "job_id4");
        cs.nodes(nodes);
        metaData.putCustom(PersistentTasksCustomMetaData.TYPE, tasks);
        cs.metaData(metaData);
        cs.routingTable(routingTable.build());
        Assignment result = OpenJobAction.selectLeastLoadedMlNode("job_id4", cs.build(), 2, 10, logger);
        assertEquals("_node_id3", result.getExecutorNode());
    }

    public void testSelectLeastLoadedMlNode_maxCapacity() {
        int numNodes = randomIntBetween(1, 10);
        int maxRunningJobsPerNode = randomIntBetween(1, 100);

        Map<String, String> nodeAttr = new HashMap<>();
        nodeAttr.put(MachineLearning.ML_ENABLED_NODE_ATTR, "true");
        DiscoveryNodes.Builder nodes = DiscoveryNodes.builder();
        PersistentTasksCustomMetaData.Builder tasksBuilder = PersistentTasksCustomMetaData.builder();
        for (int i = 0; i < numNodes; i++) {
            String nodeId = "_node_id" + i;
            InetSocketTransportAddress address = new InetSocketTransportAddress(InetAddress.getLoopbackAddress(), 9300 + i);
            nodes.add(new DiscoveryNode("_node_name" + i, nodeId, address, nodeAttr, Collections.emptySet(), Version.CURRENT));
            for (int j = 0; j < maxRunningJobsPerNode; j++) {
                long id = j + (maxRunningJobsPerNode * i);
                addJobTask("job_id" + id, nodeId, JobState.OPENED, tasksBuilder);
            }
        }
        PersistentTasksCustomMetaData tasks = tasksBuilder.build();

        ClusterState.Builder cs = ClusterState.builder(new ClusterName("_name"));
        MetaData.Builder metaData = MetaData.builder();
        RoutingTable.Builder routingTable = RoutingTable.builder();
        addJobAndIndices(metaData, routingTable, "job_id1", "job_id2");
        cs.nodes(nodes);
        metaData.putCustom(PersistentTasksCustomMetaData.TYPE, tasks);
        cs.metaData(metaData);
        cs.routingTable(routingTable.build());
        Assignment result = OpenJobAction.selectLeastLoadedMlNode("job_id2", cs.build(), 2, maxRunningJobsPerNode, logger);
        assertNull(result.getExecutorNode());
        assertTrue(result.getExplanation().contains("because this node is full. Number of opened jobs [" + maxRunningJobsPerNode
                + "], max_running_jobs [" + maxRunningJobsPerNode + "]"));
    }

    public void testSelectLeastLoadedMlNode_noMlNodes() {
        DiscoveryNodes nodes = DiscoveryNodes.builder()
                .add(new DiscoveryNode("_node_name1", "_node_id1", new InetSocketTransportAddress(InetAddress.getLoopbackAddress(), 9300),
                        Collections.emptyMap(), Collections.emptySet(), Version.CURRENT))
                .add(new DiscoveryNode("_node_name2", "_node_id2", new InetSocketTransportAddress(InetAddress.getLoopbackAddress(), 9301),
                        Collections.emptyMap(), Collections.emptySet(), Version.CURRENT))
                .build();

        PersistentTasksCustomMetaData.Builder tasksBuilder = PersistentTasksCustomMetaData.builder();
        addJobTask("job_id1", "_node_id1", null, tasksBuilder);
        PersistentTasksCustomMetaData tasks = tasksBuilder.build();

        ClusterState.Builder cs = ClusterState.builder(new ClusterName("_name"));
        MetaData.Builder metaData = MetaData.builder();
        RoutingTable.Builder routingTable = RoutingTable.builder();
        addJobAndIndices(metaData, routingTable, "job_id1", "job_id2");
        cs.nodes(nodes);
        metaData.putCustom(PersistentTasksCustomMetaData.TYPE, tasks);
        cs.metaData(metaData);
        cs.routingTable(routingTable.build());
        Assignment result = OpenJobAction.selectLeastLoadedMlNode("job_id2", cs.build(), 2, 10, logger);
        assertTrue(result.getExplanation().contains("because this node isn't a ml node"));
        assertNull(result.getExecutorNode());
    }

    public void testSelectLeastLoadedMlNode_maxConcurrentOpeningJobs() {
        Map<String, String> nodeAttr = new HashMap<>();
        nodeAttr.put(MachineLearning.ML_ENABLED_NODE_ATTR, "true");
        DiscoveryNodes nodes = DiscoveryNodes.builder()
                .add(new DiscoveryNode("_node_name1", "_node_id1", new InetSocketTransportAddress(InetAddress.getLoopbackAddress(), 9300),
                        nodeAttr, Collections.emptySet(), Version.CURRENT))
                .add(new DiscoveryNode("_node_name2", "_node_id2", new InetSocketTransportAddress(InetAddress.getLoopbackAddress(), 9301),
                        nodeAttr, Collections.emptySet(), Version.CURRENT))
                .add(new DiscoveryNode("_node_name3", "_node_id3", new InetSocketTransportAddress(InetAddress.getLoopbackAddress(), 9302),
                        nodeAttr, Collections.emptySet(), Version.CURRENT))
                .build();

        PersistentTasksCustomMetaData.Builder tasksBuilder =  PersistentTasksCustomMetaData.builder();
        addJobTask("job_id1", "_node_id1", null, tasksBuilder);
        addJobTask("job_id2", "_node_id1", null, tasksBuilder);
        addJobTask("job_id3", "_node_id2", null, tasksBuilder);
        addJobTask("job_id4", "_node_id2", null, tasksBuilder);
        addJobTask("job_id5", "_node_id3", null, tasksBuilder);
        PersistentTasksCustomMetaData tasks = tasksBuilder.build();

        ClusterState.Builder csBuilder = ClusterState.builder(new ClusterName("_name"));
        csBuilder.nodes(nodes);
        MetaData.Builder metaData = MetaData.builder();
        RoutingTable.Builder routingTable = RoutingTable.builder();
        addJobAndIndices(metaData, routingTable, "job_id1", "job_id2", "job_id3", "job_id4", "job_id5", "job_id6", "job_id7");
        csBuilder.routingTable(routingTable.build());
        metaData.putCustom(PersistentTasksCustomMetaData.TYPE, tasks);
        csBuilder.metaData(metaData);

        ClusterState cs = csBuilder.build();
        Assignment result = OpenJobAction.selectLeastLoadedMlNode("job_id6", cs, 2, 10, logger);
        assertEquals("_node_id3", result.getExecutorNode());

        tasksBuilder = PersistentTasksCustomMetaData.builder(tasks);
        addJobTask("job_id6", "_node_id3", null, tasksBuilder);
        tasks = tasksBuilder.build();

        csBuilder = ClusterState.builder(cs);
        csBuilder.metaData(MetaData.builder(cs.metaData()).putCustom(PersistentTasksCustomMetaData.TYPE, tasks));
        cs = csBuilder.build();
        result = OpenJobAction.selectLeastLoadedMlNode("job_id7", cs, 2, 10, logger);
        assertNull("no node selected, because OPENING state", result.getExecutorNode());
        assertTrue(result.getExplanation().contains("because node exceeds [2] the maximum number of jobs [2] in opening state"));

        tasksBuilder = PersistentTasksCustomMetaData.builder(tasks);
        tasksBuilder.reassignTask(MlMetadata.jobTaskId("job_id6"), new Assignment("_node_id3", "test assignment"));
        tasks = tasksBuilder.build();

        csBuilder = ClusterState.builder(cs);
        csBuilder.metaData(MetaData.builder(cs.metaData()).putCustom(PersistentTasksCustomMetaData.TYPE, tasks));
        cs = csBuilder.build();
        result = OpenJobAction.selectLeastLoadedMlNode("job_id7", cs, 2, 10, logger);
        assertNull("no node selected, because stale task", result.getExecutorNode());
        assertTrue(result.getExplanation().contains("because node exceeds [2] the maximum number of jobs [2] in opening state"));

        tasksBuilder = PersistentTasksCustomMetaData.builder(tasks);
        tasksBuilder.updateTaskStatus(MlMetadata.jobTaskId("job_id6"), null);
        tasks = tasksBuilder.build();

        csBuilder = ClusterState.builder(cs);
        csBuilder.metaData(MetaData.builder(cs.metaData()).putCustom(PersistentTasksCustomMetaData.TYPE, tasks));
        cs = csBuilder.build();
        result = OpenJobAction.selectLeastLoadedMlNode("job_id7", cs, 2, 10, logger);
        assertNull("no node selected, because null state", result.getExecutorNode());
        assertTrue(result.getExplanation().contains("because node exceeds [2] the maximum number of jobs [2] in opening state"));
    }

    public void testVerifyIndicesPrimaryShardsAreActive() {
        MetaData.Builder metaData = MetaData.builder();
        RoutingTable.Builder routingTable = RoutingTable.builder();
        addJobAndIndices(metaData, routingTable, "job_id");

        ClusterState.Builder csBuilder = ClusterState.builder(new ClusterName("_name"));
        csBuilder.routingTable(routingTable.build());
        csBuilder.metaData(metaData);

        ClusterState cs = csBuilder.build();
        assertEquals(0, OpenJobAction.verifyIndicesPrimaryShardsAreActive("job_id", cs).size());

        metaData = new MetaData.Builder(cs.metaData());
        routingTable = new RoutingTable.Builder(cs.routingTable());

        String indexToRemove = randomFrom(OpenJobAction.indicesOfInterest(cs, "job_id"));
        if (randomBoolean()) {
            routingTable.remove(indexToRemove);
        } else {
            Index index = new Index(indexToRemove, "_uuid");
            ShardId shardId = new ShardId(index, 0);
            ShardRouting shardRouting = ShardRouting.newUnassigned(shardId, true, RecoverySource.StoreRecoverySource.EMPTY_STORE_INSTANCE,
                    new UnassignedInfo(UnassignedInfo.Reason.INDEX_CREATED, ""));
            shardRouting = shardRouting.initialize("node_id", null, 0L);
            routingTable.add(IndexRoutingTable.builder(index)
                    .addIndexShard(new IndexShardRoutingTable.Builder(shardId).addShard(shardRouting).build()));
        }

        csBuilder.routingTable(routingTable.build());
        csBuilder.metaData(metaData);
        List<String> result = OpenJobAction.verifyIndicesPrimaryShardsAreActive("job_id", csBuilder.build());
        assertEquals(1, result.size());
        assertEquals(indexToRemove, result.get(0));
    }

    public static void addJobTask(String jobId, String nodeId, JobState jobState, PersistentTasksCustomMetaData.Builder builder) {
        builder.addTask(MlMetadata.jobTaskId(jobId), OpenJobAction.TASK_NAME, new OpenJobAction.JobParams(jobId),
                new Assignment(nodeId, "test assignment"));
        if (jobState != null) {
            builder.updateTaskStatus(MlMetadata.jobTaskId(jobId), new JobTaskStatus(jobState, builder.getLastAllocationId()));
        }
    }

    private void addJobAndIndices(MetaData.Builder metaData, RoutingTable.Builder routingTable, String... jobIds) {
        List<String> indices = new ArrayList<>();
        indices.add(AnomalyDetectorsIndex.jobStateIndexName());
        indices.add(AnomalyDetectorsIndex.ML_META_INDEX);
        indices.add(Auditor.NOTIFICATIONS_INDEX);
        indices.add(AnomalyDetectorsIndex.RESULTS_INDEX_PREFIX + AnomalyDetectorsIndex.RESULTS_INDEX_DEFAULT);
        for (String indexName : indices) {
            IndexMetaData.Builder indexMetaData = IndexMetaData.builder(indexName);
            indexMetaData.settings(Settings.builder()
                    .put(IndexMetaData.SETTING_VERSION_CREATED, Version.CURRENT)
                    .put(IndexMetaData.SETTING_NUMBER_OF_SHARDS, 1)
                    .put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, 0)
            );
            metaData.put(indexMetaData);
            Index index = new Index(indexName, "_uuid");
            ShardId shardId = new ShardId(index, 0);
            ShardRouting shardRouting = ShardRouting.newUnassigned(shardId, true, RecoverySource.StoreRecoverySource.EMPTY_STORE_INSTANCE,
                    new UnassignedInfo(UnassignedInfo.Reason.INDEX_CREATED, ""));
            shardRouting = shardRouting.initialize("node_id", null, 0L);
            shardRouting = shardRouting.moveToStarted();
            routingTable.add(IndexRoutingTable.builder(index)
                    .addIndexShard(new IndexShardRoutingTable.Builder(shardId).addShard(shardRouting).build()));
        }

        MlMetadata.Builder mlMetadata = new MlMetadata.Builder();
        for (String jobId : jobIds) {
            Job job = BaseMlIntegTestCase.createFareQuoteJob(jobId).build(new Date());
            mlMetadata.putJob(job, false);
        }
        metaData.putCustom(MlMetadata.TYPE, mlMetadata.build());
    }

}
