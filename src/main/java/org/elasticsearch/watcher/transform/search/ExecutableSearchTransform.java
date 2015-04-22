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
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.watcher.execution.WatchExecutionContext;
import org.elasticsearch.watcher.support.init.proxy.ClientProxy;
import org.elasticsearch.watcher.support.init.proxy.ScriptServiceProxy;
import org.elasticsearch.watcher.transform.ExecutableTransform;
import org.elasticsearch.watcher.transform.TransformException;
import org.elasticsearch.watcher.watch.Payload;

import java.io.IOException;

import static org.elasticsearch.watcher.support.Variables.createCtxModel;
import static org.elasticsearch.watcher.support.WatcherUtils.flattenModel;

/**
 *
 */
public class ExecutableSearchTransform extends ExecutableTransform<SearchTransform, SearchTransform.Result> {

    public static final SearchType DEFAULT_SEARCH_TYPE = SearchType.QUERY_THEN_FETCH;

    protected final ScriptServiceProxy scriptService;
    protected final ClientProxy client;

    public ExecutableSearchTransform(SearchTransform transform, ESLogger logger, ScriptServiceProxy scriptService, ClientProxy client) {
        super(transform, logger);
        this.scriptService  = scriptService;
        this.client = client;
    }

    @Override
    public SearchTransform.Result execute(WatchExecutionContext ctx, Payload payload) throws IOException {
        SearchRequest req = createRequest(transform.request, ctx, payload);
        SearchResponse resp = client.search(req);
        return new SearchTransform.Result(new Payload.XContent(resp));
    }

    SearchRequest createRequest(SearchRequest requestPrototype, WatchExecutionContext ctx, Payload payload) throws IOException {
        SearchRequest request = new SearchRequest(requestPrototype)
                .indicesOptions(requestPrototype.indicesOptions())
                .indices(requestPrototype.indices());
        if (Strings.hasLength(requestPrototype.source())) {
            String requestSource = XContentHelper.convertToJson(requestPrototype.source(), false);
            ExecutableScript script = scriptService.executable("mustache", requestSource, ScriptService.ScriptType.INLINE, createCtxModel(ctx, payload));
            request.source((BytesReference) script.unwrap(script.run()), false);
        } else if (requestPrototype.templateName() != null) {
            MapBuilder<String, Object> templateParams = MapBuilder.newMapBuilder(requestPrototype.templateParams())
                    .putAll(flattenModel(createCtxModel(ctx, payload)));
            request.templateParams(templateParams.map());
            request.templateName(requestPrototype.templateName());
            request.templateType(requestPrototype.templateType());
        } else {
            throw new TransformException("search requests needs either source or template name");
        }
        return request;
    }

}
