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
