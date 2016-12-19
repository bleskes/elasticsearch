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
package org.elasticsearch.xpack.prelert.job.persistence;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.xpack.prelert.job.results.Result;

abstract class ElasticsearchBatchedResultsIterator<T> extends ElasticsearchBatchedDocumentsIterator<T> {

    public ElasticsearchBatchedResultsIterator(Client client, String jobId, String resultType, ParseFieldMatcher parseFieldMatcher) {
        super(client, AnomalyDetectorsIndex.getJobIndexName(jobId), parseFieldMatcher,
                new TermsQueryBuilder(Result.RESULT_TYPE.getPreferredName(), resultType));
    }

    @Override
    protected String getType() {
        return Result.TYPE.getPreferredName();
    }
}
