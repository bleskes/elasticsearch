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

package org.elasticsearch.watcher.transform.script;

import com.carrotsearch.ant.tasks.junit4.dependencies.com.google.common.collect.ImmutableList;
import com.carrotsearch.ant.tasks.junit4.dependencies.com.google.common.collect.ImmutableSet;
import com.carrotsearch.randomizedtesting.annotations.Repeat;
import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.script.CompiledScript;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.ScriptService.ScriptType;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.watcher.WatcherException;
import org.elasticsearch.watcher.execution.WatchExecutionContext;
import org.elasticsearch.watcher.support.Script;
import org.elasticsearch.watcher.support.Variables;
import org.elasticsearch.watcher.support.init.proxy.ScriptServiceProxy;
import org.elasticsearch.watcher.transform.Transform;
import org.elasticsearch.watcher.watch.Payload;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.watcher.test.WatcherTestUtils.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class ScriptTransformTests extends ElasticsearchTestCase {

    ThreadPool tp = null;

    @Before
    public void init() {
        tp = new ThreadPool(ThreadPool.Names.SAME);
    }

    @After
    public void cleanup() {
        tp.shutdownNow();
    }


    @Test
    public void testApply_MapValue() throws Exception {
        ScriptServiceProxy service = mock(ScriptServiceProxy.class);
        ScriptType type = randomFrom(ScriptType.values());
        Map<String, Object> params = Collections.emptyMap();
        Script script = scriptBuilder(type, "_script").lang("_lang").params(params).build();
        CompiledScript compiledScript = mock(CompiledScript.class);
        when(service.compile(script)).thenReturn(compiledScript);
        ExecutableScriptTransform transform = new ExecutableScriptTransform(new ScriptTransform(script), logger, service);

        WatchExecutionContext ctx = mockExecutionContext("_name", EMPTY_PAYLOAD);

        Payload payload = simplePayload("key", "value");

        Map<String, Object> model = Variables.createCtxModel(ctx, payload);

        Map<String, Object> transformed = ImmutableMap.<String, Object>builder()
                .put("key", "value")
                .build();

        ExecutableScript executable = mock(ExecutableScript.class);
        when(executable.run()).thenReturn(transformed);
        when(service.executable(compiledScript, model)).thenReturn(executable);

        Transform.Result result = transform.execute(ctx, payload);
        assertThat(result, notNullValue());
        assertThat(result.type(), is(ScriptTransform.TYPE));
        assertThat(result.payload().data(), equalTo(transformed));
    }

    @Test
    public void testApply_NonMapValue() throws Exception {
        ScriptServiceProxy service = mock(ScriptServiceProxy.class);

        ScriptType type = randomFrom(ScriptType.values());
        Map<String, Object> params = Collections.emptyMap();
        Script script = scriptBuilder(type, "_script").lang("_lang").params(params).build();
        CompiledScript compiledScript = mock(CompiledScript.class);
        when(service.compile(script)).thenReturn(compiledScript);
        ExecutableScriptTransform transform = new ExecutableScriptTransform(new ScriptTransform(script), logger, service);

        WatchExecutionContext ctx = mockExecutionContext("_name", EMPTY_PAYLOAD);

        Payload payload = simplePayload("key", "value");

        Map<String, Object> model = Variables.createCtxModel(ctx, payload);

        ExecutableScript executable = mock(ExecutableScript.class);
        Object value = randomFrom("value", 1, new String[] { "value" }, ImmutableList.of("value"), ImmutableSet.of("value"));
        when(executable.run()).thenReturn(value);
        when(service.executable(compiledScript, model)).thenReturn(executable);

        Transform.Result result = transform.execute(ctx, payload);
        assertThat(result, notNullValue());
        assertThat(result.type(), is(ScriptTransform.TYPE));
        assertThat(result.payload().data().size(), is(1));
        assertThat(result.payload().data(), hasEntry("_value", value));
    }

    @Test
    public void testParser() throws Exception {
        ScriptServiceProxy service = mock(ScriptServiceProxy.class);
        ScriptType type = randomFrom(ScriptType.values());
        XContentBuilder builder = jsonBuilder().startObject();
        builder.field(scriptTypeField(type), "_script");
        builder.field("lang", "_lang");
        builder.startObject("params").field("key", "value").endObject();
        builder.endObject();

        XContentParser parser = JsonXContent.jsonXContent.createParser(builder.bytes());
        parser.nextToken();
        ExecutableScriptTransform transform = new ScriptTransformFactory(ImmutableSettings.EMPTY, service).parseExecutable("_id", parser);
        Script script = scriptBuilder(type, "_script").lang("_lang").params(ImmutableMap.<String, Object>builder().put("key", "value").build()).build();
        assertThat(transform.transform().getScript(), equalTo(script));
    }

    @Test
    public void testParser_String() throws Exception {
        ScriptServiceProxy service = mock(ScriptServiceProxy.class);
        XContentBuilder builder = jsonBuilder().value("_script");

        XContentParser parser = JsonXContent.jsonXContent.createParser(builder.bytes());
        parser.nextToken();
        ExecutableScriptTransform transform = new ScriptTransformFactory(ImmutableSettings.EMPTY, service).parseExecutable("_id", parser);
        assertThat(transform.transform().getScript(), equalTo(Script.defaultType("_script").build()));
    }


    @Test(expected = ScriptTransformValidationException.class)
    @Repeat(iterations = 3)
    public void testScriptConditionParser_badScript() throws Exception {
        ScriptTransformFactory transformFactory = new ScriptTransformFactory(ImmutableSettings.settingsBuilder().build(), getScriptServiceProxy(tp));
        ScriptType scriptType = randomFrom(ScriptType.values());
        String script;
        switch (scriptType) {
            case INDEXED:
            case FILE:
                script = "nonExisting_script";
                break;
            case INLINE:
            default:
                script = "foo = = 1";
        }

        XContentBuilder builder = jsonBuilder().startObject()
                .field(scriptTypeField(scriptType), script)
                .field("lang", "groovy")
                .startObject("params").field("key", "value").endObject()
                .endObject();

        XContentParser parser = XContentFactory.xContent(builder.bytes()).createParser(builder.bytes());
        parser.nextToken();
        ScriptTransform scriptTransform = transformFactory.parseTransform("_watch", parser);
        transformFactory.createExecutable(scriptTransform);
        fail("expected a transform validation exception trying to create an executable with a bad or missing script");
    }

    @Test(expected = ScriptTransformValidationException.class)
    public void testScriptConditionParser_badLang() throws Exception {
        ScriptTransformFactory transformFactory = new ScriptTransformFactory(ImmutableSettings.settingsBuilder().build(), getScriptServiceProxy(tp));
        ScriptType scriptType = randomFrom(ScriptType.values());
        String script = "return true";
        XContentBuilder builder = jsonBuilder().startObject()
                .field(scriptTypeField(scriptType), script)
                .field("lang", "not_a_valid_lang")
                .startObject("params").field("key", "value").endObject()
                .endObject();


        XContentParser parser = XContentFactory.xContent(builder.bytes()).createParser(builder.bytes());
        parser.nextToken();
        ScriptTransform scriptCondition = transformFactory.parseTransform("_watch", parser);
        transformFactory.createExecutable(scriptCondition);
        fail("expected a transform validation exception trying to create an executable with an invalid language");
    }

    static Script.Builder scriptBuilder(ScriptType type, String script) {
        switch (type) {
            case INLINE:    return Script.inline(script);
            case FILE:      return Script.file(script);
            case INDEXED:   return Script.indexed(script);
            default:
                throw new WatcherException("unsupported script type [{}]", type);
        }
    }

    static String scriptTypeField(ScriptType type) {
        switch (type) {
            case INLINE: return "inline";
            case FILE: return "file";
            case INDEXED: return "id";
            default:
                throw new WatcherException("unsupported script type [{}]", type);
        }
    }
}
