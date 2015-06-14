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

package org.elasticsearch.watcher.test;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.base.Charsets;
import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.env.Environment;
import org.elasticsearch.node.settings.NodeSettingsService;
import org.elasticsearch.script.ScriptEngineService;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.script.groovy.GroovyScriptEngineService;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.watcher.ResourceWatcherService;
import org.elasticsearch.watcher.actions.ActionStatus;
import org.elasticsearch.watcher.actions.ActionWrapper;
import org.elasticsearch.watcher.actions.ExecutableActions;
import org.elasticsearch.watcher.actions.email.EmailAction;
import org.elasticsearch.watcher.actions.email.ExecutableEmailAction;
import org.elasticsearch.watcher.actions.email.service.*;
import org.elasticsearch.watcher.actions.webhook.ExecutableWebhookAction;
import org.elasticsearch.watcher.actions.webhook.WebhookAction;
import org.elasticsearch.watcher.condition.script.ExecutableScriptCondition;
import org.elasticsearch.watcher.condition.script.ScriptCondition;
import org.elasticsearch.watcher.execution.WatchExecutionContext;
import org.elasticsearch.watcher.execution.Wid;
import org.elasticsearch.watcher.input.search.ExecutableSearchInput;
import org.elasticsearch.watcher.input.simple.ExecutableSimpleInput;
import org.elasticsearch.watcher.input.simple.SimpleInput;
import org.elasticsearch.watcher.license.LicenseService;
import org.elasticsearch.watcher.support.Script;
import org.elasticsearch.watcher.support.WatcherUtils;
import org.elasticsearch.watcher.support.http.HttpClient;
import org.elasticsearch.watcher.support.http.HttpMethod;
import org.elasticsearch.watcher.support.http.HttpRequestTemplate;
import org.elasticsearch.watcher.support.init.proxy.ClientProxy;
import org.elasticsearch.watcher.support.init.proxy.ScriptServiceProxy;
import org.elasticsearch.watcher.support.secret.Secret;
import org.elasticsearch.watcher.support.template.Template;
import org.elasticsearch.watcher.support.template.TemplateEngine;
import org.elasticsearch.watcher.support.template.xmustache.XMustacheScriptEngineService;
import org.elasticsearch.watcher.support.template.xmustache.XMustacheTemplateEngine;
import org.elasticsearch.watcher.support.xcontent.ObjectPath;
import org.elasticsearch.watcher.transform.search.ExecutableSearchTransform;
import org.elasticsearch.watcher.transform.search.SearchTransform;
import org.elasticsearch.watcher.trigger.TriggerEvent;
import org.elasticsearch.watcher.trigger.schedule.CronSchedule;
import org.elasticsearch.watcher.trigger.schedule.ScheduleTrigger;
import org.elasticsearch.watcher.watch.Payload;
import org.elasticsearch.watcher.watch.Watch;
import org.elasticsearch.watcher.watch.WatchStatus;
import org.hamcrest.Matcher;

import javax.mail.internet.AddressException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.*;

