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

package org.elasticsearch.watcher.support.text.xmustache;

import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.watcher.support.init.proxy.ScriptServiceProxy;
import org.elasticsearch.watcher.support.text.TextTemplate;
import org.elasticsearch.watcher.support.text.TextTemplateEngine;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class XMustacheTextTemplateEngine extends AbstractComponent implements TextTemplateEngine {

    private final ScriptServiceProxy service;

    @Inject
    public XMustacheTextTemplateEngine(Settings settings, ScriptServiceProxy service) {
        super(settings);
        this.service = service;
    }

    @Override
    public String render(TextTemplate template, Map<String, Object> model) {
        Map<String, Object> mergedModel = new HashMap<>();
        mergedModel.putAll(template.getParams());
        mergedModel.putAll(model);
        ExecutableScript executable = service.executable(new org.elasticsearch.script.Template(template.getTemplate(), template.getType(), XMustacheScriptEngineService.NAME, template.getContentType(), mergedModel));
        Object result = executable.run();
        if (result instanceof BytesReference) {
            return ((BytesReference) result).toUtf8();
        }
        return result.toString();
    }
}
