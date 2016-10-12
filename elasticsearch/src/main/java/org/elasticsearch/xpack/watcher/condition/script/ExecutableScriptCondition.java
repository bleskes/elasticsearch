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

package org.elasticsearch.xpack.watcher.condition.script;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.script.CompiledScript;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.xpack.watcher.Watcher;
import org.elasticsearch.xpack.watcher.condition.ExecutableCondition;
import org.elasticsearch.xpack.watcher.execution.WatchExecutionContext;
import org.elasticsearch.xpack.watcher.support.Variables;

import java.util.Collections;
import java.util.Map;

import static org.elasticsearch.xpack.watcher.support.Exceptions.invalidScript;

/**
 * This class executes a script against the ctx payload and returns a boolean
 */
public class ExecutableScriptCondition extends ExecutableCondition<ScriptCondition, ScriptCondition.Result> {

    private final ScriptService scriptService;
    private final CompiledScript compiledScript;

    public ExecutableScriptCondition(ScriptCondition condition, Logger logger, ScriptService scriptService) {
        super(condition, logger);
        this.scriptService = scriptService;
        Script script = new Script(condition.script.getScript(), condition.script.getType(),
                                   condition.script.getLang(), condition.script.getParams());
        compiledScript = scriptService.compile(script, Watcher.SCRIPT_CONTEXT, Collections.emptyMap());
    }

    @Override
    public ScriptCondition.Result execute(WatchExecutionContext ctx) {
        return doExecute(ctx);
    }

    public ScriptCondition.Result doExecute(WatchExecutionContext ctx) {
        Map<String, Object> parameters = Variables.createCtxModel(ctx, ctx.payload());
        if (condition.script.getParams() != null && !condition.script.getParams().isEmpty()) {
            parameters.putAll(condition.script.getParams());
        }
        ExecutableScript executable = scriptService.executable(compiledScript, parameters);
        Object value = executable.run();
        if (value instanceof Boolean) {
            return (Boolean) value ? ScriptCondition.Result.MET : ScriptCondition.Result.UNMET;
        }
        throw invalidScript("condition [{}] must return a boolean value (true|false) but instead returned [{}]", type(), ctx.watch().id(),
                condition.script, value);
    }
}
