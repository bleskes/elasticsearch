
package org.elasticsearch.xpack.prelert.job.persistence;

import org.elasticsearch.client.Client;
import org.elasticsearch.search.SearchHit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.xpack.prelert.job.results.Bucket;
import org.elasticsearch.xpack.prelert.job.results.Influencer;

class ElasticsearchBatchedInfluencersIterator extends ElasticsearchBatchedDocumentsIterator<Influencer>
{
    public ElasticsearchBatchedInfluencersIterator(Client client, String jobId,
            ObjectMapper objectMapper)
    {
        super(client, new ElasticsearchJobId(jobId).getIndex(), objectMapper);
    }

    @Override
    protected String getType()
    {
        return Influencer.TYPE.getPreferredName();
    }

    @Override
    protected Influencer map(ObjectMapper objectMapper, SearchHit hit)
    {
        // Remove the Kibana/Logstash '@timestamp' entry as stored in Elasticsearch,
        // and replace using the API 'timestamp' key.
        Object timestamp = hit.getSource().remove(ElasticsearchMappings.ES_TIMESTAMP);
        hit.getSource().put(Bucket.TIMESTAMP.getPreferredName(), timestamp);

        Influencer influencer = objectMapper.convertValue(hit.getSource(), Influencer.class);
        influencer.setId(hit.getId());
        return influencer;
    }
}
