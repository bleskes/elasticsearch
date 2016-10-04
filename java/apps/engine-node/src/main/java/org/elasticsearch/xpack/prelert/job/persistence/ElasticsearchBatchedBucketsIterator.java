
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
        return Bucket.TYPE;
    }

    @Override
    protected Bucket map(ObjectMapper objectMapper, SearchHit hit)
    {
        // Remove the Kibana/Logstash '@timestamp' entry as stored in Elasticsearch,
        // and replace using the API 'timestamp' key.
        Object timestamp = hit.getSource().remove(ElasticsearchMappings.ES_TIMESTAMP);
        hit.getSource().put(Bucket.TIMESTAMP, timestamp);

        Bucket bucket = objectMapper.convertValue(hit.getSource(), Bucket.class);
        bucket.setId(hit.getId());
        return bucket;
    }
}
