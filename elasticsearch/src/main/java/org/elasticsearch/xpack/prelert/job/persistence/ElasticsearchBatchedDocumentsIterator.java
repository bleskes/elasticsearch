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
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.NoSuchElementException;
import java.util.Objects;

abstract class ElasticsearchBatchedDocumentsIterator<T> implements BatchedDocumentsIterator<T> {
    private static final Logger LOGGER = Loggers.getLogger(ElasticsearchBatchedDocumentsIterator.class);

    private static final String CONTEXT_ALIVE_DURATION = "5m";
    private static final int BATCH_SIZE = 10000;

    private final Client client;
    private final String index;
    private final ResultsFilterBuilder filterBuilder;
    private volatile long count;
    private volatile long totalHits;
    private volatile String scrollId;
    private volatile boolean isScrollInitialised;
    protected ParseFieldMatcher parseFieldMatcher;

    public ElasticsearchBatchedDocumentsIterator(Client client, String index, ParseFieldMatcher parseFieldMatcher) {
        this.parseFieldMatcher = parseFieldMatcher;
        this.client = Objects.requireNonNull(client);
        this.index = Objects.requireNonNull(index);
        this.parseFieldMatcher = Objects.requireNonNull(parseFieldMatcher);
        totalHits = 0;
        count = 0;
        filterBuilder = new ResultsFilterBuilder();
        isScrollInitialised = false;
    }

    protected ElasticsearchBatchedDocumentsIterator(Client client, String index, ParseFieldMatcher parseFieldMatcher,
                                                    QueryBuilder queryBuilder) {
        this.parseFieldMatcher = parseFieldMatcher;
        this.client = Objects.requireNonNull(client);
        this.index = Objects.requireNonNull(index);
        this.parseFieldMatcher = Objects.requireNonNull(parseFieldMatcher);
        totalHits = 0;
        count = 0;
        filterBuilder = new ResultsFilterBuilder(queryBuilder);
        isScrollInitialised = false;
    }

    @Override
    public BatchedDocumentsIterator<T> timeRange(long startEpochMs, long endEpochMs) {
        filterBuilder.timeRange(Bucket.TIMESTAMP.getPreferredName(), startEpochMs, endEpochMs);
        return this;
    }

    @Override
    public BatchedDocumentsIterator<T> includeInterim(String interimFieldName) {
        filterBuilder.interim(interimFieldName, true);
        return this;
    }

    @Override
    public boolean hasNext() {
        return !isScrollInitialised || count != totalHits;
    }

    @Override
    public Deque<T> next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        SearchResponse searchResponse = (scrollId == null) ? initScroll()
                : client.prepareSearchScroll(scrollId).setScroll(CONTEXT_ALIVE_DURATION).get();
        scrollId = searchResponse.getScrollId();
        return mapHits(searchResponse);
    }

    private SearchResponse initScroll() {
        LOGGER.trace("ES API CALL: search all of type {} from index {}", getType(), index);

        isScrollInitialised = true;

        SearchResponse searchResponse = client.prepareSearch(index).setScroll(CONTEXT_ALIVE_DURATION).setSize(BATCH_SIZE)
                .setTypes(getType()).setQuery(filterBuilder.build()).addSort(SortBuilders.fieldSort(ElasticsearchMappings.ES_DOC)).get();
        totalHits = searchResponse.getHits().getTotalHits();
        scrollId = searchResponse.getScrollId();
        return searchResponse;
    }

    private Deque<T> mapHits(SearchResponse searchResponse) {
        Deque<T> results = new ArrayDeque<>();

        SearchHit[] hits = searchResponse.getHits().getHits();
        for (SearchHit hit : hits) {
            T mapped = map(hit);
            if (mapped != null) {
                results.add(mapped);
            }
        }
        count += hits.length;

        if (!hasNext() && scrollId != null) {
            client.prepareClearScroll().setScrollIds(Arrays.asList(scrollId)).get();
        }
        return results;
    }

    protected abstract String getType();

    /**
     * Maps the search hit to the document type
     * @param hit
     *            the search hit
     * @return The mapped document or {@code null} if the mapping failed
     */
    protected abstract T map(SearchHit hit);
}
