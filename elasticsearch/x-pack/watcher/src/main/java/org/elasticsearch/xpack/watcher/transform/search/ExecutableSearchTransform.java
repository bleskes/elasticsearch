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

package org.elasticsearch.xpack.watcher.transform.search;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.script.Script;
import org.elasticsearch.xpack.watcher.execution.WatchExecutionContext;
import org.elasticsearch.xpack.watcher.support.init.proxy.WatcherClientProxy;
import org.elasticsearch.xpack.watcher.support.search.WatcherSearchTemplateRequest;
import org.elasticsearch.xpack.watcher.support.search.WatcherSearchTemplateService;
import org.elasticsearch.xpack.watcher.transform.ExecutableTransform;
import org.elasticsearch.xpack.watcher.watch.Payload;

import static org.elasticsearch.xpack.watcher.transform.search.SearchTransform.TYPE;

/**
 *
 */
public class ExecutableSearchTransform extends ExecutableTransform<SearchTransform, SearchTransform.Result> {

    public static final SearchType DEFAULT_SEARCH_TYPE = SearchType.QUERY_THEN_FETCH;

    protected final WatcherClientProxy client;
    private final WatcherSearchTemplateService searchTemplateService;
    @Nullable protected final TimeValue timeout;

    public ExecutableSearchTransform(SearchTransform transform, Logger logger, WatcherClientProxy client,
                                     WatcherSearchTemplateService searchTemplateService, @Nullable TimeValue defaultTimeout) {
        super(transform, logger);
        this.client = client;
        this.searchTemplateService = searchTemplateService;
        this.timeout = transform.getTimeout() != null ? transform.getTimeout() : defaultTimeout;
    }

    @Override
    public SearchTransform.Result execute(WatchExecutionContext ctx, Payload payload) {
        WatcherSearchTemplateRequest request = null;
        try {
            Script template = transform.getRequest().getOrCreateTemplate();
            BytesReference renderedTemplate = searchTemplateService.renderTemplate(template, ctx, payload);
            // We need to make a copy, so that we don't modify the original instance that we keep around in a watch:
            request = new WatcherSearchTemplateRequest(transform.getRequest(), renderedTemplate);
            SearchResponse resp = client.search(searchTemplateService.toSearchRequest(request), timeout);
            return new SearchTransform.Result(request, new Payload.XContent(resp));
        } catch (Exception e) {
            logger.error(new ParameterizedMessage("failed to execute [{}] transform for [{}]", TYPE, ctx.id()), e);
            return new SearchTransform.Result(request, e);
        }
    }
}
