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
package org.elasticsearch.xpack.ml.job;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.LocalNodeMasterListener;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.component.LifecycleListener;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.ml.action.UpdateProcessAction;
import org.elasticsearch.xpack.ml.job.config.JobUpdate;

import java.util.concurrent.LinkedBlockingQueue;

import static org.elasticsearch.xpack.ml.action.UpdateProcessAction.Request;
import static org.elasticsearch.xpack.ml.action.UpdateProcessAction.Response;

public class UpdateJobProcessNotifier extends AbstractComponent
        implements LocalNodeMasterListener {

    private final Client client;
    private final ThreadPool threadPool;
    private final LinkedBlockingQueue<JobUpdate> orderedJobUpdates =
            new LinkedBlockingQueue<>(1000);

    private volatile ThreadPool.Cancellable cancellable;

    public UpdateJobProcessNotifier(Settings settings, Client client,
                                    ClusterService clusterService, ThreadPool threadPool) {
        super(settings);
        this.client = client;
        this.threadPool = threadPool;
        clusterService.addLocalNodeMasterListener(this);
        clusterService.addLifecycleListener(new LifecycleListener() {
            @Override
            public void beforeStop() {
                stop();
            }
        });
    }

    boolean submitJobUpdate(JobUpdate jobUpdate) {
        return orderedJobUpdates.offer(jobUpdate);
    }

    @Override
    public void onMaster() {
        start();
    }

    @Override
    public void offMaster() {
        stop();
    }

    void start() {
        cancellable = threadPool.scheduleWithFixedDelay(this::processNextUpdate,
                TimeValue.timeValueSeconds(1), ThreadPool.Names.GENERIC);
    }

    void stop() {
        orderedJobUpdates.clear();

        ThreadPool.Cancellable cancellable = this.cancellable;
        if (cancellable != null) {
            cancellable.cancel();
        }
    }

    @Override
    public String executorName() {
        // SAME is ok here, because both start() and stop() are inexpensive:
        return ThreadPool.Names.SAME;
    }

    void processNextUpdate() {
        try {
            JobUpdate jobUpdate = orderedJobUpdates.poll();
            if (jobUpdate != null) {
                executeRemoteJob(jobUpdate);
            }
        } catch (Exception e) {
            logger.error("Unable while processing next job update", e);
        }
    }

    void executeRemoteJob(JobUpdate update) {
        Request request = new Request(update.getJobId(), update.getModelPlotConfig(),
                update.getDetectorUpdates());
        client.execute(UpdateProcessAction.INSTANCE, request,
                new ActionListener<Response>() {
                    @Override
                    public void onResponse(Response response) {
                        if (response.isUpdated()) {
                            logger.info("Successfully updated remote job [{}]", update.getJobId());
                        } else {
                            logger.error("Failed to update remote job [{}]", update.getJobId());
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        logger.error("Failed to update remote job [" +  update.getJobId() + "]",
                                e);
                    }
                });
    }

}
