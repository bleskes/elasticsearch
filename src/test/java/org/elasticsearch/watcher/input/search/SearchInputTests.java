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

package org.elasticsearch.watcher.input.search;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.joda.time.DateTimeZone;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.common.xcontent.support.XContentMapValues;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import org.elasticsearch.watcher.actions.ActionWrapper;
import org.elasticsearch.watcher.actions.ExecutableActions;
import org.elasticsearch.watcher.condition.always.ExecutableAlwaysCondition;
import org.elasticsearch.watcher.input.Input;
import org.elasticsearch.watcher.input.InputException;
import org.elasticsearch.watcher.input.simple.SimpleInput;
import org.elasticsearch.watcher.license.LicenseService;
import org.elasticsearch.watcher.support.WatcherUtils;
import org.elasticsearch.watcher.support.clock.ClockMock;
import org.elasticsearch.watcher.support.init.proxy.ClientProxy;
import org.elasticsearch.watcher.support.init.proxy.ScriptServiceProxy;
import org.elasticsearch.watcher.trigger.schedule.IntervalSchedule;
import org.elasticsearch.watcher.trigger.schedule.ScheduleTrigger;
import org.elasticsearch.watcher.trigger.schedule.ScheduleTriggerEvent;
import org.elasticsearch.watcher.execution.TriggeredExecutionContext;
import org.elasticsearch.watcher.watch.Payload;
import org.elasticsearch.watcher.watch.Watch;
import org.elasticsearch.watcher.execution.WatchExecutionContext;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.index.query.FilterBuilders.rangeFilter;
import static org.elasticsearch.index.query.QueryBuilders.filteredQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.search.builder.SearchSourceBuilder.searchSource;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;


/**
 */
public class SearchInputTests extends ElasticsearchIntegrationTest {

    @Test
    public void testExecute() throws Exception {
        SearchSourceBuilder searchSourceBuilder = searchSource().query(
                filteredQuery(matchQuery("event_type", "a"), rangeFilter("_timestamp").from("{{ctx.trigger.scheduled_time}}||-30s").to("{{ctx.trigger.triggered_time}}")));
        SearchRequest request = client()
                .prepareSearch()
                .setSearchType(SearchInput.DEFAULT_SEARCH_TYPE)
                .request()
                .source(searchSourceBuilder);

        SearchInput searchInput = new SearchInput(logger,
                ScriptServiceProxy.of(internalCluster().getInstance(ScriptService.class)),
                ClientProxy.of(client()), request, null);
        WatchExecutionContext ctx = new TriggeredExecutionContext(
                new Watch("test-watch",
                        new ClockMock(),
                        mock(LicenseService.class),
                        new ScheduleTrigger(new IntervalSchedule(new IntervalSchedule.Interval(1, IntervalSchedule.Interval.Unit.MINUTES))),
                        new SimpleInput(logger, new Payload.Simple()),
                        new ExecutableAlwaysCondition(logger),
                        null,
                        new ExecutableActions(new ArrayList<ActionWrapper>()),
                        null,
                        null,
                        new Watch.Status()),
                new DateTime(0, DateTimeZone.UTC),
                new ScheduleTriggerEvent(new DateTime(0, DateTimeZone.UTC), new DateTime(0, DateTimeZone.UTC)));
        SearchInput.Result result = searchInput.execute(ctx);

        assertThat((Integer) XContentMapValues.extractValue("hits.total", result.payload().data()), equalTo(0));
        assertNotNull(result.request());
        assertEquals(result.request().searchType(),request.searchType());
        assertArrayEquals(result.request().indices(), request.indices());
        assertEquals(result.request().indicesOptions(), request.indicesOptions());
    }

    @Test
    public void testDifferentSearchType() throws Exception {
        SearchSourceBuilder searchSourceBuilder = searchSource().query(
                filteredQuery(matchQuery("event_type", "a"), rangeFilter("_timestamp").from("{{ctx.trigger.scheduled_time}}||-30s").to("{{ctx.trigger.triggered_time}}"))
        );
        SearchType searchType = randomFrom(SearchType.values());
        SearchRequest request = client()
                .prepareSearch()
                .setSearchType(searchType)
                .request()
                .source(searchSourceBuilder);

        SearchInput searchInput = new SearchInput(logger,
                ScriptServiceProxy.of(internalCluster().getInstance(ScriptService.class)),
                ClientProxy.of(client()), request, null);
        WatchExecutionContext ctx = new TriggeredExecutionContext(
                new Watch("test-watch",
                        new ClockMock(),
                        mock(LicenseService.class),
                        new ScheduleTrigger(new IntervalSchedule(new IntervalSchedule.Interval(1, IntervalSchedule.Interval.Unit.MINUTES))),
                        new SimpleInput(logger, new Payload.Simple()),
                        new ExecutableAlwaysCondition(logger),
                        null,
                        new ExecutableActions(new ArrayList<ActionWrapper>()),
                        null,
                        null,
                        new Watch.Status()),
                new DateTime(0, DateTimeZone.UTC),
                new ScheduleTriggerEvent(new DateTime(0, DateTimeZone.UTC), new DateTime(0, DateTimeZone.UTC)));
        SearchInput.Result result = searchInput.execute(ctx);

        assertThat((Integer) XContentMapValues.extractValue("hits.total", result.payload().data()), equalTo(0));
        assertNotNull(result.request());
        assertEquals(result.request().searchType(), searchType);
        assertArrayEquals(result.request().indices(), request.indices());
        assertEquals(result.request().indicesOptions(), request.indicesOptions());
    }

