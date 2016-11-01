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
package org.elasticsearch.xpack.prelert.job.persistence;

import java.util.Date;
import java.util.Deque;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A class that removes results from all the jobs that
 * have expired their respected retention time.
 */
public class OldDataRemover {

    private final JobProvider jobProvider;
    private final Function<String, ElasticsearchBulkDeleter> dataDeleterFactory;

    public OldDataRemover(JobProvider jobProvider, Function<String, ElasticsearchBulkDeleter> dataDeleterFactory) {
        this.jobProvider = Objects.requireNonNull(jobProvider);
        this.dataDeleterFactory = Objects.requireNonNull(dataDeleterFactory);
    }

    /**
     * Removes results between the time given and the current time
     */
    public void deleteResultsAfter(String jobId, long cutoffEpochMs) {
        Date now = new Date();
        JobDataDeleter deleter = dataDeleterFactory.apply(jobId);
        deleteResultsWithinRange(jobId, deleter, cutoffEpochMs, now.getTime());
        deleter.commitAndFreeDiskSpace();
    }

    private void deleteResultsWithinRange(String jobId, JobDataDeleter deleter, long start, long end) {
        deleteBatchedData(
                jobProvider.newBatchedInfluencersIterator(jobId).timeRange(start, end),
                deleter::deleteInfluencer
                );
        deleteBatchedData(
                jobProvider.newBatchedBucketsIterator(jobId).timeRange(start, end),
                deleter::deleteBucket
                );
    }

    private <T> void deleteBatchedData(BatchedDocumentsIterator<T> resultsIterator,
            Consumer<T> deleteFunction) {
        while (resultsIterator.hasNext()) {
            Deque<T> batch = resultsIterator.next();
            if (batch.isEmpty()) {
                return;
            }
            for (T result : batch) {
                deleteFunction.accept(result);
            }
        }
    }

}
