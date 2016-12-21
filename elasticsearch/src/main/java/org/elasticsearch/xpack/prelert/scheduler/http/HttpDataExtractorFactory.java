/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
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
package org.elasticsearch.xpack.prelert.scheduler.http;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.search.SearchRequestParsers;
import org.elasticsearch.search.aggregations.AggregatorFactories;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.xpack.prelert.job.Job;
import org.elasticsearch.xpack.prelert.job.extraction.DataExtractor;
import org.elasticsearch.xpack.prelert.job.extraction.DataExtractorFactory;
import org.elasticsearch.xpack.prelert.scheduler.SchedulerConfig;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HttpDataExtractorFactory implements DataExtractorFactory {

    private static final Logger LOGGER = Loggers.getLogger(HttpDataExtractorFactory.class);

    private final Client client;
    private final SearchRequestParsers searchRequestParsers;

    public HttpDataExtractorFactory(Client client, SearchRequestParsers searchRequestParsers) {
        this.client = Objects.requireNonNull(client);
        this.searchRequestParsers = Objects.requireNonNull(searchRequestParsers);
    }

    @Override
    public DataExtractor newExtractor(SchedulerConfig schedulerConfig, Job job) {
        String timeField = job.getDataDescription().getTimeField();
        ElasticsearchQueryBuilder queryBuilder = new ElasticsearchQueryBuilder(
                xContentToJson(schedulerConfig.buildQuery(searchRequestParsers.queryParsers)),
                stringifyAggregations(schedulerConfig.buildAggregations(searchRequestParsers.queryParsers,
                        searchRequestParsers.aggParsers)),
                stringifyScriptFields(schedulerConfig.buildScriptFields(searchRequestParsers.queryParsers)),
                timeField);
        HttpRequester httpRequester = new HttpRequester();
        ElasticsearchUrlBuilder urlBuilder = ElasticsearchUrlBuilder
                .create(schedulerConfig.getIndexes(), schedulerConfig.getTypes(), getBaseUrl());
        return new ElasticsearchDataExtractor(httpRequester, urlBuilder, queryBuilder, schedulerConfig.getScrollSize());
    }

    private String getBaseUrl() {
        NodesInfoResponse nodesInfoResponse = client.admin().cluster().prepareNodesInfo().get();
        TransportAddress address = nodesInfoResponse.getNodes().get(0).getHttp().getAddress().publishAddress();
        String baseUrl = "http://" + address.getAddress() + ":" + address.getPort() + "/";
        LOGGER.info("Base URL: " + baseUrl);
        return baseUrl;
    }

    private String xContentToJson(ToXContent xContent) {
        try {
            XContentBuilder jsonBuilder = JsonXContent.contentBuilder();
            xContent.toXContent(jsonBuilder, ToXContent.EMPTY_PARAMS);
            return jsonBuilder.string();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    String stringifyAggregations(AggregatorFactories.Builder aggregations) {
        if (aggregations == null) {
            return null;
        }
        return xContentToJson(aggregations);
    }

    String stringifyScriptFields(List<SearchSourceBuilder.ScriptField> scriptFields) {
        if (scriptFields.isEmpty()) {
            return null;
        }
        try {
            XContentBuilder jsonBuilder = JsonXContent.contentBuilder();
            jsonBuilder.startObject();
            for (SearchSourceBuilder.ScriptField scriptField : scriptFields) {
                scriptField.toXContent(jsonBuilder, ToXContent.EMPTY_PARAMS);
            }
            jsonBuilder.endObject();
            return jsonBuilder.string();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String writeMapAsJson(Map<String, Object> map) {
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            builder.map(map);
            return builder.string();
        } catch (IOException e) {
            throw new ElasticsearchParseException("failed to convert map to JSON string", e);
        }
    }
}
