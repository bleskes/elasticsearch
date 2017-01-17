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
package org.elasticsearch.xpack.ml.job.persistence;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.xpack.ml.job.results.AnomalyRecord;

import java.io.IOException;

class ElasticsearchBatchedRecordsIterator extends ElasticsearchBatchedResultsIterator<AnomalyRecord> {

    public ElasticsearchBatchedRecordsIterator(Client client, String jobId) {
        super(client, jobId, AnomalyRecord.RESULT_TYPE_VALUE);
    }

    @Override
    protected ResultWithIndex<AnomalyRecord> map(SearchHit hit) {
        BytesReference source = hit.getSourceRef();
        XContentParser parser;
        try {
            parser = XContentFactory.xContent(source).createParser(NamedXContentRegistry.EMPTY, source);
        } catch (IOException e) {
            throw new ElasticsearchParseException("failed to parse record", e);
        }
        AnomalyRecord record = AnomalyRecord.PARSER.apply(parser, null);
        return new ResultWithIndex<>(hit.getIndex(), record);
    }
}