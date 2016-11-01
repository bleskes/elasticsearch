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
import org.elasticsearch.xpack.prelert.job.ModelSizeStats;

public class ElasticsearchBatchedModelSizeStatsIterator extends ElasticsearchBatchedDocumentsIterator<ModelSizeStats>
{
    public ElasticsearchBatchedModelSizeStatsIterator(Client client, String jobId,
            ObjectMapper objectMapper)
    {
        super(client, new ElasticsearchJobId(jobId).getIndex(), objectMapper);
    }

    @Override
    protected String getType()
    {
        return ModelSizeStats.TYPE.getPreferredName();
    }

    @Override
    protected ModelSizeStats map(ObjectMapper objectMapper, SearchHit hit)
    {
        // Remove the Kibana/Logstash '@timestamp' entry as stored in Elasticsearch,
        // and replace using the API 'timestamp' key.
        Object timestamp = hit.getSource().remove(ElasticsearchMappings.ES_TIMESTAMP);
        hit.getSource().put(ModelSizeStats.TIMESTAMP_FIELD.getPreferredName(), timestamp);

        ModelSizeStats result = objectMapper.convertValue(hit.getSource(), ModelSizeStats.class);
        result.setModelSizeStatsId(hit.getId());
        return result;
    }
}
