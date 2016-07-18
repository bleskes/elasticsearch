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

package org.elasticsearch.script;

import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.ScriptPlugin;
import org.elasticsearch.search.lookup.SearchLookup;
import org.elasticsearch.xpack.watcher.support.WatcherScript;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * A dummy script engine used for testing. Scripts must be a number. Running the script
 */
public class SleepScriptEngine implements ScriptEngineService {

    public static final String NAME = "sleep";

    public static class TestPlugin extends Plugin implements ScriptPlugin {
        @Override
        public ScriptEngineService getScriptEngineService(Settings settings) {
            return new SleepScriptEngine();
        }
    }

    @Override
    public String getType() {
        return NAME;
    }

    @Override
    public String getExtension() {
        return NAME;
    }

    @Override
    public Object compile(String scriptName, String scriptSource, Map<String, String> params) {
        return scriptSource;
    }

    @Override
    public ExecutableScript executable(CompiledScript compiledScript, @Nullable Map<String, Object> vars) {
        return new AbstractSearchScript() {
            @Override
            public Object run() {
                try {
                    Thread.sleep(((Number) vars.get("millis")).longValue());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return true;
            }
        };
    }

    @Override
    public SearchScript search(CompiledScript compiledScript, SearchLookup lookup, @Nullable Map<String, Object> vars) {
        return null;
    }

    @Override
    public void close() throws IOException {
    }

    public static WatcherScript sleepScript(long millis) {
        return new WatcherScript.Builder.Inline("")
                .lang("sleep")
                .params(Collections.singletonMap("millis", millis)).build();
    }

    @Override
    public boolean isInlineScriptEnabled() {
        return true;
    }
}
