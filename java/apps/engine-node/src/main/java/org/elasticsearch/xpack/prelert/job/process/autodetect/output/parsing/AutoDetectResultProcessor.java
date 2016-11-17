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
package org.elasticsearch.xpack.prelert.job.process.autodetect.output.parsing;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.xpack.prelert.job.ModelSizeStats;
import org.elasticsearch.xpack.prelert.job.ModelSnapshot;
import org.elasticsearch.xpack.prelert.job.persistence.JobResultsPersister;
import org.elasticsearch.xpack.prelert.job.process.autodetect.output.FlushAcknowledgement;
import org.elasticsearch.xpack.prelert.job.process.normalizer.Renormaliser;
import org.elasticsearch.xpack.prelert.job.quantiles.Quantiles;
import org.elasticsearch.xpack.prelert.job.results.AutodetectResult;
import org.elasticsearch.xpack.prelert.job.results.Bucket;
import org.elasticsearch.xpack.prelert.job.results.CategoryDefinition;
import org.elasticsearch.xpack.prelert.job.results.ModelDebugOutput;
import org.elasticsearch.xpack.prelert.utils.CloseableIterator;

import java.io.InputStream;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

/**
 * A runnable class that reads the autodetect process output
 * and writes the results via the {@linkplain JobResultsPersister}
 * passed in the constructor.
 * <p>
 * Has methods to register and remove alert observers.
 * Also has a method to wait for a flush to be complete.
 */
public class AutoDetectResultProcessor {

    private final Renormaliser renormaliser;
    private final JobResultsPersister persister;
    private final AutodetectResultsParser parser;

    private final CountDownLatch latch = new CountDownLatch(1);
    private final FlushListener flushListener;

    public AutoDetectResultProcessor(Renormaliser renormaliser, JobResultsPersister persister, AutodetectResultsParser parser) {
        this.renormaliser = renormaliser;
        this.persister = persister;
        this.parser = parser;
        this.flushListener = new FlushListener();
    }

    AutoDetectResultProcessor(Renormaliser renormaliser, JobResultsPersister persister, AutodetectResultsParser parser,
                              FlushListener flushListener) {
        this.renormaliser = renormaliser;
        this.persister = persister;
        this.parser = parser;
        this.flushListener = flushListener;
    }

    public void process(Logger jobLogger, InputStream in, boolean isPerPartitionNormalisation) {
        try (CloseableIterator<AutodetectResult> iterator = parser.parseResults(in)) {
            int bucketCount = 0;
            Context context = new Context(jobLogger, isPerPartitionNormalisation);
            while (iterator.hasNext()) {
                AutodetectResult result = iterator.next();
                processResult(context, result);
                bucketCount++;
                jobLogger.trace("Bucket number {} parsed from output", bucketCount);
            }
            jobLogger.info(bucketCount + " buckets parsed from autodetect output - about to refresh indexes");
            jobLogger.info("Parse results Complete");
        } catch (Exception e) {
            jobLogger.info("Error parsing autodetect output", e);
        } finally {
            latch.countDown();
            flushListener.acknowledgeAllFlushes();
            renormaliser.shutdown(jobLogger);
        }
    }

    void processResult(Context context, AutodetectResult result) {
        Bucket bucket = result.getBucket();
        if (bucket != null) {
            if (context.deleteInterimRequired) {
                // Delete any existing interim results at the start
                // of a job upload:
                // these are generated by a Flush command, and will
                // be replaced or
                // superseded by new results
                context.jobLogger.trace("Deleting interim results");

                // NOCOMMIT: This feels like an odd side-effect to
                // have in a parser,
                // especially since it has to wire up to
                // actionlisteners. Feels like it should
                // be refactored out somewhere, after parsing?
                persister.deleteInterimResults();
                context.deleteInterimRequired = false;
            }
            if (context.isPerPartitionNormalization) {
                bucket.calcMaxNormalizedProbabilityPerPartition();
            }
            persister.persistBucket(bucket);
        }
        CategoryDefinition categoryDefinition = result.getCategoryDefinition();
        if (categoryDefinition != null) {
            persister.persistCategoryDefinition(categoryDefinition);
        }
        FlushAcknowledgement flushAcknowledgement = result.getFlushAcknowledgement();
        if (flushAcknowledgement != null) {
            context.jobLogger.debug("Flush acknowledgement parsed from output for ID " + flushAcknowledgement.getId());
            // Commit previous writes here, effectively continuing
            // the flush from the C++ autodetect process right
            // through to the data store
            persister.commitWrites();
            flushListener.acknowledgeFlush(flushAcknowledgement.getId());
            // Interim results may have been produced by the flush,
            // which need to be
            // deleted when the next finalized results come through
            context.deleteInterimRequired = true;
        }
        ModelDebugOutput modelDebugOutput = result.getModelDebugOutput();
        if (modelDebugOutput != null) {
            persister.persistModelDebugOutput(modelDebugOutput);
        }
        ModelSizeStats modelSizeStats = result.getModelSizeStats();
        if (modelSizeStats != null) {
            context.jobLogger.trace(String.format(Locale.ROOT, "Parsed ModelSizeStats: %d / %d / %d / %d / %d / %s",
                    modelSizeStats.getModelBytes(), modelSizeStats.getTotalByFieldCount(), modelSizeStats.getTotalOverFieldCount(),
                    modelSizeStats.getTotalPartitionFieldCount(), modelSizeStats.getBucketAllocationFailuresCount(),
                    modelSizeStats.getMemoryStatus()));

            persister.persistModelSizeStats(modelSizeStats);
        }
        ModelSnapshot modelSnapshot = result.getModelSnapshot();
        if (modelSnapshot != null) {
            persister.persistModelSnapshot(modelSnapshot);
        }
        Quantiles quantiles = result.getQuantiles();
        if (quantiles != null) {
            persister.persistQuantiles(quantiles);

            context.jobLogger.debug("Quantiles parsed from output - will " + "trigger renormalisation of scores");
            if (context.isPerPartitionNormalization) {
                renormaliser.renormaliseWithPartition(quantiles, context.jobLogger);
            } else {
                renormaliser.renormalise(quantiles, context.jobLogger);
            }
        }
    }

    public void awaitCompletion() {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    /**
     * Blocks until a flush is acknowledged or the timeout expires, whichever happens first.
     *
     * @param flushId the id of the flush request to wait for
     * @param timeout the timeout
     * @return {@code true} if the flush has completed or the parsing finished; {@code false} if the timeout expired
     */
    public boolean waitForFlushAcknowledgement(String flushId, Duration timeout) {
        return flushListener.waitForFlush(flushId, timeout.toMillis());
    }

    public void waitUntilRenormaliserIsIdle() {
        renormaliser.waitUntilIdle();
    }

    static class Context {

        private final Logger jobLogger;
        private final boolean isPerPartitionNormalization;

        boolean deleteInterimRequired;

        Context(Logger jobLogger, boolean isPerPartitionNormalization) {
            this.jobLogger = jobLogger;
            this.isPerPartitionNormalization = isPerPartitionNormalization;
            this.deleteInterimRequired = true;
        }
    }

}

