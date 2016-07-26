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


import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.ShardSearchFailure;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.script.GeneralScriptException;
import org.elasticsearch.script.ScriptException;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.script.ScriptService.ScriptType;
import org.elasticsearch.search.internal.InternalSearchResponse;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.threadpool.TestThreadPool;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.watcher.condition.script.ExecutableScriptCondition;
import org.elasticsearch.xpack.watcher.condition.script.ScriptCondition;
import org.elasticsearch.xpack.watcher.condition.script.ScriptConditionFactory;
import org.elasticsearch.xpack.watcher.execution.WatchExecutionContext;
import org.elasticsearch.xpack.watcher.support.WatcherScript;
import org.elasticsearch.xpack.watcher.watch.Payload;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;

import static java.util.Collections.singletonMap;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.messy.tests.MessyTestUtils.createScriptService;
import static org.elasticsearch.xpack.watcher.support.Exceptions.illegalArgument;
import static org.elasticsearch.xpack.watcher.test.WatcherTestUtils.mockExecutionContext;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

public class ScriptConditionTests extends ESTestCase {

    private ThreadPool tp = null;
    
    @Before
    public void init() {
        tp = new TestThreadPool(ThreadPool.Names.SAME);
    }

    @After
    public void cleanup() throws InterruptedException {
        terminate(tp);
    }

    public void testExecute() throws Exception {
        ScriptService scriptService = createScriptService(tp);
        ExecutableScriptCondition condition = new ExecutableScriptCondition(
                new ScriptCondition(WatcherScript.inline("ctx.payload.hits.total > 1").build()), logger, scriptService);
        SearchResponse response = new SearchResponse(InternalSearchResponse.empty(), "", 3, 3, 500L, new ShardSearchFailure[0]);
        WatchExecutionContext ctx = mockExecutionContext("_name", new Payload.XContent(response));
        assertFalse(condition.execute(ctx).met());
    }

    public void testExecuteMergedParams() throws Exception {
        ScriptService scriptService = createScriptService(tp);
        WatcherScript script = WatcherScript.inline("ctx.payload.hits.total > threshold")
                .lang(WatcherScript.DEFAULT_LANG).params(singletonMap("threshold", 1)).build();
        ExecutableScriptCondition executable = new ExecutableScriptCondition(new ScriptCondition(script), logger, scriptService);
        SearchResponse response = new SearchResponse(InternalSearchResponse.empty(), "", 3, 3, 500L, new ShardSearchFailure[0]);
        WatchExecutionContext ctx = mockExecutionContext("_name", new Payload.XContent(response));
        assertFalse(executable.execute(ctx).met());
    }

    public void testParserValid() throws Exception {
        ScriptConditionFactory factory = new ScriptConditionFactory(Settings.builder().build(), createScriptService(tp));

        XContentBuilder builder = createConditionContent("ctx.payload.hits.total > 1", null, ScriptType.INLINE);

        XContentParser parser = XContentFactory.xContent(builder.bytes()).createParser(builder.bytes());
        parser.nextToken();
        ScriptCondition condition = factory.parseCondition("_watch", parser);
        ExecutableScriptCondition executable = factory.createExecutable(condition);

        SearchResponse response = new SearchResponse(InternalSearchResponse.empty(), "", 3, 3, 500L, new ShardSearchFailure[0]);
        WatchExecutionContext ctx = mockExecutionContext("_name", new Payload.XContent(response));

        assertFalse(executable.execute(ctx).met());


        builder = createConditionContent("return true", null, ScriptType.INLINE);
        parser = XContentFactory.xContent(builder.bytes()).createParser(builder.bytes());
        parser.nextToken();
        condition = factory.parseCondition("_watch", parser);
        executable = factory.createExecutable(condition);

        ctx = mockExecutionContext("_name", new Payload.XContent(response));

        assertTrue(executable.execute(ctx).met());
    }

    public void testParserInvalid() throws Exception {
        ScriptConditionFactory factory = new ScriptConditionFactory(Settings.builder().build(), createScriptService(tp));
        XContentBuilder builder = XContentFactory.jsonBuilder();
        builder.startObject().endObject();
        XContentParser parser = XContentFactory.xContent(builder.bytes()).createParser(builder.bytes());
        parser.nextToken();
        try {
            factory.parseCondition("_id", parser);
            fail("expected a condition exception trying to parse an invalid condition XContent");
        } catch (ElasticsearchParseException e) {
            // TODO add these when the test if fixed
            // assertThat(e.getMessage(), is("ASDF"));
        }
    }

