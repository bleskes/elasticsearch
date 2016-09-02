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

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.script.ScriptSettings;
import org.elasticsearch.xpack.watcher.condition.ConditionFactory;

import java.io.IOException;

public class ScriptConditionFactory extends ConditionFactory<ScriptCondition, ScriptCondition.Result, ExecutableScriptCondition> {

    private final Settings settings;
    private final ScriptService scriptService;

    @Inject
    public ScriptConditionFactory(Settings settings, ScriptService service) {
        super(Loggers.getLogger(ExecutableScriptCondition.class, settings));
        this.settings = settings;
        scriptService = service;
    }

    @Override
    public String type() {
        return ScriptCondition.TYPE;
    }

    @Override
    public ScriptCondition parseCondition(String watchId, XContentParser parser, boolean upgradeConditionSource) throws IOException {
        String defaultLegacyScriptLanguage = ScriptSettings.getLegacyDefaultLang(settings);
        return ScriptCondition.parse(watchId, parser, upgradeConditionSource, defaultLegacyScriptLanguage);
    }

    @Override
    public ExecutableScriptCondition createExecutable(ScriptCondition condition) {
        return new ExecutableScriptCondition(condition, conditionLogger, scriptService);
    }
}
