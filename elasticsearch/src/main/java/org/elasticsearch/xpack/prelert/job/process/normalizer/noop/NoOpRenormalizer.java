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
package org.elasticsearch.xpack.prelert.job.process.normalizer.noop;

import org.elasticsearch.xpack.prelert.job.process.normalizer.Renormalizer;
import org.elasticsearch.xpack.prelert.job.quantiles.Quantiles;

/**
 * A {@link Renormalizer} implementation that does absolutely nothing
 * This should be removed when the normalizer code is ported
 */
public class NoOpRenormalizer implements Renormalizer {

    @Override
    public void renormalize(Quantiles quantiles) {
    }

    @Override
    public void waitUntilIdle() {
    }
}
