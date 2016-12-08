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
package org.elasticsearch.xpack.prelert.job.process.autodetect.output;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.util.Supplier;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.xpack.prelert.job.ModelSizeStats;
import org.elasticsearch.xpack.prelert.job.ModelSnapshot;
import org.elasticsearch.xpack.prelert.job.persistence.JobResultsPersister;
import org.elasticsearch.xpack.prelert.job.process.normalizer.Renormaliser;
import org.elasticsearch.xpack.prelert.job.quantiles.Quantiles;
import org.elasticsearch.xpack.prelert.job.results.AnomalyRecord;
import org.elasticsearch.xpack.prelert.job.results.AutodetectResult;
import org.elasticsearch.xpack.prelert.job.results.Bucket;
import org.elasticsearch.xpack.prelert.job.results.CategoryDefinition;
import org.elasticsearch.xpack.prelert.job.results.Influencer;
import org.elasticsearch.xpack.prelert.job.results.ModelDebugOutput;
import org.elasticsearch.xpack.prelert.job.results.PerPartitionMaxProbabilities;

import java.io.InputStream;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

/**
 * A runnable class that reads the autodetect process output in the
 * {@link #process(String, InputStream, boolean)} method and persists parsed
 * results via the {@linkplain JobResultsPersister} passed in the constructor.
 * <p>
 * Has methods to register and remove alert observers.
 * Also has a method to wait for a flush to be complete.
 *
 * Buckets are the written last after records, influencers etc
 * when the end of bucket is reached. Therefore results aren't persisted
 * until the bucket is read, this means that interim results for all
 * result types can be safely deleted when the bucket is read and before
 * the new results are updated. This is specifically for the case where
 * a flush command is issued repeatedly in the same bucket to generate
 * interim results and the old interim results have to be cleared out
 * before the new ones are written.
 */
public class AutoDetectResultProcessor {

    private static final Logger LOGGER = Loggers.getLogger(AutoDetectResultProcessor.class);

    private final Renormaliser renormaliser;
    private final JobResultsPersister persister;
    private final AutodetectResultsParser parser;

    final CountDownLatch completionLatch = new CountDownLatch(1);
    private final FlushListener flushListener;

    private volatile ModelSizeStats latestModelSizeStats;

    public AutoDetectResultProcessor(Renormaliser renormaliser, JobResultsPersister persister, AutodetectResultsParser parser) {
        this(renormaliser, persister, parser, new FlushListener());
    }

    AutoDetectResultProcessor(Renormaliser renormaliser, JobResultsPersister persister, AutodetectResultsParser parser,
                              FlushListener flushListener) {
        this.renormaliser = renormaliser;
        this.persister = persister;
        this.parser = parser;
        this.flushListener = flushListener;
    }

    public void process(String jobId, InputStream in, boolean isPerPartitionNormalisation) {
        try (Stream<AutodetectResult> stream = parser.parseResults(in)) {
            int bucketCount = 0;
            Iterator<AutodetectResult> iterator = stream.iterator();
            Context context = new Context(jobId, isPerPartitionNormalisation, persister.bulkPersisterBuilder(jobId));
            while (iterator.hasNext()) {
                AutodetectResult result = iterator.next();
                processResult(context, result);
                if (result.getBucket() != null) {
                    bucketCount++;
                    LOGGER.trace("[{}] Bucket number {} parsed from output", jobId, bucketCount);
                }
            }
            LOGGER.info("[{}] {} buckets parsed from autodetect output - about to refresh indexes", jobId, bucketCount);
            LOGGER.info("[{}] Parse results Complete", jobId);
        } catch (Exception e) {
            LOGGER.error(new ParameterizedMessage("[{}] error parsing autodetect output", new Object[] {jobId}), e);
        } finally {
            completionLatch.countDown();
            flushListener.clear();
            renormaliser.shutdown();
        }
    }

