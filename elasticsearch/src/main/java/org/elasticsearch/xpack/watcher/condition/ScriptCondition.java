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

package org.elasticsearch.xpack.watcher.condition;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.script.CompiledScript;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.xpack.watcher.Watcher;
import org.elasticsearch.xpack.watcher.execution.WatchExecutionContext;
import org.elasticsearch.xpack.watcher.support.Variables;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static org.elasticsearch.xpack.watcher.support.Exceptions.invalidScript;

/**
 * This class executes a script against the ctx payload and returns a boolean
 */
public final class ScriptCondition extends Condition {
    public static final String TYPE = "script";
    private static final Result MET = new Result(null, TYPE, true);
    private static final Result UNMET = new Result(null, TYPE, false);

    private final ScriptService scriptService;
    private final Script script;

    public ScriptCondition(Script script) {
        super(TYPE);
        this.script = script;
        scriptService = null;
    }

    ScriptCondition(Script script, ScriptService scriptService) {
        super(TYPE);
        this.scriptService = scriptService;
        this.script = script;
        // try to compile so we catch syntax errors early
        scriptService.compile(script, Watcher.SCRIPT_CONTEXT);
    }

    public Script getScript() {
        return script;
    }

    public static ScriptCondition parse(ScriptService scriptService, String watchId, XContentParser parser, boolean upgradeConditionSource,
                                        String defaultLegacyScriptLanguage) throws IOException {
        try {
            Script script;
            if (upgradeConditionSource) {
                script = Script.parse(parser, defaultLegacyScriptLanguage);
            } else {
                script = Script.parse(parser);
            }
            return new ScriptCondition(script, scriptService);
        } catch (ElasticsearchParseException pe) {
            throw new ElasticsearchParseException("could not parse [{}] condition for watch [{}]. failed to parse script", pe, TYPE,
                    watchId);
        }
    }

    @Override
    public Result execute(WatchExecutionContext ctx) {
        return doExecute(ctx);
    }

    public Result doExecute(WatchExecutionContext ctx) {
        Map<String, Object> parameters = Variables.createCtxModel(ctx, ctx.payload());
        if (script.getParams() != null && !script.getParams().isEmpty()) {
            parameters.putAll(script.getParams());
        }
        CompiledScript compiledScript = scriptService.compile(script, Watcher.SCRIPT_CONTEXT);
        ExecutableScript executable = scriptService.executable(compiledScript, parameters);
        Object value = executable.run();
        if (value instanceof Boolean) {
            return (Boolean) value ? MET : UNMET;
        }
        throw invalidScript("condition [{}] must return a boolean value (true|false) but instead returned [{}]", type(), ctx.watch().id(),
                script, value);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return script.toXContent(builder, params);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ScriptCondition condition = (ScriptCondition) o;

        return script.equals(condition.script);
    }

    @Override
    public int hashCode() {
        return script.hashCode();
    }
}
