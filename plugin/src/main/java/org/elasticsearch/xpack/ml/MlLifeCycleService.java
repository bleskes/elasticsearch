/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2017 Elasticsearch BV. All Rights Reserved.
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
package org.elasticsearch.xpack.ml;

import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.component.LifecycleListener;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xpack.ml.datafeed.DatafeedManager;
import org.elasticsearch.xpack.ml.job.process.NativeController;
import org.elasticsearch.xpack.ml.job.process.NativeControllerHolder;
import org.elasticsearch.xpack.ml.job.process.autodetect.AutodetectProcessManager;

import java.io.IOException;

public class MlLifeCycleService extends AbstractComponent {

    private final DatafeedManager datafeedManager;
    private final AutodetectProcessManager autodetectProcessManager;

    public MlLifeCycleService(Settings settings, ClusterService clusterService) {
        this(settings, clusterService, null, null);
    }

    public MlLifeCycleService(Settings settings, ClusterService clusterService, DatafeedManager datafeedManager,
                              AutodetectProcessManager autodetectProcessManager) {
        super(settings);
        this.datafeedManager = datafeedManager;
        this.autodetectProcessManager = autodetectProcessManager;
        clusterService.addLifecycleListener(new LifecycleListener() {
            @Override
            public void beforeStop() {
                stop();
            }
        });
    }

    public synchronized void stop() {
        try {
            if (MachineLearningFeatureSet.isRunningOnMlPlatform(false)) {
                // This prevents datafeeds from sending data to autodetect processes WITHOUT stopping the
                // datafeeds, so they get reallocated.  We have to do this first, otherwise the datafeeds
                // could fail if they send data to a dead autodetect process.
                if (datafeedManager != null) {
                    datafeedManager.isolateAllDatafeedsOnThisNode();
                }
                NativeController nativeController = NativeControllerHolder.getNativeController(settings);
                if (nativeController != null) {
                    // This kills autodetect processes WITHOUT closing the jobs, so they get reallocated.
                    if (autodetectProcessManager != null) {
                        autodetectProcessManager.killAllProcessesOnThisNode();
                    }
                    nativeController.stop();
                }
            }
        } catch (IOException e) {
            // We're stopping anyway, so don't let this complicate the shutdown sequence
        }
    }
}
