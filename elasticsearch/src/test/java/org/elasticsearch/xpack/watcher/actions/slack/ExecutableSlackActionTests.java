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

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.common.http.HttpClient;
import org.elasticsearch.xpack.common.http.HttpProxy;
import org.elasticsearch.xpack.common.http.HttpRequest;
import org.elasticsearch.xpack.common.http.HttpResponse;
import org.elasticsearch.xpack.common.text.TextTemplate;
import org.elasticsearch.xpack.notification.slack.SlackAccount;
import org.elasticsearch.xpack.notification.slack.SlackService;
import org.elasticsearch.xpack.notification.slack.message.SlackMessage;
import org.elasticsearch.xpack.watcher.execution.WatchExecutionContext;
import org.elasticsearch.xpack.watcher.execution.Wid;
import org.elasticsearch.xpack.watcher.test.MockTextTemplateEngine;
import org.elasticsearch.xpack.watcher.watch.Payload;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.mockito.ArgumentCaptor;

import static org.elasticsearch.xpack.watcher.test.WatcherTestUtils.mockExecutionContextBuilder;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExecutableSlackActionTests extends ESTestCase {

    public void testProxy() throws Exception {
        HttpProxy proxy = new HttpProxy("localhost", 8080);
        SlackMessage.Template messageTemplate = SlackMessage.Template.builder().addTo("to").setText(new TextTemplate("content")).build();
        SlackAction action = new SlackAction("account1", messageTemplate, proxy);

        HttpClient httpClient = mock(HttpClient.class);
        ArgumentCaptor<HttpRequest> argumentCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        when(httpClient.execute(argumentCaptor.capture())).thenReturn(new HttpResponse(200));

        Settings accountSettings = Settings.builder().put("url", "http://example.org").build();
        SlackAccount account = new SlackAccount("account1", accountSettings, Settings.EMPTY, httpClient, logger);

        SlackService service = mock(SlackService.class);
        when(service.getAccount(eq("account1"))).thenReturn(account);

        DateTime now = DateTime.now(DateTimeZone.UTC);

        Wid wid = new Wid(randomAsciiOfLength(5), now);
        WatchExecutionContext ctx = mockExecutionContextBuilder(wid.watchId())
                .wid(wid)
                .payload(new Payload.Simple())
                .time(wid.watchId(), now)
                .buildMock();

        ExecutableSlackAction executable = new ExecutableSlackAction(action, logger, service, new MockTextTemplateEngine());
        executable.execute("foo", ctx, new Payload.Simple());

        HttpRequest request = argumentCaptor.getValue();
        assertThat(request.proxy(), is(proxy));
    }

}
