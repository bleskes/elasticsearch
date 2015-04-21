/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated. All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elasticsearch Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Elasticsearch Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elasticsearch Incorporated.
 */

package org.elasticsearch.watcher.input;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.watcher.input.http.HttpInput;
import org.elasticsearch.watcher.input.none.NoneInput;
import org.elasticsearch.watcher.input.search.SearchInput;
import org.elasticsearch.watcher.input.simple.SimpleInput;
import org.elasticsearch.watcher.support.http.HttpRequestTemplate;
import org.elasticsearch.watcher.watch.Payload;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public final class InputBuilders {

    private InputBuilders() {
    }

    public static NoneInput.Builder noneInput() {
        return NoneInput.builder();
    }

    public static SearchInput.Builder searchInput(SearchRequest request) {
        return SearchInput.builder(request);
    }

    public static SearchInput.Builder searchInput(SearchRequestBuilder builder) {
        return searchInput(builder.request());
    }

    public static SimpleInput.Builder simpleInput() {
        return simpleInput(new HashMap<String, Object>());
    }

    public static SimpleInput.Builder simpleInput(String key, Object value) {
        return simpleInput(MapBuilder.<String, Object>newMapBuilder().put(key, value));
    }

    public static SimpleInput.Builder simpleInput(ImmutableMap.Builder<String, Object> data) {
        return simpleInput(data.build());
    }

    public static SimpleInput.Builder simpleInput(MapBuilder<String, Object> data) {
        return simpleInput(data.map());
    }

    public static SimpleInput.Builder simpleInput(Map<String, Object> data) {
        return SimpleInput.builder(new Payload.Simple(data));
    }

    public static HttpInput.Builder httpInput(HttpRequestTemplate.Builder request) {
        return httpInput(request.build());
    }

    public static HttpInput.Builder httpInput(HttpRequestTemplate request) {
        return HttpInput.builder(request);
    }
}
