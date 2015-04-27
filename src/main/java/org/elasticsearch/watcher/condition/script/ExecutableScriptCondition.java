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

package org.elasticsearch.watcher.condition.script;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.groovy.GroovyScriptExecutionException;
import org.elasticsearch.watcher.condition.ExecutableCondition;
import org.elasticsearch.watcher.execution.WatchExecutionContext;
import org.elasticsearch.watcher.support.Variables;
import org.elasticsearch.watcher.support.init.proxy.ScriptServiceProxy;

import java.io.IOException;

/**
 * This class executes a script against the ctx payload and returns a boolean
 */
public class ExecutableScriptCondition extends ExecutableCondition<ScriptCondition, ScriptCondition.Result> {

    private final ScriptServiceProxy scriptService;

    public ExecutableScriptCondition(ScriptCondition condition, ESLogger logger, ScriptServiceProxy scriptService) {
        super(condition, logger);
        this.scriptService = scriptService;
    }

    @Override
    public ScriptCondition.Result execute(WatchExecutionContext ctx) throws IOException {
        try {
            ExecutableScript executable = scriptService.executable(condition.script, Variables.createCtxModel(ctx, ctx.payload()));
            Object value = executable.run();
            if (value instanceof Boolean) {
                return (Boolean) value ? ScriptCondition.Result.MET : ScriptCondition.Result.UNMET;
            }
            throw new ScriptConditionException("failed to execute [{}] condition for watch [{}]. script [{}] must return a boolean value (true|false) but instead returned [{}]", type(), ctx.watch().id(), condition.script.script(), value);
        } catch (GroovyScriptExecutionException gsee) {
            throw new ScriptConditionException("failed to execute [{}] condition for watch [{}]. script [{}] threw an exception", gsee, type(), ctx.watch().id(), condition.script.script());
        }
    }
}
