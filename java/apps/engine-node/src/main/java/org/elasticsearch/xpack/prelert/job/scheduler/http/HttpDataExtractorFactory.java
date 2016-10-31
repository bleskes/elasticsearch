package org.elasticsearch.xpack.prelert.job.scheduler.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.SchedulerConfig;
import org.elasticsearch.xpack.prelert.job.extraction.DataExtractor;
import org.elasticsearch.xpack.prelert.job.extraction.DataExtractorFactory;

import java.util.Map;

public class HttpDataExtractorFactory implements DataExtractorFactory {

    public HttpDataExtractorFactory() {}

    @Override
    public DataExtractor newExtractor(JobDetails job) {
        SchedulerConfig schedulerConfig = job.getSchedulerConfig();
        if (schedulerConfig.getDataSource() == SchedulerConfig.DataSource.ELASTICSEARCH) {
            return createElasticsearchDataExtractor(job);
        }
        throw new IllegalArgumentException();
    }

    private DataExtractor createElasticsearchDataExtractor(JobDetails job) {
        String timeField = job.getDataDescription().getTimeField();
        SchedulerConfig schedulerConfig = job.getSchedulerConfig();
        ElasticsearchQueryBuilder queryBuilder = new ElasticsearchQueryBuilder(
                stringifyElasticsearchQuery(schedulerConfig.getQuery()),
                stringifyElasticsearchAggregations(schedulerConfig.getAggregations(), schedulerConfig.getAggs()),
                stringifyElasticsearchScriptFields(schedulerConfig.getScriptFields()),
                Boolean.TRUE.equals(schedulerConfig.getRetrieveWholeSource()) ? null : writeObjectAsJson(job.allFields()),
                timeField);
        HttpRequester httpRequester = new HttpRequester();
        ElasticsearchUrlBuilder urlBuilder = ElasticsearchUrlBuilder
                .create(schedulerConfig.getBaseUrl(), schedulerConfig.getIndexes(), schedulerConfig.getTypes());
        return new ElasticsearchDataExtractor(httpRequester, urlBuilder, queryBuilder, schedulerConfig.getScrollSize());
    }

    String stringifyElasticsearchQuery(Map<String, Object> queryMap) {
        String queryStr = writeObjectAsJson(queryMap);
        if (queryStr.startsWith("{") && queryStr.endsWith("}")) {
            return queryStr.substring(1, queryStr.length() - 1);
        }
        return queryStr;
    }

    String stringifyElasticsearchAggregations(Map<String, Object> aggregationsMap, Map<String, Object> aggsMap) {
        if (aggregationsMap != null) {
            return writeObjectAsJson(aggregationsMap);
        }
        if (aggsMap != null) {
            return writeObjectAsJson(aggsMap);
        }
        return null;
    }

    String stringifyElasticsearchScriptFields(Map<String, Object> scriptFieldsMap) {
        if (scriptFieldsMap != null) {
            return writeObjectAsJson(scriptFieldsMap);
        }
        return null;
    }

    private static String writeObjectAsJson(Object obj) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
