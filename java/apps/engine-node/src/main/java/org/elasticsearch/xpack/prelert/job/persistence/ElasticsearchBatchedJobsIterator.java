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

import org.elasticsearch.client.Client;
import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.search.SearchHit;

import org.elasticsearch.xpack.prelert.job.JobDetails;

class ElasticsearchBatchedJobsIterator extends ElasticsearchBatchedDocumentsIterator<JobDetails>
{
    private final ElasticsearchJobDetailsMapper jobMapper;

    public ElasticsearchBatchedJobsIterator(Client client, String index,
            ParseFieldMatcher parserFieldMatcher)
    {
        super(client, index, parserFieldMatcher);
        jobMapper = new ElasticsearchJobDetailsMapper(client, parserFieldMatcher);
    }

    @Override
    protected String getType()
    {
        return JobDetails.TYPE;
    }

    @Override
    protected JobDetails map(SearchHit hit)
    {
        return jobMapper.map(hit.getSourceRef());
    }
}
