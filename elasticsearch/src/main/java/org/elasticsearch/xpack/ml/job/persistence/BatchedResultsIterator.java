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
package org.elasticsearch.xpack.ml.job.persistence;

import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.xpack.ml.job.results.Result;

public abstract class BatchedResultsIterator<T>
        extends BatchedDocumentsIterator<BatchedResultsIterator.ResultWithIndex<T>> {

    public BatchedResultsIterator(Client client, String jobId, String resultType) {
        super(client, AnomalyDetectorsIndex.jobResultsIndexName(jobId),
                new TermsQueryBuilder(Result.RESULT_TYPE.getPreferredName(), resultType));
    }

    @Override
    protected String getType() {
        return Result.TYPE.getPreferredName();
    }

    public static class ResultWithIndex<T> {
        public final String indexName;
        public final T result;

        public ResultWithIndex(String indexName, T result) {
            this.indexName = indexName;
            this.result = result;
        }
    }
}
