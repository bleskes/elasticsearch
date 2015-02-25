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

import org.elasticsearch.alerts.AlertsSettingsException;
import org.elasticsearch.alerts.ExecutionContext;
import org.elasticsearch.alerts.Payload;
import org.elasticsearch.alerts.support.Script;
import org.elasticsearch.alerts.support.init.proxy.ScriptServiceProxy;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.script.ExecutableScript;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class ScriptTransform extends Transform {

    public static final String TYPE = "script";

    private final Script script;
    private final ScriptServiceProxy scriptService;

    public ScriptTransform(Script script, ScriptServiceProxy scriptService) {
        this.script = script;
        this.scriptService = scriptService;
    }

    @Override
    public String type() {
        return TYPE;
    }

    Script script() {
        return script;
    }

    @Override
    public Result apply(ExecutionContext ctx, Payload payload) throws IOException {
        Map<String, Object> model = new HashMap<>();
        model.putAll(script.params());
        model.putAll(createModel(ctx, payload));
        ExecutableScript executable = scriptService.executable(script.lang(), script.script(), script.type(), model);
        Object value = executable.run();
        if (!(value instanceof Map)) {
            throw new TransformException("illegal [script] transform [" + script.script() + "]. script must output a Map<String, Object> structure but outputted [" + value.getClass().getSimpleName() + "] instead");
        }
        return new Result(TYPE, new Payload.Simple((Map<String, Object>) value));
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.value(script);
    }

    public static class Parser implements Transform.Parser<ScriptTransform> {

        private final ScriptServiceProxy scriptService;

        @Inject
        public Parser(ScriptServiceProxy scriptService) {
            this.scriptService = scriptService;
        }

        @Override
        public String type() {
            return TYPE;
        }

        @Override
        public ScriptTransform parse(XContentParser parser) throws IOException {
            Script script = null;
            try {
                script = Script.parse(parser);
            } catch (Script.ParseException pe) {
                throw new AlertsSettingsException("could not parse [script] transform", pe);
            }
            return new ScriptTransform(script, scriptService);
        }
    }
}
