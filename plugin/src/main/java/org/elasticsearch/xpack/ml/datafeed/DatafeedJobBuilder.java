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
package org.elasticsearch.xpack.ml.datafeed;

import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.xpack.ml.action.util.QueryPage;
import org.elasticsearch.xpack.ml.datafeed.extractor.DataExtractorFactory;
import org.elasticsearch.xpack.ml.job.config.DataDescription;
import org.elasticsearch.xpack.ml.job.config.Job;
import org.elasticsearch.xpack.ml.job.persistence.BucketsQueryBuilder;
import org.elasticsearch.xpack.ml.job.persistence.JobProvider;
import org.elasticsearch.xpack.ml.job.process.autodetect.state.DataCounts;
import org.elasticsearch.xpack.ml.job.results.Bucket;
import org.elasticsearch.xpack.ml.job.results.Result;
import org.elasticsearch.xpack.ml.notifications.Auditor;

import java.time.Duration;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class DatafeedJobBuilder {

    private final Client client;
    private final JobProvider jobProvider;
    private final Auditor auditor;
    private final Supplier<Long> currentTimeSupplier;

    public DatafeedJobBuilder(Client client, JobProvider jobProvider, Auditor auditor, Supplier<Long> currentTimeSupplier) {
        this.client = Objects.requireNonNull(client);
        this.jobProvider = Objects.requireNonNull(jobProvider);
        this.auditor = Objects.requireNonNull(auditor);
        this.currentTimeSupplier = Objects.requireNonNull(currentTimeSupplier);
    }

    void build(Job job, DatafeedConfig datafeed, ActionListener<DatafeedJob> listener) {

        // Step 5. Build datafeed job object
        Consumer<Context> contextHanlder = context -> {
            Duration frequency = getFrequencyOrDefault(datafeed, job);
            Duration queryDelay = Duration.ofMillis(datafeed.getQueryDelay().millis());
            DatafeedJob datafeedJob = new DatafeedJob(job.getId(), buildDataDescription(job), frequency.toMillis(), queryDelay.toMillis(),
                    context.dataExtractorFactory, client, auditor, currentTimeSupplier,
                    context.latestFinalBucketEndMs, context.latestRecordTimeMs);
            listener.onResponse(datafeedJob);
        };

        final Context context = new Context();

        // Step 4. Context building complete - invoke final listener
        ActionListener<DataExtractorFactory> dataExtractorFactoryHandler = ActionListener.wrap(
                dataExtractorFactory -> {
                    context.dataExtractorFactory = dataExtractorFactory;
                    contextHanlder.accept(context);
                }, e -> {
                    auditor.error(job.getId(), e.getMessage());
                    listener.onFailure(e);
                }
        );

        // Step 3. Create data extractor factory
        Consumer<DataCounts> dataCountsHandler = dataCounts -> {
            if (dataCounts.getLatestRecordTimeStamp() != null) {
                context.latestRecordTimeMs = dataCounts.getLatestRecordTimeStamp().getTime();
            }
            DataExtractorFactory.create(client, datafeed, job, dataExtractorFactoryHandler);
        };

        // Step 2. Collect data counts
        Consumer<QueryPage<Bucket>> bucketsHandler = buckets -> {
            if (buckets.results().size() == 1) {
                TimeValue bucketSpan = job.getAnalysisConfig().getBucketSpan();
                context.latestFinalBucketEndMs = buckets.results().get(0).getTimestamp().getTime() + bucketSpan.millis() - 1;
            }
            jobProvider.dataCounts(job.getId(), dataCountsHandler, listener::onFailure);
        };

        // Step 1. Collect latest bucket
        BucketsQueryBuilder.BucketsQuery latestBucketQuery = new BucketsQueryBuilder()
                .sortField(Result.TIMESTAMP.getPreferredName())
                .sortDescending(true).size(1)
                .includeInterim(false)
                .build();
        jobProvider.bucketsViaInternalClient(job.getId(), latestBucketQuery, bucketsHandler, e -> {
            if (e instanceof ResourceNotFoundException) {
                QueryPage<Bucket> empty = new QueryPage<>(Collections.emptyList(), 0, Bucket.RESULT_TYPE_FIELD);
                bucketsHandler.accept(empty);
            } else {
                listener.onFailure(e);
            }
        });
    }

    private static Duration getFrequencyOrDefault(DatafeedConfig datafeed, Job job) {
        TimeValue frequency = datafeed.getFrequency();
        TimeValue bucketSpan = job.getAnalysisConfig().getBucketSpan();
        return frequency == null ? DefaultFrequency.ofBucketSpan(bucketSpan.seconds()) : Duration.ofSeconds(frequency.seconds());
    }

    private static DataDescription buildDataDescription(Job job) {
        DataDescription.Builder dataDescription = new DataDescription.Builder();
        dataDescription.setFormat(DataDescription.DataFormat.XCONTENT);
        if (job.getDataDescription() != null) {
            dataDescription.setTimeField(job.getDataDescription().getTimeField());
        }
        dataDescription.setTimeFormat(DataDescription.EPOCH_MS);
        return dataDescription.build();
    }

    private static class Context {
        volatile long latestFinalBucketEndMs = -1L;
        volatile long latestRecordTimeMs = -1L;
        volatile DataExtractorFactory dataExtractorFactory;
    }
}
