/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated. All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elasticsearch Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Elasticsearch Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elasticsearch Incorporated.
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
