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
package org.elasticsearch.xpack.ml.utils;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.ClusterStateObserver;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.ml.job.config.JobState;
import org.elasticsearch.xpack.ml.job.metadata.Allocation;
import org.elasticsearch.xpack.ml.job.metadata.MlMetadata;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class JobStateObserver {

    private static final Logger LOGGER = Loggers.getLogger(JobStateObserver.class);

    private final ThreadPool threadPool;
    private final ClusterService clusterService;

    public JobStateObserver(ThreadPool threadPool, ClusterService clusterService) {
        this.threadPool = threadPool;
        this.clusterService = clusterService;
    }

    public void waitForState(String jobId, TimeValue waitTimeout, JobState expectedState, Consumer<Exception> handler) {
        ClusterStateObserver observer =
                new ClusterStateObserver(clusterService, LOGGER, threadPool.getThreadContext());
        JobStatePredicate jobStatePredicate = new JobStatePredicate(jobId, expectedState);
        observer.waitForNextChange(new ClusterStateObserver.Listener() {
            @Override
            public void onNewClusterState(ClusterState state) {
                handler.accept(null);
            }

            @Override
            public void onClusterServiceClose() {
                Exception e = new IllegalArgumentException("Cluster service closed while waiting for job state to change to ["
                        + expectedState + "]");
                handler.accept(new IllegalStateException(e));
            }

            @Override
            public void onTimeout(TimeValue timeout) {
                if (jobStatePredicate.test(clusterService.state())) {
                    handler.accept(null);
                } else {
                    Exception e = new IllegalArgumentException("Timeout expired while waiting for job state to change to ["
                            + expectedState + "]");
                    handler.accept(e);
                }
            }
        }, jobStatePredicate, waitTimeout);
    }

    private static class JobStatePredicate implements Predicate<ClusterState> {

        private final String jobId;
        private final JobState expectedState;

        JobStatePredicate(String jobId, JobState expectedState) {
            this.jobId = jobId;
            this.expectedState = expectedState;
        }

        @Override
        public boolean test(ClusterState newState) {
            MlMetadata metadata = newState.getMetaData().custom(MlMetadata.TYPE);
            if (metadata != null) {
                Allocation allocation = metadata.getAllocations().get(jobId);
                if (allocation != null) {
                    return allocation.getState() == expectedState;
                }
            }
            return false;
        }

    }

}
