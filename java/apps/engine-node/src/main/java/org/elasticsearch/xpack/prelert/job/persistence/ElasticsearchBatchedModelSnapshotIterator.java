
package org.elasticsearch.xpack.prelert.job.persistence;

import org.elasticsearch.client.Client;
import org.elasticsearch.search.SearchHit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.xpack.prelert.job.ModelSnapshot;

class ElasticsearchBatchedModelSnapshotIterator extends ElasticsearchBatchedDocumentsIterator<ModelSnapshot>
{
    public ElasticsearchBatchedModelSnapshotIterator(Client client, String jobId,
            ObjectMapper objectMapper)
    {
        super(client, new ElasticsearchJobId(jobId).getIndex(), objectMapper);
    }

    @Override
    protected String getType()
    {
        return ModelSnapshot.TYPE;
    }

    @Override
    protected ModelSnapshot map(ObjectMapper objectMapper, SearchHit hit)
    {
        // Remove the Kibana/Logstash '@timestamp' entry as stored in Elasticsearch,
        // and replace using the API 'timestamp' key.
        Object timestamp = hit.getSource().remove(ElasticsearchMappings.ES_TIMESTAMP);
        hit.getSource().put(ModelSnapshot.TIMESTAMP, timestamp);

        return objectMapper.convertValue(hit.getSource(), ModelSnapshot.class);
    }
}
