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
import org.elasticsearch.script.CompiledScript;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.ScriptContext;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.shield.XPackUser;
import org.elasticsearch.shield.SecurityContext;
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
    private SecurityContext securityContext;

    /**
     * Creates a proxy to the given script service (can be used for testing)
     */
    public static ScriptServiceProxy of(ScriptService service) {
        ScriptServiceProxy proxy = new ScriptServiceProxy();
        proxy.service = service;
        proxy.securityContext = SecurityContext.Insecure.INSTANCE;
        return proxy;
    }

    @Override
    public void init(Injector injector) {
        this.service = injector.getInstance(ScriptService.class);
        this.securityContext = injector.getInstance(SecurityContext.class);
    }

    public CompiledScript compile(Script script) {
        return securityContext.executeAs(XPackUser.INSTANCE, () ->
            compile(new org.elasticsearch.script.Script(script.script(), script.type(), script.lang(), script.params()), Collections.emptyMap()));
    }

    public CompiledScript compile(org.elasticsearch.script.Script script, Map<String, String> compileParams) {
        return securityContext.executeAs(XPackUser.INSTANCE, () ->
                service.compile(script, WatcherScriptContext.CTX, compileParams));
    }

    public ExecutableScript executable(CompiledScript compiledScript, Map<String, Object> vars) {
        return securityContext.executeAs(XPackUser.INSTANCE, () ->
                service.executable(compiledScript, vars));
    }


    public ExecutableScript executable(org.elasticsearch.script.Script script) {
        return securityContext.executeAs(XPackUser.INSTANCE, () ->
                service.executable(script, WatcherScriptContext.CTX, Collections.emptyMap()));
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
