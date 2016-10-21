
package org.elasticsearch.xpack.prelert.job.persistence;

import org.elasticsearch.client.Client;
import org.elasticsearch.search.SearchHit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.xpack.prelert.job.results.ModelDebugOutput;

class ElasticsearchBatchedModelDebugOutputIterator extends ElasticsearchBatchedDocumentsIterator<ModelDebugOutput>
{
    public ElasticsearchBatchedModelDebugOutputIterator(Client client, String jobId,
            ObjectMapper objectMapper)
    {
        super(client, new ElasticsearchJobId(jobId).getIndex(), objectMapper);
    }

    @Override
    protected String getType()
    {
        return ModelDebugOutput.TYPE.getPreferredName();
    }

    @Override
    protected ModelDebugOutput map(ObjectMapper objectMapper, SearchHit hit)
    {
        // Remove the Kibana/Logstash '@timestamp' entry as stored in Elasticsearch,
        // and replace using the API 'timestamp' key.
        Object timestamp = hit.getSource().remove(ElasticsearchMappings.ES_TIMESTAMP);
        hit.getSource().put(ModelDebugOutput.TIMESTAMP.getPreferredName(), timestamp);
        ModelDebugOutput result = objectMapper.convertValue(hit.getSource(), ModelDebugOutput.class);
        result.setId(hit.getId());
        return result;
    }
}
