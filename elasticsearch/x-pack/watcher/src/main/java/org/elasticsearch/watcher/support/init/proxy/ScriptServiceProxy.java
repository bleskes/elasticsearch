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

package org.elasticsearch.watcher.support.init.proxy;

import org.elasticsearch.common.inject.Injector;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.script.CompiledScript;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.ScriptContext;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.watcher.shield.ShieldIntegration;
import org.elasticsearch.watcher.support.Script;
import org.elasticsearch.watcher.support.init.InitializingService;

import java.util.Collections;
import java.util.Map;

/**
 *A lazily initialized proxy to the elasticsearch {@link ScriptService}. Inject this proxy whenever the script
 * service needs to be injected to avoid circular dependencies issues.
 */
public class ScriptServiceProxy implements InitializingService.Initializable {

    private ScriptService service;
    private ThreadContext threadContext;
    private ShieldIntegration shieldIntegration;

    /**
     * Creates a proxy to the given script service (can be used for testing)
     */
    public static ScriptServiceProxy of(ScriptService service) {
        ScriptServiceProxy proxy = new ScriptServiceProxy();
        proxy.service = service;
        return proxy;
    }

    @Override
    public void init(Injector injector) {
        this.service = injector.getInstance(ScriptService.class);
        this.threadContext = injector.getInstance(ThreadPool.class).getThreadContext();
        this.shieldIntegration = injector.getInstance(ShieldIntegration.class);
    }

    public CompiledScript compile(Script script) {
        if (shieldIntegration != null) {
            try (ThreadContext.StoredContext ctx = threadContext.stashContext()) {
                shieldIntegration.setWatcherUser();
                return compile(new org.elasticsearch.script.Script(script.script(), script.type(), script.lang(), script.params()));
            }
        }
        return compile(new org.elasticsearch.script.Script(script.script(), script.type(), script.lang(), script.params()));
    }

    public CompiledScript compile(org.elasticsearch.script.Script script) {
        if (shieldIntegration != null) {
            try (ThreadContext.StoredContext ctx = threadContext.stashContext()) {
                shieldIntegration.setWatcherUser();
                return service.compile(script, WatcherScriptContext.CTX, Collections.emptyMap());
            }
        }
        return service.compile(script, WatcherScriptContext.CTX, Collections.emptyMap());
    }

    public ExecutableScript executable(CompiledScript compiledScript, Map<String, Object> vars) {
        if (shieldIntegration != null) {
            try (ThreadContext.StoredContext ctx = threadContext.stashContext()) {
                shieldIntegration.setWatcherUser();
                return service.executable(compiledScript, vars);
            }
        }
        return service.executable(compiledScript, vars);
    }


    public ExecutableScript executable(org.elasticsearch.script.Script script) {
        if (shieldIntegration != null) {
            try (ThreadContext.StoredContext ctx = threadContext.stashContext()) {
                shieldIntegration.setWatcherUser();
                return service.executable(script, WatcherScriptContext.CTX, Collections.emptyMap());
            }
        }
        return service.executable(script, WatcherScriptContext.CTX, Collections.emptyMap());
    }

    public static final ScriptContext.Plugin INSTANCE = new ScriptContext.Plugin("elasticsearch-watcher", "watch");

    private static class WatcherScriptContext implements ScriptContext {
        public static final ScriptContext CTX = new WatcherScriptContext();
        @Override
        public String getKey() {
            return INSTANCE.getKey();
        }
    }
}
