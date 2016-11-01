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
package org.elasticsearch.xpack.prelert.job.process.normalizer.noop;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.xpack.prelert.job.process.normalizer.Renormaliser;
import org.elasticsearch.xpack.prelert.job.quantiles.Quantiles;

/**
 * A {@link Renormaliser} implementation that does absolutely nothing
 * This should be removed when the normaliser code is ported
 */
public class NoOpRenormaliser implements Renormaliser {
    // NORELEASE Remove once the normaliser code is ported
    @Override
    public void renormalise(Quantiles quantiles, Logger logger) {

    }

    @Override
    public void renormaliseWithPartition(Quantiles quantiles, Logger logger) {

    }

    @Override
    public void waitUntilIdle() {

    }

    @Override
    public boolean shutdown(Logger logger) {
        return true;
    }
}
