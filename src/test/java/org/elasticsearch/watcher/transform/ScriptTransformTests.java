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

package org.elasticsearch.watcher.transform;

import com.carrotsearch.ant.tasks.junit4.dependencies.com.google.common.collect.ImmutableList;
import com.carrotsearch.ant.tasks.junit4.dependencies.com.google.common.collect.ImmutableSet;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.watcher.execution.WatchExecutionContext;
import org.elasticsearch.watcher.transform.script.ExecutableScriptTransform;
import org.elasticsearch.watcher.transform.script.ScriptTransform;
import org.elasticsearch.watcher.transform.script.ScriptTransformFactory;
import org.elasticsearch.watcher.watch.Payload;
import org.elasticsearch.watcher.support.Script;
import org.elasticsearch.watcher.support.Variables;
import org.elasticsearch.watcher.support.init.proxy.ScriptServiceProxy;
import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.elasticsearch.watcher.test.WatcherTestUtils.*;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class ScriptTransformTests extends ElasticsearchTestCase {

    @Test
    public void testApply_MapValue() throws Exception {
        ScriptServiceProxy service = mock(ScriptServiceProxy.class);
        ScriptService.ScriptType type = randomFrom(ScriptService.ScriptType.values());
        Map<String, Object> params = Collections.emptyMap();
        Script script = new Script("_script", type, "_lang", params);
        ExecutableScriptTransform transform = new ExecutableScriptTransform(new ScriptTransform(script), logger, service);

        WatchExecutionContext ctx = mockExecutionContext("_name", EMPTY_PAYLOAD);

        Payload payload = simplePayload("key", "value");

        Map<String, Object> model = Variables.createCtxModel(ctx, payload);

        Map<String, Object> transformed = ImmutableMap.<String, Object>builder()
                .put("key", "value")
                .build();

        ExecutableScript executable = mock(ExecutableScript.class);
        when(executable.run()).thenReturn(transformed);
        when(service.executable("_lang", "_script", type, model)).thenReturn(executable);

        Transform.Result result = transform.execute(ctx, payload);
        assertThat(result, notNullValue());
        assertThat(result.type(), is(ScriptTransform.TYPE));
        assertThat(result.payload().data(), equalTo(transformed));
    }

    @Test
    public void testApply_NonMapValue() throws Exception {
        ScriptServiceProxy service = mock(ScriptServiceProxy.class);
        ScriptService.ScriptType type = randomFrom(ScriptService.ScriptType.values());
        Map<String, Object> params = Collections.emptyMap();
        Script script = new Script("_script", type, "_lang", params);
        ExecutableScriptTransform transform = new ExecutableScriptTransform(new ScriptTransform(script), logger, service);

        WatchExecutionContext ctx = mockExecutionContext("_name", EMPTY_PAYLOAD);

        Payload payload = simplePayload("key", "value");

        Map<String, Object> model = Variables.createCtxModel(ctx, payload);

        ExecutableScript executable = mock(ExecutableScript.class);
        Object value = randomFrom("value", 1, new String[] { "value" }, ImmutableList.of("value"), ImmutableSet.of("value"));
        when(executable.run()).thenReturn(value);
        when(service.executable("_lang", "_script", type, model)).thenReturn(executable);

        Transform.Result result = transform.execute(ctx, payload);
        assertThat(result, notNullValue());
        assertThat(result.type(), is(ScriptTransform.TYPE));
        assertThat(result.payload().data().size(), is(1));
        assertThat(result.payload().data(), hasEntry("_value", value));
    }

    @Test
    public void testParser() throws Exception {
        ScriptServiceProxy service = mock(ScriptServiceProxy.class);
        ScriptService.ScriptType type = randomFrom(ScriptService.ScriptType.values());
        XContentBuilder builder = jsonBuilder().startObject()
                .field("script", "_script")
                .field("lang", "_lang")
                .field("type", type.name())
                .startObject("params").field("key", "value").endObject()
                .endObject();

        XContentParser parser = JsonXContent.jsonXContent.createParser(builder.bytes());
        parser.nextToken();
        ExecutableScriptTransform transform = new ScriptTransformFactory(ImmutableSettings.EMPTY, service).parseExecutable("_id", parser);
        assertThat(transform.transform().getScript(), equalTo(new Script("_script", type, "_lang", ImmutableMap.<String, Object>builder().put("key", "value").build())));
    }

    @Test
    public void testParser_String() throws Exception {
        ScriptServiceProxy service = mock(ScriptServiceProxy.class);
        XContentBuilder builder = jsonBuilder().value("_script");

        XContentParser parser = JsonXContent.jsonXContent.createParser(builder.bytes());
        parser.nextToken();
        ExecutableScriptTransform transform = new ScriptTransformFactory(ImmutableSettings.EMPTY, service).parseExecutable("_id", parser);
        assertThat(transform.transform().getScript(), equalTo(new Script("_script", ScriptService.ScriptType.INLINE, ScriptService.DEFAULT_LANG, ImmutableMap.<String, Object>of())));
    }
}
