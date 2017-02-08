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
package org.elasticsearch.xpack.ml.job.process.normalizer;

import java.util.Objects;
import java.util.concurrent.ExecutorService;

public class NormalizerFactory {

    private final NormalizerProcessFactory processFactory;
    private final ExecutorService executorService;

    public NormalizerFactory(NormalizerProcessFactory processFactory, ExecutorService executorService) {
        this.processFactory = Objects.requireNonNull(processFactory);
        this.executorService = Objects.requireNonNull(executorService);
    }

    public Normalizer create(String jobId) {
        return new Normalizer(jobId, processFactory, executorService);
    }
}