    void processResult(Context context, AutodetectResult result) {
        Bucket bucket = result.getBucket();
        if (bucket != null) {
            if (context.deleteInterimRequired) {
                // Delete any existing interim results generated by a Flush command
                // which have not been replaced or superseded by new results.
                LOGGER.trace("[{}] Deleting interim results", context.jobId);
                persister.deleteInterimResults(context.jobId);
                context.deleteInterimRequired = false;
            }

            // persist after deleting interim results in case the new
            // results are also interim
            context.bulkResultsPersister.persistBucket(bucket).executeRequest();

            context.bulkResultsPersister = persister.bulkPersisterBuilder(context.jobId);

        }
        List<AnomalyRecord> records = result.getRecords();
        if (records != null && !records.isEmpty()) {
            context.bulkResultsPersister.persistRecords(records);
            if (context.isPerPartitionNormalization) {
                context.bulkResultsPersister.persistPerPartitionMaxProbabilities(new PerPartitionMaxProbabilities(records));
            }
        }
        List<Influencer> influencers = result.getInfluencers();
        if (influencers != null && !influencers.isEmpty()) {
            context.bulkResultsPersister.persistInfluencers(influencers);
        }
        CategoryDefinition categoryDefinition = result.getCategoryDefinition();
        if (categoryDefinition != null) {
            persister.persistCategoryDefinition(categoryDefinition);
        }
        ModelDebugOutput modelDebugOutput = result.getModelDebugOutput();
        if (modelDebugOutput != null) {
            persister.persistModelDebugOutput(modelDebugOutput);
        }
        ModelSizeStats modelSizeStats = result.getModelSizeStats();
        if (modelSizeStats != null) {
            LOGGER.trace("[{}] Parsed ModelSizeStats: {} / {} / {} / {} / {} / {}",
                    context.jobId, modelSizeStats.getModelBytes(), modelSizeStats.getTotalByFieldCount(),
                    modelSizeStats.getTotalOverFieldCount(), modelSizeStats.getTotalPartitionFieldCount(),
                    modelSizeStats.getBucketAllocationFailuresCount(), modelSizeStats.getMemoryStatus());

            latestModelSizeStats = modelSizeStats;
            persister.persistModelSizeStats(modelSizeStats);
        }
        ModelSnapshot modelSnapshot = result.getModelSnapshot();
        if (modelSnapshot != null) {
            persister.persistModelSnapshot(modelSnapshot);
        }
        Quantiles quantiles = result.getQuantiles();
        if (quantiles != null) {
            persister.persistQuantiles(quantiles);

            LOGGER.debug("[{}] Quantiles parsed from output - will " + "trigger renormalisation of scores", context.jobId);
            if (context.isPerPartitionNormalization) {
                renormaliser.renormaliseWithPartition(quantiles);
            } else {
                renormaliser.renormalise(quantiles);
            }
        }
        FlushAcknowledgement flushAcknowledgement = result.getFlushAcknowledgement();
        if (flushAcknowledgement != null) {
            LOGGER.debug("[{}] Flush acknowledgement parsed from output for ID {}", context.jobId, flushAcknowledgement.getId());
            // Commit previous writes here, effectively continuing
            // the flush from the C++ autodetect process right
            // through to the data store
            persister.commitWrites(context.jobId);
            flushListener.acknowledgeFlush(flushAcknowledgement.getId());
            // Interim results may have been produced by the flush,
            // which need to be
            // deleted when the next finalized results come through
            context.deleteInterimRequired = true;
        }
    }

    public void awaitCompletion() {
        try {
            completionLatch.await();
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

        private final String jobId;
        private final boolean isPerPartitionNormalization;
        private JobResultsPersister.Builder bulkResultsPersister;

        boolean deleteInterimRequired;

        Context(String jobId, boolean isPerPartitionNormalization, JobResultsPersister.Builder bulkResultsPersister) {
            this.jobId = jobId;
            this.isPerPartitionNormalization = isPerPartitionNormalization;
            this.deleteInterimRequired = true;
            this.bulkResultsPersister = bulkResultsPersister;
        }
    }

    public Optional<ModelSizeStats> modelSizeStats() {
        return Optional.ofNullable(latestModelSizeStats);
    }

}

