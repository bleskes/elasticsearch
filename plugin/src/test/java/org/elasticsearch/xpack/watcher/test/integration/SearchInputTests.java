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

package org.elasticsearch.xpack.watcher.test.integration;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.search.SearchAction;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.search.ShardSearchFailure;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.env.Environment;
import org.elasticsearch.script.MockMustacheScriptEngine;
import org.elasticsearch.script.ScriptContext;
import org.elasticsearch.script.ScriptContextRegistry;
import org.elasticsearch.script.ScriptEngineRegistry;
import org.elasticsearch.script.ScriptEngineService;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.script.ScriptSettings;
import org.elasticsearch.search.SearchModule;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.internal.InternalSearchResponse;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.watcher.ResourceWatcherService;
import org.elasticsearch.xpack.watcher.condition.AlwaysCondition;
import org.elasticsearch.xpack.watcher.execution.TriggeredExecutionContext;
import org.elasticsearch.xpack.watcher.execution.WatchExecutionContext;
import org.elasticsearch.xpack.watcher.input.Input;
import org.elasticsearch.xpack.watcher.input.search.ExecutableSearchInput;
import org.elasticsearch.xpack.watcher.input.search.SearchInput;
import org.elasticsearch.xpack.watcher.input.search.SearchInputFactory;
import org.elasticsearch.xpack.watcher.input.simple.ExecutableSimpleInput;
import org.elasticsearch.xpack.watcher.input.simple.SimpleInput;
import org.elasticsearch.xpack.watcher.support.init.proxy.WatcherClientProxy;
import org.elasticsearch.xpack.watcher.support.search.WatcherSearchTemplateRequest;
import org.elasticsearch.xpack.watcher.support.search.WatcherSearchTemplateService;
import org.elasticsearch.xpack.watcher.test.WatcherTestUtils;
import org.elasticsearch.xpack.watcher.trigger.schedule.IntervalSchedule;
import org.elasticsearch.xpack.watcher.trigger.schedule.ScheduleTrigger;
import org.elasticsearch.xpack.watcher.trigger.schedule.ScheduleTriggerEvent;
import org.elasticsearch.xpack.watcher.watch.Payload;
import org.elasticsearch.xpack.watcher.watch.Watch;
import org.elasticsearch.xpack.watcher.watch.WatchStatus;
import org.joda.time.DateTime;
import org.junit.Before;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyMap;
import static org.elasticsearch.common.unit.TimeValue.timeValueSeconds;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.rangeQuery;
import static org.elasticsearch.mock.orig.Mockito.when;
import static org.elasticsearch.search.builder.SearchSourceBuilder.searchSource;
import static org.elasticsearch.xpack.watcher.test.WatcherTestUtils.getRandomSupportedSearchType;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.joda.time.DateTimeZone.UTC;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class SearchInputTests extends ESTestCase {

    private ScriptService scriptService;
    private Client client;

    @Before
    public void setup() throws Exception {
        List<ScriptEngineService> scriptEngineServices = new ArrayList<>();
        scriptEngineServices.add(new MockMustacheScriptEngine());
        ScriptEngineRegistry engineRegistry = new ScriptEngineRegistry(scriptEngineServices);

        ThreadPool threadPool = mock(ThreadPool.class);
        Settings settings = Settings.builder().put("path.home", createTempDir()).build();
        ResourceWatcherService resourceWatcherService = new ResourceWatcherService(settings, threadPool);
        Environment environment = new Environment(settings);
        Collection<ScriptContext.Plugin> customScriptContexts = Collections.singletonList(new ScriptContext.Plugin("xpack", "watch"));
        ScriptContextRegistry scriptContextRegistry = new ScriptContextRegistry(customScriptContexts);
        ScriptSettings scriptSettings = new ScriptSettings(engineRegistry, scriptContextRegistry);
        scriptService = new ScriptService(settings, environment, resourceWatcherService, engineRegistry, scriptContextRegistry,
                scriptSettings);

        client = mock(Client.class);
        when(client.settings()).thenReturn(settings);
    }

    public void testExecute() throws Exception {
        ArgumentCaptor<SearchRequest> requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        SearchResponse searchResponse = new SearchResponse(InternalSearchResponse.empty(), "", 1, 1, 1234, ShardSearchFailure.EMPTY_ARRAY);
        doAnswer(invocation -> {
            ActionListener listener = (ActionListener) invocation.getArguments()[2];
            listener.onResponse(searchResponse);
            return listener;
        }).when(client).execute(eq(SearchAction.INSTANCE), requestCaptor.capture(), anyObject());

        SearchSourceBuilder searchSourceBuilder = searchSource().query(boolQuery().must(matchQuery("event_type", "a")));

        WatcherSearchTemplateRequest request = WatcherTestUtils.templateRequest(searchSourceBuilder);
        ExecutableSearchInput searchInput = new ExecutableSearchInput(new SearchInput(request, null, null, null), logger,
                WatcherClientProxy.of(client), watcherSearchTemplateService(), TimeValue.timeValueMinutes(1));
        WatchExecutionContext ctx = createExecutionContext();
        SearchInput.Result result = searchInput.execute(ctx, new Payload.Simple());

        assertThat(result.status(), is(Input.Result.Status.SUCCESS));
        SearchRequest searchRequest = requestCaptor.getValue();
        assertThat(searchRequest.searchType(), is(request.getSearchType()));
        assertThat(searchRequest.indicesOptions(), is(request.getIndicesOptions()));
        assertThat(searchRequest.indices(), is(arrayContainingInAnyOrder(request.getIndices())));

    }

    private TriggeredExecutionContext createExecutionContext() {
        return new TriggeredExecutionContext(
                new Watch("test-watch",
                        new ScheduleTrigger(new IntervalSchedule(new IntervalSchedule.Interval(1, IntervalSchedule.Interval.Unit.MINUTES))),
                        new ExecutableSimpleInput(new SimpleInput(new Payload.Simple()), logger),
                        AlwaysCondition.INSTANCE,
                        null,
                        null,
                        new ArrayList<>(),
                        null,
                        new WatchStatus(new DateTime(0, UTC), emptyMap())),
                new DateTime(0, UTC),
                new ScheduleTriggerEvent("test-watch", new DateTime(0, UTC), new DateTime(0, UTC)),
                timeValueSeconds(5));
    }

    public void testDifferentSearchType() throws Exception {
        ArgumentCaptor<SearchRequest> requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        SearchResponse searchResponse = new SearchResponse(InternalSearchResponse.empty(), "", 1, 1, 1234, ShardSearchFailure.EMPTY_ARRAY);
        doAnswer(invocation -> {
            ActionListener listener = (ActionListener) invocation.getArguments()[2];
            listener.onResponse(searchResponse);
            return listener;
        }).when(client).execute(eq(SearchAction.INSTANCE), requestCaptor.capture(), anyObject());


        SearchSourceBuilder searchSourceBuilder = searchSource().query(boolQuery().must(matchQuery("event_type", "a")));
        SearchType searchType = getRandomSupportedSearchType();
        WatcherSearchTemplateRequest request = WatcherTestUtils.templateRequest(searchSourceBuilder, searchType);

        ExecutableSearchInput searchInput = new ExecutableSearchInput(new SearchInput(request, null, null, null), logger,
                WatcherClientProxy.of(client), watcherSearchTemplateService(), TimeValue.timeValueMinutes(1));
        WatchExecutionContext ctx = createExecutionContext();
        SearchInput.Result result = searchInput.execute(ctx, new Payload.Simple());

        assertThat(result.status(), is(Input.Result.Status.SUCCESS));
        SearchRequest searchRequest = requestCaptor.getValue();
        assertThat(searchRequest.searchType(), is(request.getSearchType()));
        assertThat(searchRequest.indicesOptions(), is(request.getIndicesOptions()));
        assertThat(searchRequest.indices(), is(arrayContainingInAnyOrder(request.getIndices())));
    }

    public void testParserValid() throws Exception {
        SearchSourceBuilder source = searchSource()
                        .query(boolQuery().must(matchQuery("event_type", "a")).must(rangeQuery("_timestamp")
                                .from("{{ctx.trigger.scheduled_time}}||-30s").to("{{ctx.trigger.triggered_time}}")));

        TimeValue timeout = randomBoolean() ? TimeValue.timeValueSeconds(randomInt(10)) : null;
        XContentBuilder builder = jsonBuilder().value(new SearchInput(WatcherTestUtils.templateRequest(source), null, timeout, null));
        XContentParser parser = createParser(builder);
        parser.nextToken();

        SearchInputFactory factory = new SearchInputFactory(Settings.EMPTY, WatcherClientProxy.of(client),
                                                            xContentRegistry(), scriptService);

        SearchInput searchInput = factory.parseInput("_id", parser, false);
        assertEquals(SearchInput.TYPE, searchInput.type());
        assertThat(searchInput.getTimeout(), equalTo(timeout));
    }

    // source: https://discuss.elastic.co/t/need-help-for-energy-monitoring-system-alerts/89415/3
    public void testThatEmptyRequestBodyWorks() throws Exception {
        ArgumentCaptor<SearchRequest> requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        SearchResponse searchResponse = new SearchResponse(InternalSearchResponse.empty(), "", 1, 1, 1234, ShardSearchFailure.EMPTY_ARRAY);
        doAnswer(invocation -> {
            ActionListener listener = (ActionListener) invocation.getArguments()[2];
            listener.onResponse(searchResponse);
            return listener;
        }).when(client).execute(eq(SearchAction.INSTANCE), requestCaptor.capture(), anyObject());


        try (XContentBuilder builder = jsonBuilder().startObject().startObject("request")
                .startArray("indices").value("foo").endArray().endObject().endObject();
             XContentParser parser = XContentFactory.xContent(XContentType.JSON).createParser(NamedXContentRegistry.EMPTY,
                     builder.bytes())) {

            parser.nextToken(); // advance past the first starting object

            SearchInputFactory factory = new SearchInputFactory(Settings.EMPTY, WatcherClientProxy.of(client), xContentRegistry(),
                    scriptService);
            SearchInput input = factory.parseInput("my-watch", parser, false);
            assertThat(input.getRequest(), is(not(nullValue())));
            assertThat(input.getRequest().getSearchSource(), is(BytesArray.EMPTY));

            ExecutableSearchInput executableSearchInput = factory.createExecutable(input);
            WatchExecutionContext ctx = createExecutionContext();
            SearchInput.Result result = executableSearchInput.execute(ctx, Payload.Simple.EMPTY);
            assertThat(result.status(), is(Input.Result.Status.SUCCESS));
            // no body in the search request
            ToXContent.Params params = new ToXContent.MapParams(Collections.singletonMap("pretty", "false"));
            assertThat(requestCaptor.getValue().source().toString(params), is("{}"));
        }
    }

    private WatcherSearchTemplateService watcherSearchTemplateService() {
        SearchModule module = new SearchModule(Settings.EMPTY, false, Collections.emptyList());
        return new WatcherSearchTemplateService(Settings.EMPTY, scriptService, new NamedXContentRegistry(module.getNamedXContents()));
    }
}
