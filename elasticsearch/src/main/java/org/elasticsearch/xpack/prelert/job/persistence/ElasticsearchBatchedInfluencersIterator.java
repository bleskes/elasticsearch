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

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.search.SearchHit;

import java.io.IOException;

import org.elasticsearch.xpack.prelert.job.results.Influencer;

class ElasticsearchBatchedInfluencersIterator extends ElasticsearchBatchedDocumentsIterator<Influencer>
{
    public ElasticsearchBatchedInfluencersIterator(Client client, String jobId,
            ParseFieldMatcher parserFieldMatcher)
    {
        super(client, JobResultsPersister.getJobIndexName(jobId), parserFieldMatcher);
    }

    @Override
    protected String getType()
    {
        return Influencer.TYPE.getPreferredName();
    }

    @Override
    protected Influencer map(SearchHit hit)
    {
        BytesReference source = hit.getSourceRef();
        XContentParser parser;
        try {
            parser = XContentFactory.xContent(source).createParser(source);
        } catch (IOException e) {
            throw new ElasticsearchParseException("failed to parser influencer", e);
        }

        Influencer influencer = Influencer.PARSER.apply(parser, () -> parseFieldMatcher);
        influencer.setId(hit.getId());
        return influencer;
    }
}
