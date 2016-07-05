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

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.script.CompiledScript;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.xpack.common.ScriptServiceProxy;
import org.elasticsearch.xpack.watcher.condition.ExecutableCondition;
import org.elasticsearch.xpack.watcher.execution.WatchExecutionContext;
import org.elasticsearch.xpack.watcher.support.Variables;

import java.util.Map;

import static org.elasticsearch.xpack.watcher.support.Exceptions.invalidScript;

/**
 * This class executes a script against the ctx payload and returns a boolean
 */
public class ExecutableScriptCondition extends ExecutableCondition<ScriptCondition, ScriptCondition.Result> {

    private final ScriptServiceProxy scriptService;
    private final CompiledScript compiledScript;

    public ExecutableScriptCondition(ScriptCondition condition, ESLogger logger, ScriptServiceProxy scriptService) {
        super(condition, logger);
        this.scriptService = scriptService;
        try {
            compiledScript = scriptService.compile(condition.script);
        } catch (Exception e) {
            throw invalidScript("failed to compile script [{}] with lang [{}] of type [{}]", e, condition.script.script(),
                    condition.script.lang(), condition.script.type(), e);
        }
    }

    @Override
    public ScriptCondition.Result execute(WatchExecutionContext ctx) {
        return doExecute(ctx);
    }

    public ScriptCondition.Result doExecute(WatchExecutionContext ctx) {
        Map<String, Object> parameters = Variables.createCtxModel(ctx, ctx.payload());
        if (condition.script.params() != null && !condition.script.params().isEmpty()) {
            parameters.putAll(condition.script.params());
        }
        ExecutableScript executable = scriptService.executable(compiledScript, parameters);
        Object value = executable.run();
        if (value instanceof Boolean) {
            return (Boolean) value ? ScriptCondition.Result.MET : ScriptCondition.Result.UNMET;
        }
        throw invalidScript("condition [{}] must return a boolean value (true|false) but instead returned [{}]", type(), ctx.watch().id(),
                condition.script.script(), value);
    }
}