import static com.carrotsearch.randomizedtesting.RandomizedTest.randomInt;
import static org.elasticsearch.common.joda.time.DateTimeZone.UTC;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.search.builder.SearchSourceBuilder.searchSource;
import static org.elasticsearch.test.ElasticsearchTestCase.randomFrom;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public final class WatcherTestUtils {

    public static final Payload EMPTY_PAYLOAD = new Payload.Simple(ImmutableMap.<String, Object>of());

    private WatcherTestUtils() {
    }

    public static void assertValue(Map<String, Object> map, String path, Matcher<?> matcher) {
        assertThat(ObjectPath.eval(path, map), (Matcher<Object>) matcher);
    }

    public static XContentParser xContentParser(XContentBuilder builder) throws IOException {
        return builder.contentType().xContent().createParser(builder.bytes());
    }

    public static SearchRequest newInputSearchRequest(String... indices) {
        SearchRequest request = new SearchRequest(indices);
        request.indicesOptions(WatcherUtils.DEFAULT_INDICES_OPTIONS);
        request.searchType(ExecutableSearchInput.DEFAULT_SEARCH_TYPE);
        return request;
    }

    public static SearchRequest matchAllRequest() {
        return matchAllRequest(null);
    }

    public static SearchRequest matchAllRequest(IndicesOptions indicesOptions) {
        // TODO (2.0 upgrade): move back to BytesReference, instead of converting to a string
        SearchRequest request = new SearchRequest(Strings.EMPTY_ARRAY)
                .source(SearchSourceBuilder.searchSource().query(matchAllQuery()).buildAsBytes(XContentType.JSON).toUtf8());
        if (indicesOptions != null) {
            request.indicesOptions(indicesOptions);
        }
        return request;
    }

    public static Payload simplePayload(String key, Object value) {
        return new Payload.Simple(key, value);
    }

    public static WatchExecutionContextMockBuilder mockExecutionContextBuilder(String watchId) {
        return new WatchExecutionContextMockBuilder(watchId)
                .wid(new Wid(watchId, randomInt(10), DateTime.now(UTC)));
    }

    public static WatchExecutionContext mockExecutionContext(String watchId, Payload payload) {
        return mockExecutionContextBuilder(watchId)
                .wid(new Wid(watchId, randomInt(10), DateTime.now(UTC)))
                .payload(payload)
                .buildMock();
    }

    public static WatchExecutionContext mockExecutionContext(String watchId, DateTime time, Payload payload) {
        return mockExecutionContextBuilder(watchId)
                .wid(new Wid(watchId, randomInt(10), DateTime.now(UTC)))
                .payload(payload)
                .time(watchId, time)
                .buildMock();
    }

    public static WatchExecutionContext mockExecutionContext(String watchId, DateTime executionTime, TriggerEvent event, Payload payload) {
        return mockExecutionContextBuilder(watchId)
                .wid(new Wid(watchId, randomInt(10), DateTime.now(UTC)))
                .payload(payload)
                .executionTime(executionTime)
                .triggerEvent(event)
                .buildMock();
    }


    public static Watch createTestWatch(String watchName, ScriptServiceProxy scriptService, HttpClient httpClient, EmailService emailService, ESLogger logger) throws AddressException {
        return createTestWatch(watchName, ClientProxy.of(ElasticsearchIntegrationTest.client()), scriptService, httpClient, emailService, logger);
    }


    public static Watch createTestWatch(String watchName, ClientProxy client, ScriptServiceProxy scriptService, HttpClient httpClient, EmailService emailService, ESLogger logger) throws AddressException {

        SearchRequest conditionRequest = newInputSearchRequest("my-condition-index").source(searchSource().query(matchAllQuery()));
        SearchRequest transformRequest = newInputSearchRequest("my-payload-index").source(searchSource().query(matchAllQuery()));
        transformRequest.searchType(ExecutableSearchTransform.DEFAULT_SEARCH_TYPE);
        conditionRequest.searchType(ExecutableSearchInput.DEFAULT_SEARCH_TYPE);

        List<ActionWrapper> actions = new ArrayList<>();

        HttpRequestTemplate.Builder httpRequest = HttpRequestTemplate.builder("localhost", 80);
        httpRequest.method(HttpMethod.POST);

        Template path = Template.inline("/foobarbaz/{{ctx.watch_id}}").build();
        httpRequest.path(path);
        Template body = Template.inline("{{ctx.watch_id}} executed with {{ctx.payload.response.hits.total_hits}} hits").build();
        httpRequest.body(body);

        TemplateEngine engine = new XMustacheTemplateEngine(ImmutableSettings.EMPTY, scriptService);

        actions.add(new ActionWrapper("_webhook", new ExecutableWebhookAction(new WebhookAction(httpRequest.build()), logger, httpClient, engine)));

        String from = "from@test.com";
        String to = "to@test.com";

        EmailTemplate email = EmailTemplate.builder()
                .from(from)
                .to(to)
                .build();

        TemplateEngine templateEngine = new XMustacheTemplateEngine(ImmutableSettings.EMPTY, scriptService);

        Authentication auth = new Authentication("testname", new Secret("testpassword".toCharArray()));

        EmailAction action = new EmailAction(email, "testaccount", auth, Profile.STANDARD, null);
        ExecutableEmailAction executale = new ExecutableEmailAction(action, logger, emailService, templateEngine, new HtmlSanitizer(ImmutableSettings.EMPTY));

        actions.add(new ActionWrapper("_email", executale));

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("foo", "bar");

        Map<String, Object> inputData = new LinkedHashMap<>();
        inputData.put("bar", "foo");

        LicenseService licenseService = mock(LicenseService.class);
        when(licenseService.enabled()).thenReturn(true);

        DateTime now = DateTime.now(UTC);
        return new Watch(
                watchName,
                new ScheduleTrigger(new CronSchedule("0/5 * * * * ? *")),
                new ExecutableSimpleInput(new SimpleInput(new Payload.Simple(inputData)), logger),
                new ExecutableScriptCondition(new ScriptCondition(Script.inline("return true").build()), logger, scriptService),
                new ExecutableSearchTransform(new SearchTransform(transformRequest), logger, client),
                new TimeValue(0),
                new ExecutableActions(actions),
                metadata,
                new WatchStatus(ImmutableMap.<String, ActionStatus>builder()
                        .put("_webhook", new ActionStatus(now))
                        .put("_email", new ActionStatus(now))
                        .build()));
    }

    public static ScriptServiceProxy getScriptServiceProxy(ThreadPool tp) throws Exception {
        Settings settings = ImmutableSettings.settingsBuilder()
                .put(ScriptService.DISABLE_DYNAMIC_SCRIPTING_SETTING, "none")
                .build();
        GroovyScriptEngineService groovyScriptEngineService = new GroovyScriptEngineService(settings);
        XMustacheScriptEngineService mustacheScriptEngineService = new XMustacheScriptEngineService(settings);
        Set<ScriptEngineService> engineServiceSet = new HashSet<>();
        engineServiceSet.add(mustacheScriptEngineService);
        engineServiceSet.add(groovyScriptEngineService);
        NodeSettingsService nodeSettingsService = new NodeSettingsService(settings);

        // TODO (2.0 upgrade): remove this reflection hack:
        Class scriptServiceClass = ScriptService.class;
        Constructor scriptServiceConstructor = scriptServiceClass.getConstructors()[0];
        if (scriptServiceConstructor.getParameterTypes().length == 5) {
            return ScriptServiceProxy.of((ScriptService) scriptServiceConstructor.newInstance(settings, new Environment(), engineServiceSet, new ResourceWatcherService(settings, tp), nodeSettingsService));
        } else if (scriptServiceConstructor.getParameterTypes().length == 6) {
            Class scriptContextRegistryClass = Class.forName("org.elasticsearch.script.ScriptContextRegistry");
            Constructor scriptContextRegistryConstructor = scriptContextRegistryClass.getDeclaredConstructors()[0];
            scriptContextRegistryConstructor.setAccessible(true);
            Object scriptContextRegistry = scriptContextRegistryConstructor.newInstance(Collections.emptyList());
            return ScriptServiceProxy.of((ScriptService) scriptServiceConstructor.newInstance(settings, new Environment(), engineServiceSet, new ResourceWatcherService(settings, tp), nodeSettingsService, scriptContextRegistry));
        } else {
            throw new RuntimeException("ScriptService is supposed to have 5 or 6 parameters in its constructor");
        }
    }

    public static SearchType getRandomSupportedSearchType() {
        return randomFrom(
                SearchType.COUNT,
                SearchType.DFS_QUERY_AND_FETCH,
                SearchType.DFS_QUERY_THEN_FETCH,
                SearchType.DFS_QUERY_AND_FETCH);
    }

    public static boolean areJsonEquivalent(String json1, String json2) throws IOException {
        XContentParser parser1 = XContentHelper.createParser(json1.getBytes(Charsets.UTF_8), 0, json1.getBytes(Charsets.UTF_8).length);
        XContentParser parser2 = XContentHelper.createParser(json2.getBytes(Charsets.UTF_8), 0, json2.getBytes(Charsets.UTF_8).length);
        Map<String, Object> map1 = parser1.map();
        Map<String, Object> map2 = parser2.map();
        return map1.equals(map2);
    }
}
