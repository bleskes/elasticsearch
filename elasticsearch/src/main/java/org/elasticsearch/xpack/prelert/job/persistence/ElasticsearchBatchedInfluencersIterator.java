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
import org.elasticsearch.xpack.prelert.job.results.Influencer;

class ElasticsearchBatchedInfluencersIterator extends ElasticsearchBatchedResultsIterator<Influencer> {
    public ElasticsearchBatchedInfluencersIterator(Client client, String jobId,
                                                   ParseFieldMatcher parserFieldMatcher) {
        super(client, jobId, Influencer.RESULT_TYPE_VALUE, parserFieldMatcher);
    }

    @Override
    protected Influencer map(SearchHit hit) {
        BytesReference source = hit.getSourceRef();
        XContentParser parser;
        try {
            parser = XContentFactory.xContent(source).createParser(source);
        } catch (IOException e) {
            throw new ElasticsearchParseException("failed to parser influencer", e);
        }

        return Influencer.PARSER.apply(parser, () -> parseFieldMatcher);
    }
}
