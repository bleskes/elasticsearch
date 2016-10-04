
package org.elasticsearch.xpack.prelert.job.persistence;

import org.elasticsearch.client.Client;
import org.elasticsearch.search.SearchHit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.exceptions.CannotMapJobFromJson;

class ElasticsearchBatchedJobsIterator extends ElasticsearchBatchedDocumentsIterator<JobDetails>
{
    private final ElasticsearchJobDetailsMapper m_JobMapper;

    public ElasticsearchBatchedJobsIterator(Client client, String index,
            ObjectMapper objectMapper)
    {
        super(client, index, objectMapper);
        m_JobMapper = new ElasticsearchJobDetailsMapper(client, objectMapper);
    }

    @Override
    protected String getType()
    {
        return JobDetails.TYPE;
    }

    @Override
    protected JobDetails map(ObjectMapper objectMapper, SearchHit hit)
    {
        try
        {
            return m_JobMapper.map(hit.getSource());
        }
        catch (CannotMapJobFromJson e)
        {
            return null;
        }
    }
}
