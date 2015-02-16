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

package org.elasticsearch.alerts.transform;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.alerts.ExecutionContext;
import org.elasticsearch.alerts.Payload;
import org.elasticsearch.alerts.support.AlertUtils;
import org.elasticsearch.alerts.support.init.proxy.ClientProxy;
import org.elasticsearch.alerts.support.init.proxy.ScriptServiceProxy;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.ScriptService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.elasticsearch.alerts.support.AlertsDateUtils.formatDate;

/**
 *
 */
public class SearchTransform implements Transform {

    public static final String TYPE = "search";

    protected final ESLogger logger;
    protected final ScriptServiceProxy scriptService;
    protected final ClientProxy client;
    protected final SearchRequest request;

    public SearchTransform(ESLogger logger, ScriptServiceProxy scriptService, ClientProxy client, SearchRequest request) {
        this.logger = logger;
        this.scriptService = scriptService;
        this.client = client;
        this.request = request;
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Transform.Result apply(ExecutionContext ctx, Payload payload) throws IOException {
        SearchRequest req = createRequest(request, ctx.scheduledTime(), ctx.fireTime(), payload.data());
        SearchResponse resp = client.search(req).actionGet();
        return new Transform.Result(TYPE, new Payload.ActionResponse(resp));
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, ToXContent.Params params) throws IOException {
        AlertUtils.writeSearchRequest(request, builder, params);
        return builder;
    }

    public SearchRequest createRequest(SearchRequest request, DateTime scheduledFireTime, DateTime fireTime, Map<String, Object> data) throws IOException {
        SearchRequest triggerSearchRequest = new SearchRequest(request)
                .indicesOptions(request.indicesOptions())
                .indices(request.indices());
        if (Strings.hasLength(request.source())) {
            Map<String, String> templateParams = new HashMap<>();
            templateParams.put(AlertUtils.SCHEDULED_FIRE_TIME_VARIABLE_NAME, formatDate(scheduledFireTime));
            templateParams.put(AlertUtils.FIRE_TIME_VARIABLE_NAME, formatDate(fireTime));
            String requestSource = XContentHelper.convertToJson(request.source(), false);
            ExecutableScript script = scriptService.executable("mustache", requestSource, ScriptService.ScriptType.INLINE, templateParams);
            triggerSearchRequest.source((BytesReference) script.unwrap(script.run()), false);
        } else if (request.templateName() != null) {
            MapBuilder<String, String> templateParams = MapBuilder.newMapBuilder(request.templateParams())
                    .put(AlertUtils.SCHEDULED_FIRE_TIME_VARIABLE_NAME, formatDate(scheduledFireTime))
                    .put(AlertUtils.FIRE_TIME_VARIABLE_NAME, formatDate(fireTime));
            triggerSearchRequest.templateParams(templateParams.map());
            triggerSearchRequest.templateName(request.templateName());
            triggerSearchRequest.templateType(request.templateType());
        } else {
            throw new TransformException("search requests needs either source or template name");
        }
        return triggerSearchRequest;
    }

    public static class Parser extends AbstractComponent implements Transform.Parser<SearchTransform> {

        protected final ScriptServiceProxy scriptService;
        protected final ClientProxy client;

        @Inject
        public Parser(Settings settings, ScriptServiceProxy scriptService, ClientProxy client) {
            super(settings);
            this.scriptService = scriptService;
            this.client = client;
        }

        @Override
        public String type() {
            return TYPE;
        }

        @Override
        public SearchTransform parse(XContentParser parser) throws IOException {
            SearchRequest request = AlertUtils.readSearchRequest(parser, AlertUtils.DEFAULT_PAYLOAD_SEARCH_TYPE);
            return new SearchTransform(logger, scriptService, client, request);
        }
    }

}
