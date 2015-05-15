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

package org.elasticsearch.watcher.transform.search;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.watcher.execution.WatchExecutionContext;
import org.elasticsearch.watcher.support.DynamicIndexName;
import org.elasticsearch.watcher.support.WatcherUtils;
import org.elasticsearch.watcher.support.init.proxy.ClientProxy;
import org.elasticsearch.watcher.transform.ExecutableTransform;
import org.elasticsearch.watcher.watch.Payload;

/**
 *
 */
public class ExecutableSearchTransform extends ExecutableTransform<SearchTransform, SearchTransform.Result> {

    public static final SearchType DEFAULT_SEARCH_TYPE = SearchType.QUERY_THEN_FETCH;

    protected final ClientProxy client;
    private final @Nullable DynamicIndexName[] indexNames;

    public ExecutableSearchTransform(SearchTransform transform, ESLogger logger, ClientProxy client, DynamicIndexName.Parser indexNameParser) {
        super(transform, logger);
        this.client = client;
        String[] indices = transform.request.indices();
        this.indexNames = indices != null ? indexNameParser.parse(indices) : null;
    }

    DynamicIndexName[] indexNames() {
        return indexNames;
    }

    @Override
    public SearchTransform.Result execute(WatchExecutionContext ctx, Payload payload) {
        SearchRequest request = null;
        try {
            request = WatcherUtils.createSearchRequestFromPrototype(transform.request, indexNames, ctx, payload);
            SearchResponse resp = client.search(request);
            return new SearchTransform.Result(request, new Payload.XContent(resp));
        } catch (Exception e) {
            logger.error("failed to execute [{}] transform for [{}]", e, SearchTransform.TYPE, ctx.id());
            return new SearchTransform.Result(request, e);
        }
    }
}
