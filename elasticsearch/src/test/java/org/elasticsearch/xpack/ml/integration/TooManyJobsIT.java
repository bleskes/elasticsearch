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

import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xpack.ml.action.CloseJobAction;
import org.elasticsearch.xpack.ml.action.GetJobsStatsAction;
import org.elasticsearch.xpack.ml.action.OpenJobAction;
import org.elasticsearch.xpack.ml.action.PutJobAction;
import org.elasticsearch.xpack.ml.job.config.Job;
import org.elasticsearch.xpack.ml.job.config.JobState;
import org.elasticsearch.xpack.ml.job.process.autodetect.AutodetectProcessManager;
import org.elasticsearch.xpack.ml.support.BaseMlIntegTestCase;
import org.elasticsearch.xpack.persistent.PersistentActionResponse;

import java.util.concurrent.ExecutionException;

public class TooManyJobsIT extends BaseMlIntegTestCase {
  
    public void testSingleNode() throws Exception {
        verifyMaxNumberOfJobsLimit(1, randomIntBetween(1, 32));
    }

    public void testMultipleNodes() throws Exception {
        verifyMaxNumberOfJobsLimit(3, randomIntBetween(1, 32));
    }

    private void verifyMaxNumberOfJobsLimit(int numNodes, int maxNumberOfJobsPerNode) throws Exception {
        // clear all nodes, so that we can set max_running_jobs setting:
        internalCluster().ensureAtMostNumDataNodes(0);
        logger.info("[{}] is [{}]", AutodetectProcessManager.MAX_RUNNING_JOBS_PER_NODE.getKey(), maxNumberOfJobsPerNode);
        for (int i = 0; i < numNodes; i++) {
            internalCluster().startNode(Settings.builder()
                    .put(AutodetectProcessManager.MAX_RUNNING_JOBS_PER_NODE.getKey(), maxNumberOfJobsPerNode));
        }
        logger.info("Started [{}] nodes", numNodes);

        int clusterWideMaxNumberOfJobs = numNodes * maxNumberOfJobsPerNode;
        for (int i = 1; i <= (clusterWideMaxNumberOfJobs + 1); i++) {
            Job.Builder job = createJob(Integer.toString(i));
            PutJobAction.Request putJobRequest = new PutJobAction.Request(job.build(true, job.getId()));
            PutJobAction.Response putJobResponse = client().execute(PutJobAction.INSTANCE, putJobRequest).get();
            assertTrue(putJobResponse.isAcknowledged());

            try {
                OpenJobAction.Request openJobRequest = new OpenJobAction.Request(job.getId());
                PersistentActionResponse openJobResponse = client().execute(OpenJobAction.INSTANCE, openJobRequest).get();
                assertBusy(() -> {
                    GetJobsStatsAction.Response statsResponse =
                            client().execute(GetJobsStatsAction.INSTANCE, new GetJobsStatsAction.Request(job.getId())).actionGet();
                    assertEquals(statsResponse.getResponse().results().get(0).getState(), JobState.OPENED);
                });
                logger.info("Opened {}th job", i);
            } catch (ExecutionException e) {
                Exception cause = (Exception) e.getCause();
                assertEquals(ElasticsearchStatusException.class, cause.getClass());
                assertEquals("[" + i + "] expected state [" + JobState.OPENED + "] but got [" + JobState.FAILED +"]", cause.getMessage());
                logger.info("good news everybody --> reached maximum number of allowed opened jobs, after trying to open the {}th job", i);

                // close the first job and check if the latest job gets opened:
                CloseJobAction.Request closeRequest = new CloseJobAction.Request("1");
                CloseJobAction.Response closeResponse = client().execute(CloseJobAction.INSTANCE, closeRequest).actionGet();
                assertTrue(closeResponse.isClosed());
                assertBusy(() -> {
                    GetJobsStatsAction.Response statsResponse =
                            client().execute(GetJobsStatsAction.INSTANCE, new GetJobsStatsAction.Request(job.getId())).actionGet();
                    assertEquals(statsResponse.getResponse().results().get(0).getState(), JobState.OPENED);
                });
                cleanupWorkaround(numNodes);
                return;
            }
        }
        cleanupWorkaround(numNodes);
        fail("shouldn't be able to add more than [" + clusterWideMaxNumberOfJobs + "] jobs");
    }

}
