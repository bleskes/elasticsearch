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
package org.elasticsearch.xpack.ml.integration;

import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.CheckedRunnable;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.test.junit.annotations.TestLogging;
import org.elasticsearch.xpack.ml.action.GetDatafeedsStatsAction;
import org.elasticsearch.xpack.ml.action.GetDatafeedsStatsAction.Response.DatafeedStats;
import org.elasticsearch.xpack.ml.action.GetJobsStatsAction;
import org.elasticsearch.xpack.ml.action.GetJobsStatsAction.Response.JobStats;
import org.elasticsearch.xpack.ml.action.OpenJobAction;
import org.elasticsearch.xpack.ml.action.PutDatafeedAction;
import org.elasticsearch.xpack.ml.action.PutJobAction;
import org.elasticsearch.xpack.ml.action.StartDatafeedAction;
import org.elasticsearch.xpack.ml.datafeed.DatafeedConfig;
import org.elasticsearch.xpack.ml.datafeed.DatafeedState;
import org.elasticsearch.xpack.ml.job.config.Job;
import org.elasticsearch.xpack.ml.job.config.JobState;
import org.elasticsearch.xpack.ml.job.process.autodetect.state.DataCounts;
import org.elasticsearch.xpack.ml.support.BaseMlIntegTestCase;
import org.elasticsearch.xpack.persistent.PersistentTasksCustomMetaData;
import org.elasticsearch.xpack.persistent.PersistentTasksCustomMetaData.PersistentTask;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@TestLogging("org.elasticsearch.xpack.ml.datafeed:DEBUG,org.elasticsearch.xpack.ml.action:DEBUG")
public class MlDistributedFailureIT extends BaseMlIntegTestCase {

    public void testFailOver() throws Exception {
        internalCluster().ensureAtLeastNumDataNodes(3);
        ensureStableCluster(3);
        run(() -> {
            GetJobsStatsAction.Request  request = new GetJobsStatsAction.Request("job_id");
            GetJobsStatsAction.Response response = client().execute(GetJobsStatsAction.INSTANCE, request).actionGet();
            DiscoveryNode discoveryNode = response.getResponse().results().get(0).getNode();
            internalCluster().stopRandomNode(settings -> discoveryNode.getName().equals(settings.get("node.name")));
            ensureStableCluster(2);
        });
    }

    @TestLogging("org.elasticsearch.xpack.persistent:TRACE,org.elasticsearch.cluster.service:TRACE")
    public void testLoseDedicatedMasterNode() throws Exception {
        internalCluster().ensureAtMostNumDataNodes(0);
        logger.info("Starting dedicated master node...");
        internalCluster().startNode(Settings.builder()
                .put("node.master", true)
                .put("node.data", false)
                .put("node.ml", false)
                .build());
        logger.info("Starting ml and data node...");
        String mlAndDataNode = internalCluster().startNode(Settings.builder()
                .put("node.master", false)
                .build());
        ensureStableCluster(2);
        run(() -> {
            logger.info("Stopping dedicated master node");
            internalCluster().stopRandomNode(settings -> settings.getAsBoolean("node.master", false));
            assertBusy(() -> {
                ClusterState state = client(mlAndDataNode).admin().cluster().prepareState()
                        .setLocal(true).get().getState();
                assertNull(state.nodes().getMasterNodeId());
            });
            logger.info("Restarting dedicated master node");
            internalCluster().startNode(Settings.builder()
                    .put("node.master", true)
                    .put("node.data", false)
                    .put("node.ml", false)
                    .build());
            ensureStableCluster(2);
        });
    }

    public void testFullClusterRestart() throws Exception {
        internalCluster().ensureAtLeastNumDataNodes(3);
        ensureStableCluster(3);
        run(() -> {
            logger.info("Restarting all nodes");
            internalCluster().fullRestart();
            logger.info("Restarted all nodes");
        });
    }

