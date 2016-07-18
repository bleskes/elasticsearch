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

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.xpack.watcher.transform.TransformFactory;

import java.io.IOException;

/**
 *
 */
public class ScriptTransformFactory extends TransformFactory<ScriptTransform, ScriptTransform.Result, ExecutableScriptTransform> {

    private final ScriptService scriptService;

    @Inject
    public ScriptTransformFactory(Settings settings, ScriptService scriptService) {
        super(Loggers.getLogger(ExecutableScriptTransform.class, settings));
        this.scriptService = scriptService;
    }

    @Override
    public String type() {
        return ScriptTransform.TYPE;
    }

    @Override
    public ScriptTransform parseTransform(String watchId, XContentParser parser) throws IOException {
        return ScriptTransform.parse(watchId, parser);
    }

    @Override
    public ExecutableScriptTransform createExecutable(ScriptTransform transform) {
        return new ExecutableScriptTransform(transform, transformLogger, scriptService);
    }
}
