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

package org.elasticsearch.alerts.actions.webhook;

import org.elasticsearch.alerts.support.StringTemplateUtils;
import org.elasticsearch.alerts.support.init.proxy.ScriptServiceProxy;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.script.ScriptEngineService;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.script.mustache.MustacheScriptEngineService;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.watcher.ResourceWatcherService;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 */
public class WebhookTest extends ElasticsearchTestCase {

    public void testRequestParameterSerialization() throws Exception {

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("hits",0);

        Settings settings = ImmutableSettings.settingsBuilder().build();
        MustacheScriptEngineService mustacheScriptEngineService = new MustacheScriptEngineService(settings);
        ThreadPool tp;
        tp = new ThreadPool(ThreadPool.Names.SAME);
        Set<ScriptEngineService> engineServiceSet = new HashSet<>();
        engineServiceSet.add(mustacheScriptEngineService);
        ScriptService scriptService = new ScriptService(settings, new Environment(), engineServiceSet, new ResourceWatcherService(settings, tp));

        StringTemplateUtils.Template testTemplate = new StringTemplateUtils.Template("{ 'alertname' : '{{alert_name}}', 'response' : { 'hits' : {{response.hits}} } }");
        String testBody = WebhookAction.applyTemplate(new StringTemplateUtils(settings, ScriptServiceProxy.of(scriptService)), testTemplate, "foobar", responseMap);

        tp.shutdownNow();
        assertEquals("{ 'alertname' : 'foobar', 'response' : { 'hits' : 0 } }", testBody);


    }
}