    private void run(CheckedRunnable<Exception> disrupt) throws Exception {
        client().admin().indices().prepareCreate("data")
                .addMapping("type", "time", "type=date")
                .get();
        long numDocs1 = randomIntBetween(32, 2048);
        long now = System.currentTimeMillis();
        long weekAgo = now - 604800000;
        long twoWeeksAgo = weekAgo - 604800000;
        indexDocs(logger, "data", numDocs1, twoWeeksAgo, weekAgo);

        Job.Builder job = createScheduledJob("job_id");
        PutJobAction.Request putJobRequest = new PutJobAction.Request(job);
        PutJobAction.Response putJobResponse = client().execute(PutJobAction.INSTANCE, putJobRequest).actionGet();
        assertTrue(putJobResponse.isAcknowledged());

        DatafeedConfig config = createDatafeed("data_feed_id", job.getId(), Collections.singletonList("data"));
        PutDatafeedAction.Request putDatafeedRequest = new PutDatafeedAction.Request(config);
        PutDatafeedAction.Response putDatadeedResponse = client().execute(PutDatafeedAction.INSTANCE, putDatafeedRequest)
                .actionGet();
        assertTrue(putDatadeedResponse.isAcknowledged());

        client().execute(OpenJobAction.INSTANCE, new OpenJobAction.Request(job.getId()));
        assertBusy(() -> {
            GetJobsStatsAction.Response statsResponse =
                    client().execute(GetJobsStatsAction.INSTANCE, new GetJobsStatsAction.Request(job.getId())).actionGet();
            assertEquals(statsResponse.getResponse().results().get(0).getState(), JobState.OPENED);
        });

        StartDatafeedAction.Request startDatafeedRequest = new StartDatafeedAction.Request(config.getId(), 0L);
        client().execute(StartDatafeedAction.INSTANCE, startDatafeedRequest).get();
        assertBusy(() -> {
            DataCounts dataCounts = getDataCounts(job.getId());
            assertEquals(numDocs1, dataCounts.getProcessedRecordCount());
            assertEquals(0L, dataCounts.getOutOfOrderTimeStampCount());
        });

        disrupt.run();
        assertBusy(() -> {
            ClusterState clusterState = client().admin().cluster().prepareState().get().getState();
            PersistentTasksCustomMetaData tasks = clusterState.metaData().custom(PersistentTasksCustomMetaData.TYPE);
            assertNotNull(tasks);
            assertEquals(2, tasks.taskMap().size());
            for (PersistentTask<?> task : tasks.tasks()) {
                assertFalse(task.needsReassignment(clusterState.nodes()));
            }

            GetJobsStatsAction.Request jobStatsRequest = new GetJobsStatsAction.Request("job_id");
            JobStats jobStats = client().execute(GetJobsStatsAction.INSTANCE, jobStatsRequest).actionGet()
                    .getResponse().results().get(0);
            assertEquals(JobState.OPENED, jobStats.getState());
            assertNotNull(jobStats.getNode());

            GetDatafeedsStatsAction.Request datafeedStatsRequest = new GetDatafeedsStatsAction.Request("data_feed_id");
            DatafeedStats datafeedStats = client().execute(GetDatafeedsStatsAction.INSTANCE, datafeedStatsRequest).actionGet()
                    .getResponse().results().get(0);
            assertEquals(DatafeedState.STARTED, datafeedStats.getDatafeedState());
            assertNotNull(datafeedStats.getNode());
        });

        long numDocs2 = randomIntBetween(2, 64);
        long now2 = System.currentTimeMillis();
        indexDocs(logger, "data", numDocs2, now2 + 5000, now2 + 6000);
        assertBusy(() -> {
            DataCounts dataCounts = getDataCounts(job.getId());
            assertEquals(numDocs1 + numDocs2, dataCounts.getProcessedRecordCount());
            assertEquals(0L, dataCounts.getOutOfOrderTimeStampCount());
        }, 30, TimeUnit.SECONDS);
    }

}
