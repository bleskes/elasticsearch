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

package org.elasticsearch.alerts.trigger.search;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.alerts.Payload;
import org.elasticsearch.alerts.support.AlertUtils;
import org.elasticsearch.alerts.support.init.proxy.ClientProxy;
import org.elasticsearch.alerts.support.init.proxy.ScriptServiceProxy;
import org.elasticsearch.alerts.trigger.TriggerException;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.ScriptService;

import java.io.IOException;

/**
 *
 */
public class ScriptSearchTrigger extends SearchTrigger {

    public static final String TYPE = "script";

    private final String script;
    private final ScriptService.ScriptType scriptType;
    private final String scriptLang;

    public ScriptSearchTrigger(ESLogger logger, ScriptServiceProxy scriptService, ClientProxy client,
                               SearchRequest request, String script, ScriptService.ScriptType scriptType,
                               String scriptLang) {
        super(logger, scriptService, client, request);
        this.script = script;
        this.scriptType = scriptType;
        this.scriptLang = scriptLang;
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    protected Result processSearchResponse(SearchResponse response) {
        Payload payload = new Payload.ActionResponse(response);
        ExecutableScript executable = scriptService.executable(scriptLang, script, scriptType, payload.data());
        Object value = executable.run();
        if (value instanceof Boolean) {
            return new Result(TYPE, (Boolean) value, request, payload);
        }
        throw new TriggerException("trigger script [" + script + "] did not return a boolean value");
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, ToXContent.Params params) throws IOException {
        builder.startObject();
        builder.field("request");
        AlertUtils.writeSearchRequest(request, builder, params);
        builder.field("script", script);
        builder.field("script_type", scriptType);
        builder.field("script_lang", scriptLang);
        builder.endObject();
        return builder;
    }

    public static class Parser extends AbstractComponent implements SearchTrigger.Parser<ScriptSearchTrigger> {

        private final ClientProxy client;
        private final ScriptServiceProxy scriptService;

        @Inject
        public Parser(Settings settings, ClientProxy client, ScriptServiceProxy scriptService) {
            super(settings);
            this.client = client;
            this.scriptService = scriptService;
        }

        @Override
        public String type() {
            return TYPE;
        }

        @Override
        public ScriptSearchTrigger parse(XContentParser parser) throws IOException {

            SearchRequest request = null;
            String scriptLang = null;
            String script = null;
            ScriptService.ScriptType scriptType = ScriptService.ScriptType.INLINE;

            String currentFieldName = null;
            XContentParser.Token token;
            while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                if (token == XContentParser.Token.FIELD_NAME) {
                    currentFieldName = parser.currentName();
                } else if (token.isValue()) {
                    switch (currentFieldName) {
                        case "request":
                            request = AlertUtils.readSearchRequest(parser, AlertUtils.DEFAULT_TRIGGER_SEARCH_TYPE);
                        case "script_id" :
                            script = parser.text();
                            scriptType = ScriptService.ScriptType.INDEXED;
                            break;
                        case "script" :
                            script = parser.text();
                            break;
                        case "script_type" :
                            scriptType = ScriptService.ScriptType.valueOf(parser.text());
                            break;
                        case "script_lang" :
                            scriptLang = parser.text();
                            break;
                        default:
                            throw new TriggerException("could not parse script trigger. unexpected field [" + currentFieldName + "]");
                    }
                }
            }

            if (request == null) {
                throw new TriggerException("could not parse script trigger. missing required search request");
            }

            if (script == null) {
                throw new TriggerException("could not parse script trigger. either [script] or [script_id] must be provided");
            }

            return new ScriptSearchTrigger(logger, scriptService, client, request, script, scriptType, scriptLang);
        }
    }
}
