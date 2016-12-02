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

package org.elasticsearch.xpack.watcher.transform.script;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.util.Supplier;
import org.elasticsearch.script.CompiledScript;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.xpack.watcher.Watcher;
import org.elasticsearch.xpack.watcher.execution.WatchExecutionContext;
import org.elasticsearch.xpack.watcher.transform.ExecutableTransform;
import org.elasticsearch.xpack.watcher.watch.Payload;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.elasticsearch.xpack.watcher.support.Exceptions.invalidScript;
import static org.elasticsearch.xpack.watcher.support.Variables.createCtxModel;
import static org.elasticsearch.xpack.watcher.transform.script.ScriptTransform.TYPE;

public class ExecutableScriptTransform extends ExecutableTransform<ScriptTransform, ScriptTransform.Result> {

    private final ScriptService scriptService;

    public ExecutableScriptTransform(ScriptTransform transform, Logger logger, ScriptService scriptService) {
        super(transform, logger);
        this.scriptService = scriptService;
        Script script = transform.getScript();
        // try to compile so we catch syntax errors early
        try {
            scriptService.compile(script, Watcher.SCRIPT_CONTEXT, Collections.emptyMap());
        } catch (Exception e) {
            throw invalidScript("failed to compile script [{}]", e, script, e);
        }
    }

    @Override
    public ScriptTransform.Result execute(WatchExecutionContext ctx, Payload payload) {
        try {
            return doExecute(ctx, payload);
        } catch (Exception e) {
            logger.error((Supplier<?>) () -> new ParameterizedMessage("failed to execute [{}] transform for [{}]", TYPE, ctx.id()), e);
            return new ScriptTransform.Result(e);
        }
    }

    ScriptTransform.Result doExecute(WatchExecutionContext ctx, Payload payload) throws IOException {
        Script script = transform.getScript();
        Map<String, Object> model = new HashMap<>();
        if (script.getParams() != null) {
            model.putAll(script.getParams());
        }
        model.putAll(createCtxModel(ctx, payload));
        CompiledScript compiledScript = scriptService.compile(script, Watcher.SCRIPT_CONTEXT, Collections.emptyMap());
        ExecutableScript executable = scriptService.executable(compiledScript, model);
        Object value = executable.run();
        if (value instanceof Map) {
            return new ScriptTransform.Result(new Payload.Simple((Map<String, Object>) value));
        }
        Map<String, Object> data = new HashMap<>();
        data.put("_value", value);
        return new ScriptTransform.Result(new Payload.Simple(data));
    }
}
