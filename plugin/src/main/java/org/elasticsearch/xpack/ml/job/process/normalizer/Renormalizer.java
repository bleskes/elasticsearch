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

import org.elasticsearch.xpack.ml.job.process.autodetect.state.Quantiles;

public interface Renormalizer {
    /**
     * Update the anomaly score field on all previously persisted buckets
     * and all contained records
     */
    void renormalize(Quantiles quantiles);

    /**
     * Blocks until the renormalizer is idle and no further quantiles updates are pending.
     */
    void waitUntilIdle();
}
