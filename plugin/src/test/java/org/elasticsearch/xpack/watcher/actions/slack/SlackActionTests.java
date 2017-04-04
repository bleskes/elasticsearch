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

package org.elasticsearch.xpack.watcher.actions.slack;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.common.http.HttpProxy;
import org.elasticsearch.xpack.common.http.HttpRequest;
import org.elasticsearch.xpack.common.http.HttpResponse;
import org.elasticsearch.xpack.common.text.TextTemplateEngine;
import org.elasticsearch.xpack.notification.slack.SentMessages;
import org.elasticsearch.xpack.notification.slack.SlackAccount;
import org.elasticsearch.xpack.notification.slack.SlackService;
import org.elasticsearch.xpack.notification.slack.message.SlackMessage;
import org.elasticsearch.xpack.notification.slack.message.SlackMessageDefaults;
import org.elasticsearch.xpack.notification.slack.message.SlackMessageTests;
import org.elasticsearch.xpack.watcher.actions.Action;
import org.elasticsearch.xpack.watcher.execution.WatchExecutionContext;
import org.elasticsearch.xpack.watcher.execution.Wid;
import org.elasticsearch.xpack.watcher.watch.Payload;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.xpack.watcher.test.WatcherTestUtils.mockExecutionContextBuilder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SlackActionTests extends ESTestCase {
    private SlackService service;

    @Before
    public void init() throws Exception {
        service = mock(SlackService.class);
    }

    public void testExecute() throws Exception {
        final String accountName = "account1";

        TextTemplateEngine templateEngine = mock(TextTemplateEngine.class);

        SlackMessage.Template messageTemplate = mock(SlackMessage.Template.class);
        SlackMessage message = mock(SlackMessage.class);

        SlackAction action = new SlackAction(accountName, messageTemplate, null);
        ExecutableSlackAction executable = new ExecutableSlackAction(action, logger, service, templateEngine);

        Map<String, Object> data = new HashMap<>();
        Payload payload = new Payload.Simple(data);

        Map<String, Object> metadata = MapBuilder.<String, Object>newMapBuilder().put("_key", "_val").map();

        DateTime now = DateTime.now(DateTimeZone.UTC);

        Wid wid = new Wid(randomAlphaOfLength(5), now);
        WatchExecutionContext ctx = mockExecutionContextBuilder(wid.watchId())
                .wid(wid)
                .payload(payload)
                .time(wid.watchId(), now)
                .metadata(metadata)
                .buildMock();

        Map<String, Object> triggerModel = new HashMap<>();
        triggerModel.put("triggered_time", now);
        triggerModel.put("scheduled_time", now);
        Map<String, Object> ctxModel = new HashMap<>();
        ctxModel.put("id", ctx.id().value());
        ctxModel.put("watch_id", wid.watchId());
        ctxModel.put("payload", data);
        ctxModel.put("metadata", metadata);
        ctxModel.put("execution_time", now);
        ctxModel.put("trigger", triggerModel);
        ctxModel.put("vars", emptyMap());
        Map<String, Object> expectedModel = singletonMap("ctx", ctxModel);

        when(messageTemplate.render(eq(wid.watchId()), eq("_action"), eq(templateEngine), eq(expectedModel),
                any(SlackMessageDefaults.class))).thenReturn(message);
        SlackAccount account = mock(SlackAccount.class);
        when(service.getAccount(accountName)).thenReturn(account);


        List<SentMessages.SentMessage> messages = new ArrayList<>();
        boolean hasError = false;
        boolean hasSuccess = false;
        int count = randomIntBetween(1, 2);
        for (int i = 0; i < count; i++) {
            HttpResponse response = mock(HttpResponse.class);
            HttpRequest request = mock(HttpRequest.class);
            switch (randomIntBetween(0, 2)) {
                case 0:
                    messages.add(SentMessages.SentMessage.error(randomAlphaOfLength(10), message, "unknown error"));
                    hasError = true;
                    break;
                case 1:
                    when(response.status()).thenReturn(randomIntBetween(300, 600)); // error reponse
                    messages.add(SentMessages.SentMessage.responded(randomAlphaOfLength(10), message, request, response));
                    hasError = true;
                    break;
                case 2:
                    when(response.status()).thenReturn(randomIntBetween(200, 299)); // success
                    messages.add(SentMessages.SentMessage.responded(randomAlphaOfLength(10), message, request, response));
                    hasSuccess = true;
            }
        }
        SentMessages sentMessages = new SentMessages(accountName, messages);
        when(account.send(message, eq(any()))).thenReturn(sentMessages);

        Action.Result.Status expectedStatus = !hasError ? Action.Result.Status.SUCCESS :
                !hasSuccess ? Action.Result.Status.FAILURE :
                        Action.Result.Status.PARTIAL_FAILURE;


        Action.Result result = executable.execute("_action", ctx, payload);

        assertThat(result, notNullValue());
        assertThat(result, instanceOf(SlackAction.Result.Executed.class));
        assertThat(result.status(), equalTo(expectedStatus));
        assertThat(((SlackAction.Result.Executed) result).sentMessages(), sameInstance(sentMessages));
    }

    public void testParser() throws Exception {
        XContentBuilder builder = jsonBuilder().startObject();

        String accountName = randomAlphaOfLength(10);
        SlackMessage.Template message = SlackMessageTests.createRandomTemplate();

        builder.field("account", accountName);
        builder.field("message", message, ToXContent.EMPTY_PARAMS);
        builder.endObject();

        BytesReference bytes = builder.bytes();
        logger.info("slack action json [{}]", bytes.utf8ToString());
        XContentParser parser = createParser(JsonXContent.jsonXContent, bytes);
        parser.nextToken();

        SlackAction action = SlackAction.parse("_watch", "_action", parser);

        assertThat(action, notNullValue());
        assertThat(action.account, is(accountName));
        assertThat(action.message, notNullValue());
        assertThat(action.message, is(message));
    }

    public void testParserSelfGenerated() throws Exception {
        String accountName = randomBoolean() ? randomAlphaOfLength(10) : null;
        SlackMessage.Template message = SlackMessageTests.createRandomTemplate();

        HttpProxy proxy = null;
        if (randomBoolean()) {
            proxy = new HttpProxy("localhost", 8080);
        }
        SlackAction action = new SlackAction(accountName, message, proxy);

        XContentBuilder builder = jsonBuilder();
        action.toXContent(builder, ToXContent.EMPTY_PARAMS);
        BytesReference bytes = builder.bytes();
        logger.info("{}", bytes.utf8ToString());
        XContentParser parser = createParser(JsonXContent.jsonXContent, bytes);
        parser.nextToken();

        SlackAction parsedAction = SlackAction.parse("_watch", "_action", parser);

        assertThat(parsedAction, notNullValue());
        assertThat(parsedAction, is(action));
        assertThat(parsedAction.proxy, is(action.proxy));
    }

    public void testParserInvalid() throws Exception {
        XContentBuilder builder = jsonBuilder().startObject().field("unknown_field", "value").endObject();
        XContentParser parser = createParser(builder);
        parser.nextToken();
        try {
            SlackAction.parse("_watch", "_action", parser);
            fail("Expected ElasticsearchParseException");
        } catch (ElasticsearchParseException e) {
            assertThat(e.getMessage(), is("failed to parse [slack] action [_watch/_action]. unexpected token [VALUE_STRING]"));
        }
    }
}
