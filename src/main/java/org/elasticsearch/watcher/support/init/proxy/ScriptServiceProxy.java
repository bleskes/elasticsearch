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
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.script.SearchScript;
import org.elasticsearch.search.lookup.SearchLookup;
import org.elasticsearch.watcher.support.init.InitializingService;

import java.util.Map;

/**
 *A lazily initialized proxy to the elasticsearch {@link ScriptService}. Inject this proxy whenever the script
 * service needs to be injected to avoid circular dependencies issues.
 */
public class ScriptServiceProxy implements InitializingService.Initializable {

    private ScriptService service;

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
    }

    public ExecutableScript executable(String lang, String script, ScriptService.ScriptType scriptType, Map<String, Object> vars) {
        return service.executable(lang, script, scriptType, vars);
    }

    public SearchScript search(SearchLookup lookup, String lang, String script, ScriptService.ScriptType scriptType, Map<String, Object> vars) {
        return service.search(lookup, lang, script, scriptType, vars);
    }
}
