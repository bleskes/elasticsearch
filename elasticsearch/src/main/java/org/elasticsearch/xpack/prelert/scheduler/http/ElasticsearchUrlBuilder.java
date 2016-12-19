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
    private static final String INDEX_SETTINGS_END_POINT = "%s/_settings";
    private static final String SEARCH_SIZE_ONE_END_POINT = "_search?size=1";
    private static final String SEARCH_SCROLL_END_POINT = "_search?scroll=" + SCROLL_CONTEXT_MINUTES + "m&size=%d";
    private static final String CONTINUE_SCROLL_END_POINT = "_search/scroll?scroll=" + SCROLL_CONTEXT_MINUTES + "m";
    private static final String CLEAR_SCROLL_END_POINT = "_search/scroll";

    private final String baseUrl;
    private final String indexes;
    private final String types;

    private ElasticsearchUrlBuilder(String baseUrl, String indexes, String types) {
        this.baseUrl = Objects.requireNonNull(baseUrl);
        this.indexes = Objects.requireNonNull(indexes);
        this.types = Objects.requireNonNull(types);
    }

    public static ElasticsearchUrlBuilder create(List<String> indexes, List<String> types) {
        // norelease: This class will be removed once we switch to a client based data extractor
        return create(indexes, types, "http://localhost:9200/");
    }

    public static ElasticsearchUrlBuilder create(List<String> indexes, List<String> types, String baseUrl) {
        String indexesAsString = indexes.stream().collect(Collectors.joining(COMMA));
        String typesAsString = types.stream().collect(Collectors.joining(COMMA));
        return new ElasticsearchUrlBuilder(baseUrl, indexesAsString, typesAsString);
    }

    public String buildIndexSettingsUrl(String index) {
        return newUrlBuilder().append(String.format(Locale.ROOT, INDEX_SETTINGS_END_POINT, index)).toString();
    }

    public String buildSearchSizeOneUrl() {
        return buildUrlWithIndicesAndTypes().append(SEARCH_SIZE_ONE_END_POINT).toString();
    }

    public String buildInitScrollUrl(int scrollSize) {
        return buildUrlWithIndicesAndTypes()
                .append(String.format(Locale.ROOT, SEARCH_SCROLL_END_POINT, scrollSize))
                .toString();
    }

    public String buildContinueScrollUrl() {
        return newUrlBuilder().append(CONTINUE_SCROLL_END_POINT).toString();
    }

    public String buildClearScrollUrl() {
        return newUrlBuilder().append(CLEAR_SCROLL_END_POINT).toString();
    }

    private StringBuilder newUrlBuilder() {
        return new StringBuilder(baseUrl);
    }

    private StringBuilder buildUrlWithIndicesAndTypes() {
        StringBuilder urlBuilder = buildUrlWithIndices();
        if (!types.isEmpty()) {
            urlBuilder.append(types).append(SLASH);
        }
        return urlBuilder;
    }

    private StringBuilder buildUrlWithIndices() {
        return newUrlBuilder().append(indexes).append(SLASH);
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
