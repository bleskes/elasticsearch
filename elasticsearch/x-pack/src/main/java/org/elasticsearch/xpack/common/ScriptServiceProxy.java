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

package org.elasticsearch.xpack.common;

import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.script.CompiledScript;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.ScriptContext;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.xpack.security.SecurityContext;
import org.elasticsearch.xpack.security.user.XPackUser;
import org.elasticsearch.xpack.watcher.support.Script;

import java.util.Map;

import static java.util.Collections.emptyMap;

/**
 * Wraps {@link ScriptService} but ensure that all scripts are run or compiled as {@link XPackUser}.
 */
public class ScriptServiceProxy {

    private final ScriptService service;
    private final SecurityContext securityContext;

    @Inject
    public ScriptServiceProxy(ScriptService service, SecurityContext securityContext) {
        this.service = service;
        this.securityContext = securityContext;
    }

    public CompiledScript compile(Script script) {
        return compile(new org.elasticsearch.script.Script(script.script(), script.type(), script.lang(), script.params()), emptyMap());
    }

    public CompiledScript compile(org.elasticsearch.script.Script script, Map<String, String> compileParams) {
        return securityContext.executeAs(XPackUser.INSTANCE, () ->
                service.compile(script, WatcherScriptContext.CTX, compileParams));
    }

    public ExecutableScript executable(CompiledScript compiledScript, Map<String, Object> vars) {
        return securityContext.executeAs(XPackUser.INSTANCE, () ->
                service.executable(compiledScript, vars));
    }

    public static final ScriptContext.Plugin INSTANCE = new ScriptContext.Plugin("xpack", "watch");

    private static class WatcherScriptContext implements ScriptContext {

        public static final ScriptContext CTX = new WatcherScriptContext();

        @Override
        public String getKey() {
            return INSTANCE.getKey();
        }
    }

    /**
     * Factory helper method for testing.
     */
    public static ScriptServiceProxy of(ScriptService service) {
        return new ScriptServiceProxy(service, SecurityContext.Insecure.INSTANCE);
    }
}
