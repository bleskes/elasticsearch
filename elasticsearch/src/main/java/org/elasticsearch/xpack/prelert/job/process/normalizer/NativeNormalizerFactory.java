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
package org.elasticsearch.xpack.prelert.job.process.normalizer;

import java.util.Objects;
import java.util.concurrent.ExecutorService;

public class NativeNormalizerFactory implements NormalizerFactory {

    private final NormalizerProcessFactory processFactory;
    private final ExecutorService executorService;

    public NativeNormalizerFactory(NormalizerProcessFactory processFactory, ExecutorService executorService) {
        this.processFactory = Objects.requireNonNull(processFactory);
        this.executorService = Objects.requireNonNull(executorService);
    }

    @Override
    public Normalizer create(String jobId) {
        return new Normalizer(jobId, processFactory, executorService);
    }
}