    public void testScriptConditionParserBadScript() throws Exception {
        ScriptConditionFactory conditionParser = new ScriptConditionFactory(Settings.builder().build(), createScriptService(tp));
        ScriptType scriptType = randomFrom(ScriptType.values());
        String script;
        switch (scriptType) {
            case STORED:
            case FILE:
                script = "nonExisting_script";
                break;
            case INLINE:
            default:
                script = "foo = = 1";
        }
        XContentBuilder builder = createConditionContent(script, "groovy", scriptType);
        XContentParser parser = XContentFactory.xContent(builder.bytes()).createParser(builder.bytes());
        parser.nextToken();
        ScriptCondition scriptCondition = conditionParser.parseCondition("_watch", parser);
        GeneralScriptException exception = expectThrows(GeneralScriptException.class,
                () -> conditionParser.createExecutable(scriptCondition));
    }

    public void testScriptConditionParser_badLang() throws Exception {
        ScriptConditionFactory conditionParser = new ScriptConditionFactory(Settings.builder().build(), createScriptService(tp));
        ScriptType scriptType = ScriptType.INLINE;
        String script = "return true";
        XContentBuilder builder = createConditionContent(script, "not_a_valid_lang", scriptType);
        XContentParser parser = XContentFactory.xContent(builder.bytes()).createParser(builder.bytes());
        parser.nextToken();
        ScriptCondition scriptCondition = conditionParser.parseCondition("_watch", parser);
        GeneralScriptException exception = expectThrows(GeneralScriptException.class,
                () -> conditionParser.createExecutable(scriptCondition));
        assertThat(exception.getMessage(), containsString("script_lang not supported [not_a_valid_lang]]"));
    }

    public void testScriptConditionThrowException() throws Exception {
        ScriptService scriptService = createScriptService(tp);
        ExecutableScriptCondition condition = new ExecutableScriptCondition(
                new ScriptCondition(WatcherScript.inline("null.foo").build()), logger, scriptService);
        SearchResponse response = new SearchResponse(InternalSearchResponse.empty(), "", 3, 3, 500L, new ShardSearchFailure[0]);
        WatchExecutionContext ctx = mockExecutionContext("_name", new Payload.XContent(response));
        ScriptException exception = expectThrows(ScriptException.class, () -> condition.execute(ctx));
        assertThat(exception.getMessage(), containsString("Error evaluating null.foo"));
    }

    public void testScriptConditionReturnObjectThrowsException() throws Exception {
        ScriptService scriptService = createScriptService(tp);
        ExecutableScriptCondition condition = new ExecutableScriptCondition(
                new ScriptCondition(WatcherScript.inline("return new Object()").build()), logger, scriptService);
        SearchResponse response = new SearchResponse(InternalSearchResponse.empty(), "", 3, 3, 500L, new ShardSearchFailure[0]);
        WatchExecutionContext ctx = mockExecutionContext("_name", new Payload.XContent(response));
        Exception exception = expectThrows(GeneralScriptException.class, () -> condition.execute(ctx));
        assertThat(exception.getMessage(),
                containsString("condition [script] must return a boolean value (true|false) but instead returned [_name]"));
    }

    public void testScriptConditionAccessCtx() throws Exception {
        ScriptService scriptService = createScriptService(tp);
        ExecutableScriptCondition condition = new ExecutableScriptCondition(
                new ScriptCondition(WatcherScript.inline("ctx.trigger.scheduled_time.getMillis() < new Date().time ").build()),
                logger, scriptService);
        SearchResponse response = new SearchResponse(InternalSearchResponse.empty(), "", 3, 3, 500L, new ShardSearchFailure[0]);
        WatchExecutionContext ctx = mockExecutionContext("_name", new DateTime(DateTimeZone.UTC), new Payload.XContent(response));
        Thread.sleep(10);
        assertThat(condition.execute(ctx).met(), is(true));
    }

    private static XContentBuilder createConditionContent(String script, String scriptLang, ScriptType scriptType) throws IOException {
        XContentBuilder builder = jsonBuilder();
        if (scriptType == null) {
            return builder.value(script);
        }
        builder.startObject();
        switch (scriptType) {
            case INLINE:
                builder.field("inline", script);
                break;
            case FILE:
                builder.field("file", script);
                break;
            case STORED:
                builder.field("id", script);
                break;
            default:
                throw illegalArgument("unsupported script type [{}]", scriptType);
        }
        if (scriptLang != null) {
            builder.field("lang", scriptLang);
        }
        return builder.endObject();
    }
}
