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

import org.elasticsearch.xpack.common.text.DefaultTextTemplateEngine;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A mock script engine that registers itself under the 'mustache' name so that
 * {@link DefaultTextTemplateEngine}
 * uses it and adds validation that watcher tests don't rely on mustache templating/
 */
public class MockMustacheScriptEngine extends MockScriptEngine {

    public static final String NAME = "mustache";

    public static class TestPlugin extends MockScriptEngine.TestPlugin {

        public void onModule(ScriptModule module) {
            module.addScriptEngine(new ScriptEngineRegistry.ScriptEngineRegistration(MockMustacheScriptEngine.class, NAME, true));
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
    public Object compile(String name, String script, Map<String, String> params) {
        if (script.contains("{{") && script.contains("}}")) {
            throw new IllegalArgumentException("Fix your test to not rely on mustache");
        }

        return super.compile(name, script, params);
    }
}
