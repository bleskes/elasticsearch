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
import org.elasticsearch.xpack.prelert.job.results.Bucket;

class ElasticsearchBatchedBucketsIterator extends ElasticsearchBatchedResultsIterator<Bucket> {

    public ElasticsearchBatchedBucketsIterator(Client client, String jobId, ParseFieldMatcher parseFieldMatcher) {
        super(client, jobId, Bucket.RESULT_TYPE_VALUE, parseFieldMatcher);
    }

    @Override
    protected Bucket map(SearchHit hit) {
        BytesReference source = hit.getSourceRef();
        XContentParser parser;
        try {
            parser = XContentFactory.xContent(source).createParser(source);
        } catch (IOException e) {
            throw new ElasticsearchParseException("failed to parse bucket", e);
        }
        return Bucket.PARSER.apply(parser, () -> parseFieldMatcher);
    }
}