    @Test
    public void testParser_Valid() throws Exception {
        SearchRequest request = client().prepareSearch()
                .setSearchType(SearchInput.DEFAULT_SEARCH_TYPE)
                .request()
                .source(searchSource()
                        .query(filteredQuery(matchQuery("event_type", "a"), rangeFilter("_timestamp").from("{{ctx.trigger.scheduled_time}}||-30s").to("{{ctx.trigger.triggered_time}}"))));

        SearchInput.SourceBuilder sourceBuilder = new SearchInput.SourceBuilder(request);
        XContentBuilder builder = jsonBuilder().value(sourceBuilder);
        XContentParser parser = JsonXContent.jsonXContent.createParser(builder.bytes());
        parser.nextToken();

        Input.Parser searchInputParser = new SearchInput.Parser(ImmutableSettings.EMPTY,
                ScriptServiceProxy.of(internalCluster().getInstance(ScriptService.class)),
                ClientProxy.of(client()));

        Input searchInput = searchInputParser.parse(parser);
        assertEquals(SearchInput.TYPE, searchInput.type());
    }

    @Test(expected = InputException.class)
    public void testParser_Invalid() throws Exception {
        Input.Parser searchInputParser = new SearchInput.Parser(ImmutableSettings.settingsBuilder().build(),
                ScriptServiceProxy.of(internalCluster().getInstance(ScriptService.class)),
                ClientProxy.of(client()));

        Map<String, Object> data = new HashMap<>();
        data.put("foo", "bar");
        data.put("baz", new ArrayList<String>() );

        XContentBuilder jsonBuilder = jsonBuilder();
        jsonBuilder.startObject();
        jsonBuilder.field(Input.Result.PAYLOAD_FIELD.getPreferredName(), data);
        jsonBuilder.endObject();

        searchInputParser.parseResult(XContentFactory.xContent(jsonBuilder.bytes()).createParser(jsonBuilder.bytes()));
        fail("result parsing should fail if payload is provided but request is missing");
    }

    @Test
    public void testResultParser() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("foo", "bar");
        data.put("baz", new ArrayList<String>() );

        SearchSourceBuilder searchSourceBuilder = searchSource().query(
                filteredQuery(matchQuery("event_type", "a"), rangeFilter("_timestamp").from("{{ctx.triggered.scheduled_time}}||-30s").to("{{ctx.triggered.triggered_time}}")));
        SearchRequest request = client()
                .prepareSearch()
                .setSearchType(SearchInput.DEFAULT_SEARCH_TYPE)
                .request()
                .source(searchSourceBuilder);

        XContentBuilder jsonBuilder = jsonBuilder();
        jsonBuilder.startObject();
        jsonBuilder.field(Input.Result.PAYLOAD_FIELD.getPreferredName(), data);
        jsonBuilder.field(SearchInput.Parser.REQUEST_FIELD.getPreferredName());
        WatcherUtils.writeSearchRequest(request, jsonBuilder, ToXContent.EMPTY_PARAMS);
        jsonBuilder.endObject();

        Input.Parser searchInputParser = new SearchInput.Parser(ImmutableSettings.settingsBuilder().build(),
                ScriptServiceProxy.of(internalCluster().getInstance(ScriptService.class)),
                ClientProxy.of(client()));

        Input.Result result = searchInputParser.parseResult(XContentFactory.xContent(jsonBuilder.bytes()).createParser(jsonBuilder.bytes()));

        assertEquals(SearchInput.TYPE, result.type());
        assertEquals(result.payload().data().get("foo"), "bar");
        List baz = (List)result.payload().data().get("baz");
        assertTrue(baz.isEmpty());

        assertTrue(result instanceof SearchInput.Result);
        SearchInput.Result searchInputResult = (SearchInput.Result) result;
        assertNotNull(searchInputResult.request());
    }
}
