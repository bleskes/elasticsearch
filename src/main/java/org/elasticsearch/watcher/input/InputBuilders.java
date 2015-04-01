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
import org.elasticsearch.watcher.input.http.HttpInput;
import org.elasticsearch.watcher.input.search.SearchInput;
import org.elasticsearch.watcher.input.simple.SimpleInput;
import org.elasticsearch.watcher.support.http.TemplatedHttpRequest;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public final class InputBuilders {

    private InputBuilders() {
    }

    public static SearchInput.SourceBuilder searchInput(SearchRequest request) {
        return new SearchInput.SourceBuilder(request);
    }

    public static SearchInput.SourceBuilder searchInput(SearchRequestBuilder builder) {
        return searchInput(builder.request());
    }

    public static SimpleInput.SourceBuilder simpleInput() {
        return simpleInput(new HashMap<String, Object>());
    }

    public static SimpleInput.SourceBuilder simpleInput(Map<String, Object> data) {
        return new SimpleInput.SourceBuilder(data);
    }

    public static HttpInput.SourceBuilder httpInput(TemplatedHttpRequest.SourceBuilder requestSource) {
        return new HttpInput.SourceBuilder(requestSource);
    }
}
