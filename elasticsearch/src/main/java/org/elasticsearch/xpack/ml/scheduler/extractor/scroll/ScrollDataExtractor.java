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
package org.elasticsearch.xpack.ml.scheduler.extractor.scroll;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.search.ClearScrollAction;
import org.elasticsearch.action.search.SearchAction;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollAction;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.PipelineAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.xpack.ml.scheduler.extractor.DataExtractor;
import org.elasticsearch.xpack.ml.scheduler.extractor.SearchHitFieldExtractor;
import org.elasticsearch.xpack.ml.scheduler.extractor.SearchHitToJsonProcessor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * An implementation that extracts data from elasticsearch using search and scroll on a client.
 * It supports safe and responsive cancellation by continuing the scroll until a new timestamp
 * is seen.
 * Note that this class is NOT thread-safe.
 */
class ScrollDataExtractor implements DataExtractor {

    private static final Logger LOGGER = Loggers.getLogger(ScrollDataExtractor.class);
    private static final TimeValue SCROLL_TIMEOUT = new TimeValue(10, TimeUnit.MINUTES);

    private final Client client;
    private final ScrollDataExtractorContext context;
    private String scrollId;
    private boolean isCancelled;
    private boolean hasNext;
    private Long timestampOnCancel;

    public ScrollDataExtractor(Client client, ScrollDataExtractorContext dataExtractorContext) {
        this.client = Objects.requireNonNull(client);
        this.context = Objects.requireNonNull(dataExtractorContext);
        this.hasNext = true;
    }

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void cancel() {
        LOGGER.trace("[{}] Data extractor received cancel request", context.jobId);
        isCancelled = true;
    }

    @Override
    public Optional<InputStream> next() throws IOException {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        Optional<InputStream> stream = scrollId == null ? Optional.ofNullable(initScroll()) : Optional.ofNullable(continueScroll());
        if (!stream.isPresent()) {
            hasNext = false;
        }
        return stream;
    }

    private InputStream initScroll() throws IOException {
        SearchResponse searchResponse = executeSearchRequest(buildSearchRequest());
        if (searchResponse.status() != RestStatus.OK) {
            throw new IOException("[" + context.jobId + "] Search request returned status code: " + searchResponse.status()
                    + ". Response was:\n" + searchResponse.toString());
        }
        return processSearchResponse(searchResponse);
    }

    protected SearchResponse executeSearchRequest(SearchRequestBuilder searchRequestBuilder) {
        return searchRequestBuilder.get();
    }

    private SearchRequestBuilder buildSearchRequest() {
        SearchRequestBuilder searchRequestBuilder = SearchAction.INSTANCE.newRequestBuilder(client)
                .setScroll(SCROLL_TIMEOUT)
                .addSort(context.timeField, SortOrder.ASC)
                .setIndices(context.indexes)
                .setTypes(context.types)
                .setSize(context.scrollSize)
                .setQuery(createQuery());
        if (context.aggregations != null) {
            searchRequestBuilder.setSize(0);
            for (AggregationBuilder aggregationBuilder : context.aggregations.getAggregatorFactories()) {
                searchRequestBuilder.addAggregation(aggregationBuilder);
            }
            for (PipelineAggregationBuilder pipelineAggregationBuilder : context.aggregations.getPipelineAggregatorFactories()) {
                searchRequestBuilder.addAggregation(pipelineAggregationBuilder);
            }
        }
        for (SearchSourceBuilder.ScriptField scriptField : context.scriptFields) {
            searchRequestBuilder.addScriptField(scriptField.fieldName(), scriptField.script());
        }
        return searchRequestBuilder;
    }

    private InputStream processSearchResponse(SearchResponse searchResponse) throws IOException {
        scrollId = searchResponse.getScrollId();
        if (searchResponse.getHits().hits().length == 0) {
            hasNext = false;
            return null;
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (SearchHitToJsonProcessor hitProcessor = new SearchHitToJsonProcessor(context.jobFields, outputStream)) {
            for (SearchHit hit : searchResponse.getHits().hits()) {
                if (isCancelled) {
                    Long timestamp = SearchHitFieldExtractor.extractTimeField(hit, context.timeField);
                    if (timestamp != null) {
                        if (timestampOnCancel == null) {
                            timestampOnCancel = timestamp;
                        } else if (timestamp != timestampOnCancel) {
                            hasNext = false;
                            clearScroll(scrollId);
                            break;
                        }
                    }
                }
                hitProcessor.process(hit);
            }
        }
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    private InputStream continueScroll() throws IOException {
        SearchResponse searchResponse = executeSearchScrollRequest(scrollId);
        if (searchResponse.status() != RestStatus.OK) {
            throw new IOException("[" + context.jobId + "] Continue search scroll request with id '" + scrollId + "' returned status code: "
                    + searchResponse.status() + ". Response was:\n" + searchResponse.toString());
        }
        return processSearchResponse(searchResponse);
    }

    protected SearchResponse executeSearchScrollRequest(String scrollId) {
        return SearchScrollAction.INSTANCE.newRequestBuilder(client)
                .setScroll(SCROLL_TIMEOUT)
                .setScrollId(scrollId)
                .get();
    }

    private QueryBuilder createQuery() {
        QueryBuilder userQuery = context.query;
        QueryBuilder timeQuery = new RangeQueryBuilder(context.timeField).gte(context.start).lt(context.end).format("epoch_millis");
        return new BoolQueryBuilder().filter(userQuery).filter(timeQuery);
    }

    void clearScroll(String scrollId) {
        ClearScrollAction.INSTANCE.newRequestBuilder(client).addScrollId(scrollId).get();
    }
}
