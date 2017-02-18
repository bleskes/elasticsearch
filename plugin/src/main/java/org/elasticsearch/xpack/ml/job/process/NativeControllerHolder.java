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
package org.elasticsearch.xpack.ml.job.process;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.xpack.ml.MachineLearning;
import org.elasticsearch.xpack.ml.utils.NamedPipeHelper;

import java.io.IOException;

/**
 * Manages a singleton NativeController so that both the MachineLearningFeatureSet and MachineLearning classes can
 * get access to the same one.
 */
public class NativeControllerHolder {

    private static final Object lock = new Object();
    private static NativeController nativeController;

    private NativeControllerHolder() {
    }

    /**
     * Get a reference to the singleton native process controller.
     *
     * The NativeController is created lazily to allow time for the C++ process to be started before connection is attempted.
     *
     * null is returned to tests that haven't bothered to set up path.home and all runs where useNativeProcess=false.
     *
     * Calls may throw an exception if initial connection to the C++ process fails.
     */
    public static NativeController getNativeController(Settings settings) throws IOException {

        if (Environment.PATH_HOME_SETTING.exists(settings) && MachineLearning.USE_NATIVE_PROCESS_OPTION.get(settings)) {
            synchronized (lock) {
                if (nativeController == null) {
                    nativeController = new NativeController(new Environment(settings), new NamedPipeHelper());
                    nativeController.tailLogsInThread();
                }
            }
            return nativeController;
        }
        return null;
    }
}
