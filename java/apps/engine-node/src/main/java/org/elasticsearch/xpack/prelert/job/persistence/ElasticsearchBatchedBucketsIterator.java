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
import org.elasticsearch.search.SearchHit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.xpack.prelert.job.results.Bucket;

class ElasticsearchBatchedBucketsIterator extends ElasticsearchBatchedDocumentsIterator<Bucket>
{
    public ElasticsearchBatchedBucketsIterator(Client client, String jobId,
            ObjectMapper objectMapper)
    {
        super(client, new ElasticsearchJobId(jobId).getIndex(), objectMapper);
    }

    @Override
    protected String getType()
    {
        return Bucket.TYPE.getPreferredName();
    }

    @Override
    protected Bucket map(ObjectMapper objectMapper, SearchHit hit)
    {
        // Remove the Kibana/Logstash '@timestamp' entry as stored in Elasticsearch,
        // and replace using the API 'timestamp' key.
        Object timestamp = hit.getSource().remove(ElasticsearchMappings.ES_TIMESTAMP);
        hit.getSource().put(Bucket.TIMESTAMP.getPreferredName(), timestamp);

        Bucket bucket = objectMapper.convertValue(hit.getSource(), Bucket.class);
        bucket.setId(hit.getId());
        return bucket;
    }
}
