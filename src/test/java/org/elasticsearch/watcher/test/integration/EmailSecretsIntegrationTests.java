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

package org.elasticsearch.watcher.test.integration;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.support.XContentMapValues;
import org.elasticsearch.watcher.actions.email.service.EmailTemplate;
import org.elasticsearch.watcher.actions.email.service.support.EmailServer;
import org.elasticsearch.watcher.client.WatcherClient;
import org.elasticsearch.watcher.shield.ShieldSecretService;
import org.elasticsearch.watcher.support.secret.SecretService;
import org.elasticsearch.watcher.support.xcontent.XContentSource;
import org.elasticsearch.watcher.test.AbstractWatcherIntegrationTests;
import org.elasticsearch.watcher.transport.actions.execute.ExecuteWatchResponse;
import org.elasticsearch.watcher.transport.actions.get.GetWatchResponse;
import org.elasticsearch.watcher.trigger.TriggerEvent;
import org.elasticsearch.watcher.trigger.schedule.ScheduleTriggerEvent;
import org.elasticsearch.watcher.watch.WatchStore;
import org.junit.After;
import org.junit.Test;

import javax.mail.internet.MimeMessage;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.elasticsearch.common.joda.time.DateTimeZone.UTC;
import static org.elasticsearch.watcher.actions.ActionBuilders.emailAction;
import static org.elasticsearch.watcher.client.WatchSourceBuilders.watchBuilder;
import static org.elasticsearch.watcher.condition.ConditionBuilders.alwaysCondition;
import static org.elasticsearch.watcher.input.InputBuilders.simpleInput;
import static org.elasticsearch.watcher.trigger.TriggerBuilders.schedule;
import static org.elasticsearch.watcher.trigger.schedule.Schedules.cron;
import static org.hamcrest.Matchers.*;

/**
 *
 */
public class EmailSecretsIntegrationTests extends AbstractWatcherIntegrationTests {

    static final String USERNAME = "_user";
    static final String PASSWORD = "_passwd";

    private EmailServer server;
    private Boolean encryptSensitiveData;

    @After
    public void cleanup() throws Exception {
        server.stop();
    }

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        if(server == null) {
            //Need to construct the Email Server here as this happens before init()
            server = EmailServer.localhost("2500-2600", USERNAME, PASSWORD, logger);
        }
        if (encryptSensitiveData == null) {
            encryptSensitiveData = shieldEnabled() && randomBoolean();
        }
        return ImmutableSettings.builder()
                .put(super.nodeSettings(nodeOrdinal))
                .put("watcher.actions.email.service.account.test.smtp.auth", true)
                .put("watcher.actions.email.service.account.test.smtp.port", server.port())
                .put("watcher.actions.email.service.account.test.smtp.host", "localhost")
                .put("watcher.shield.encrypt_sensitive_data", encryptSensitiveData)
                .build();
    }

    @Test
    public void testEmail() throws Exception {
        WatcherClient watcherClient = watcherClient();
        watcherClient.preparePutWatch("_id")
                .setSource(watchBuilder()
                        .trigger(schedule(cron("0 0 0 1 * ? 2020")))
                        .input(simpleInput())
                        .condition(alwaysCondition())
                        .addAction("_email", emailAction(
                                EmailTemplate.builder()
                                        .from("_from")
                                        .to("_to")
                                        .subject("_subject"))
                                .setAuthentication(USERNAME, PASSWORD.toCharArray())))
                .get();

        // verifying the email password is stored encrypted in the index
        GetResponse response = client().prepareGet(WatchStore.INDEX, WatchStore.DOC_TYPE, "_id").get();
        assertThat(response, notNullValue());
        assertThat(response.getId(), is("_id"));
        Map<String, Object> source = response.getSource();
        Object value = XContentMapValues.extractValue("actions._email.email.password", source);
        assertThat(value, notNullValue());
        if (shieldEnabled() && encryptSensitiveData) {
            assertThat(value, not(is((Object) PASSWORD)));
            SecretService secretService = getInstanceFromMaster(SecretService.class);
            assertThat(secretService, instanceOf(ShieldSecretService.class));
            assertThat(new String(secretService.decrypt(((String) value).toCharArray())), is(PASSWORD));
        } else {
            assertThat(value, is((Object) PASSWORD));
            SecretService secretService = getInstanceFromMaster(SecretService.class);
            if (shieldEnabled()) {
                assertThat(secretService, instanceOf(ShieldSecretService.class));
            } else {
                assertThat(secretService, instanceOf(SecretService.PlainText.class));
            }
            assertThat(new String(secretService.decrypt(((String) value).toCharArray())), is(PASSWORD));
        }

        // verifying the password is not returned by the GET watch API
        GetWatchResponse watchResponse = watcherClient.prepareGetWatch("_id").get();
        assertThat(watchResponse, notNullValue());
        assertThat(watchResponse.getId(), is("_id"));
        XContentSource contentSource = watchResponse.getSource();
        value = contentSource.getValue("actions._email.email.password");
        assertThat(value, nullValue());

        // now we restart, to make sure the watches and their secrets are reloaded from the index properly
        assertThat(watcherClient.prepareWatchService().restart().get().isAcknowledged(), is(true));
        ensureWatcherStarted();

        // now lets execute the watch manually

        final CountDownLatch latch = new CountDownLatch(1);
        server.addListener(new EmailServer.Listener() {
            @Override
            public void on(MimeMessage message) throws Exception {
                assertThat(message.getSubject(), is("_subject"));
                latch.countDown();
            }
        });

        TriggerEvent triggerEvent = new ScheduleTriggerEvent(new DateTime(UTC), new DateTime(UTC));
        ExecuteWatchResponse executeResponse = watcherClient.prepareExecuteWatch("_id")
                .setRecordExecution(false)
                .setIgnoreThrottle(true)
                .setTriggerEvent(triggerEvent)
                .get();
        assertThat(executeResponse, notNullValue());
        contentSource = executeResponse.getSource();

        value = contentSource.getValue("execution_result.actions.0.email.status");
        assertThat((String) value, is("success"));

        if (!latch.await(5, TimeUnit.SECONDS)) {
            fail("waiting too long for the email to be sent");
        }
    }
}
