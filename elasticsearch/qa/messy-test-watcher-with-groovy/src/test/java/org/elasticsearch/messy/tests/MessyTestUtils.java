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

package org.elasticsearch.messy.tests;

import org.apache.lucene.util.LuceneTestCase;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.SuppressForbidden;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.script.ScriptContextRegistry;
import org.elasticsearch.script.ScriptEngineRegistry;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.script.ScriptSettings;
import org.elasticsearch.script.groovy.GroovyScriptEngineService;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.watcher.ResourceWatcherService;
import org.elasticsearch.xpack.common.ScriptServiceProxy;
import org.junit.Ignore;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;

@Ignore // not a test.
@SuppressForbidden(reason = "gradle is broken and tries to run me as a test")
public final class MessyTestUtils {
    public static ScriptServiceProxy getScriptServiceProxy(ThreadPool tp) throws Exception {
        Settings settings = Settings.builder()
                .put("script.inline", "true")
                .put("script.indexed", "true")
                .put("path.home", LuceneTestCase.createTempDir())
                .build();
        GroovyScriptEngineService groovyScriptEngineService = new GroovyScriptEngineService(settings);
        ScriptEngineRegistry scriptEngineRegistry = new ScriptEngineRegistry(Collections.singleton(groovyScriptEngineService));
        ScriptContextRegistry scriptContextRegistry = new ScriptContextRegistry(Arrays.asList(ScriptServiceProxy.INSTANCE));

        ClusterService clusterService = Mockito.mock(ClusterService.class);
        Mockito.when(clusterService.state()).thenReturn(ClusterState.builder(new ClusterName("_name")).build());
        ScriptSettings scriptSettings = new ScriptSettings(scriptEngineRegistry, scriptContextRegistry);
        return  ScriptServiceProxy.of(new ScriptService(settings, new Environment(settings),
                new ResourceWatcherService(settings, tp), scriptEngineRegistry, scriptContextRegistry, scriptSettings),
                clusterService);
    }
}
