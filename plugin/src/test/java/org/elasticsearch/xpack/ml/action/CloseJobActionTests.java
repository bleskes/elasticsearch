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
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.ml.job.config.JobState;
import org.elasticsearch.xpack.ml.job.metadata.MlMetadata;
import org.elasticsearch.xpack.persistent.PersistentTasksInProgress;
import org.elasticsearch.xpack.persistent.PersistentTasksInProgress.PersistentTaskInProgress;

import java.util.Collections;

import static org.elasticsearch.xpack.ml.job.config.JobTests.buildJobBuilder;

public class CloseJobActionTests extends ESTestCase {

    public void testMoveJobToClosingState() {
        MlMetadata.Builder mlBuilder = new MlMetadata.Builder();
        mlBuilder.putJob(buildJobBuilder("job_id").build(), false);
        PersistentTaskInProgress<OpenJobAction.Request> task =
                new PersistentTaskInProgress<>(1L, OpenJobAction.NAME, new OpenJobAction.Request("job_id"), false, true, null);
        task = new PersistentTaskInProgress<>(task, randomFrom(JobState.OPENED, JobState.FAILED));

        ClusterState.Builder csBuilder = ClusterState.builder(new ClusterName("_name"))
                .metaData(new MetaData.Builder().putCustom(MlMetadata.TYPE, mlBuilder.build())
                        .putCustom(PersistentTasksInProgress.TYPE, new PersistentTasksInProgress(1L, Collections.singletonMap(1L, task))));
        ClusterState result = CloseJobAction.moveJobToClosingState("job_id", csBuilder.build());

        PersistentTasksInProgress actualTasks = result.getMetaData().custom(PersistentTasksInProgress.TYPE);
        assertEquals(JobState.CLOSING, actualTasks.getTask(1L).getStatus());

        MlMetadata actualMetadata = result.metaData().custom(MlMetadata.TYPE);
        assertNotNull(actualMetadata.getJobs().get("job_id").getFinishedTime());
    }

    public void testMoveJobToClosingState_jobMissing() {
        MlMetadata.Builder mlBuilder = new MlMetadata.Builder();
        ClusterState.Builder csBuilder = ClusterState.builder(new ClusterName("_name"))
                .metaData(new MetaData.Builder().putCustom(MlMetadata.TYPE, mlBuilder.build())
                        .putCustom(PersistentTasksInProgress.TYPE, new PersistentTasksInProgress(1L, Collections.emptyMap())));
        expectThrows(ResourceNotFoundException.class, () -> CloseJobAction.moveJobToClosingState("job_id", csBuilder.build()));
    }

    public void testMoveJobToClosingState_unexpectedJobState() {
        MlMetadata.Builder mlBuilder = new MlMetadata.Builder();
        mlBuilder.putJob(buildJobBuilder("job_id").build(), false);
        PersistentTaskInProgress<OpenJobAction.Request> task =
                new PersistentTaskInProgress<>(1L, OpenJobAction.NAME, new OpenJobAction.Request("job_id"), false, true, null);
        task = new PersistentTaskInProgress<>(task, JobState.OPENING);

        ClusterState.Builder csBuilder1 = ClusterState.builder(new ClusterName("_name"))
                .metaData(new MetaData.Builder().putCustom(MlMetadata.TYPE, mlBuilder.build())
                        .putCustom(PersistentTasksInProgress.TYPE, new PersistentTasksInProgress(1L, Collections.singletonMap(1L, task))));
        ElasticsearchStatusException result =
                expectThrows(ElasticsearchStatusException.class, () -> CloseJobAction.moveJobToClosingState("job_id", csBuilder1.build()));
        assertEquals("cannot close job, expected job state [opened], but got [opening]", result.getMessage());

        ClusterState.Builder csBuilder2 = ClusterState.builder(new ClusterName("_name"))
                .metaData(new MetaData.Builder().putCustom(MlMetadata.TYPE, mlBuilder.build())
                        .putCustom(PersistentTasksInProgress.TYPE, new PersistentTasksInProgress(1L, Collections.emptyMap())));
        result = expectThrows(ElasticsearchStatusException.class, () -> CloseJobAction.moveJobToClosingState("job_id", csBuilder2.build()));
        assertEquals("cannot close job, expected job state [opened], but got [closed]", result.getMessage());
    }

}
