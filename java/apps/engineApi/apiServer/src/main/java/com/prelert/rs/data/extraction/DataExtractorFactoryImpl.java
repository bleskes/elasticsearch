package com.prelert.rs.data.extraction;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prelert.data.extractor.elasticsearch.ElasticsearchDataExtractor;
import com.prelert.job.JobDetails;
import com.prelert.job.SchedulerConfig;
import com.prelert.job.SchedulerConfig.DataSource;
import com.prelert.job.data.extraction.DataExtractor;
import com.prelert.job.data.extraction.DataExtractorFactory;

public class DataExtractorFactoryImpl implements DataExtractorFactory
{
    @Override
    public DataExtractor newExtractor(JobDetails job)
    {
        SchedulerConfig schedulerConfig = job.getSchedulerConfig();
        if (schedulerConfig.getDataSource() == DataSource.ELASTICSEARCH)
        {
            return createElasticsearchDataExtractor(job);
        }
        throw new IllegalArgumentException();
    }

    private DataExtractor createElasticsearchDataExtractor(JobDetails job)
    {
        String timeField = job.getDataDescription().getTimeField();
        SchedulerConfig schedulerConfig = job.getSchedulerConfig();
        return ElasticsearchDataExtractor.create(schedulerConfig.getBaseUrl(),
                schedulerConfig.getIndexes(), schedulerConfig.getTypes(),
                stringifyElasticsearchSearch(schedulerConfig.getSearch()), timeField);
    }

    private String stringifyElasticsearchSearch(Map<String, Object> search)
    {
        String query = search.containsKey("query") ? writeObjectAsJson(search.get("query"))
                : writeObjectAsJson(search);
        if (query.startsWith("{") && query.endsWith("}"))
        {
            return query.substring(1, query.length() - 1);
        }
        return query;
    }

    private static String writeObjectAsJson(Object obj)
    {
        ObjectMapper objectMapper = new ObjectMapper();
        try
        {
            return objectMapper.writeValueAsString(obj);
        }
        catch (JsonProcessingException e)
        {
            throw new IllegalStateException();
        }
    }

}
