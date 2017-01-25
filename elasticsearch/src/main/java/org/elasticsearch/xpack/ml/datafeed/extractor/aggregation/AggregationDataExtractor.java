/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2017 Elasticsearch BV. All Rights Reserved.
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
package org.elasticsearch.xpack.ml.datafeed.extractor.aggregation;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.search.SearchAction;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.xpack.ml.datafeed.extractor.DataExtractor;
import org.elasticsearch.xpack.ml.datafeed.extractor.ExtractorUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

/**
 * An implementation that extracts data from elasticsearch using search with aggregations on a client.
 * Cancellation is effective only when it is called before the first time {@link #next()} is called.
 * Note that this class is NOT thread-safe.
 */
class AggregationDataExtractor implements DataExtractor {

    private static final Logger LOGGER = Loggers.getLogger(AggregationDataExtractor.class);

    private final Client client;
    private final AggregationDataExtractorContext context;
    private boolean hasNext;
    private boolean isCancelled;

    public AggregationDataExtractor(Client client, AggregationDataExtractorContext dataExtractorContext) {
        this.client = Objects.requireNonNull(client);
        this.context = Objects.requireNonNull(dataExtractorContext);
        this.hasNext = true;
    }

    @Override
    public boolean hasNext() {
        return hasNext && !isCancelled;
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
        Optional<InputStream> stream = Optional.ofNullable(search());
        hasNext = false;
        return stream;
    }

    private InputStream search() throws IOException {
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
                .addSort(context.timeField, SortOrder.ASC)
                .setIndices(context.indexes)
                .setTypes(context.types)
                .setSize(0)
                .setQuery(ExtractorUtils.wrapInTimeRangeQuery(context.query, context.timeField, context.start, context.end));

        context.aggs.getAggregatorFactories().forEach(a -> searchRequestBuilder.addAggregation(a));
        context.aggs.getPipelineAggregatorFactories().forEach(a -> searchRequestBuilder.addAggregation(a));
        return searchRequestBuilder;
    }

    private InputStream processSearchResponse(SearchResponse searchResponse) throws IOException {
        if (searchResponse.getAggregations() == null) {
            return null;
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (AggregationToJsonProcessor processor = new AggregationToJsonProcessor(outputStream)) {
            for (Aggregation agg : searchResponse.getAggregations().asList()) {
                processor.process(agg);
            }
        }
        return new ByteArrayInputStream(outputStream.toByteArray());
    }
}
