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

package org.elasticsearch.watcher.shield;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.PluginsService;
import org.elasticsearch.shield.ShieldPlugin;
import org.elasticsearch.shield.authc.support.SecuredString;
import org.elasticsearch.shield.authz.AuthorizationException;
import org.elasticsearch.watcher.WatcherPlugin;
import org.elasticsearch.watcher.WatcherState;
import org.elasticsearch.watcher.test.AbstractWatcherIntegrationTests;
import org.elasticsearch.watcher.transport.actions.delete.DeleteWatchResponse;
import org.elasticsearch.watcher.transport.actions.execute.ExecuteWatchResponse;
import org.elasticsearch.watcher.transport.actions.get.GetWatchResponse;
import org.elasticsearch.watcher.transport.actions.put.PutWatchResponse;
import org.elasticsearch.watcher.transport.actions.stats.WatcherStatsResponse;
import org.elasticsearch.watcher.trigger.TriggerEvent;
import org.elasticsearch.watcher.trigger.schedule.IntervalSchedule;
import org.elasticsearch.watcher.trigger.schedule.ScheduleTriggerEvent;
import org.joda.time.DateTime;
import org.junit.Test;

import static org.elasticsearch.shield.authc.support.UsernamePasswordToken.basicAuthHeaderValue;
import static org.elasticsearch.watcher.client.WatchSourceBuilders.watchBuilder;
import static org.elasticsearch.watcher.trigger.TriggerBuilders.schedule;
import static org.elasticsearch.watcher.trigger.schedule.Schedules.interval;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.joda.time.DateTimeZone.UTC;

public class BasicShieldTests extends AbstractWatcherIntegrationTests {

    @Override
    protected boolean enableShield() {
        return true;
    }

    @Override
    protected Settings transportClientSettings() {
        return Settings.builder()
                .put("client.transport.sniff", false)
                .put("plugin.types", ShieldPlugin.class.getName() + "," + WatcherPlugin.class.getName())
                // Use just the transport user here, so we can test Watcher roles specifically
                .put("shield.user", "transport_client:changeme")
                .put(PluginsService.LOAD_PLUGIN_FROM_CLASSPATH, false)
                .build();
    }

    @Test
    public void testNoAuthorization() throws Exception {
        try {
            watcherClient().prepareWatcherStats().get();
            fail("authentication failure should have occurred");
        } catch (AuthorizationException e) {
            // transport_client is the default user
            assertThat(e.getMessage(), equalTo("action [cluster:monitor/watcher/stats] is unauthorized for user [transport_client]"));
        }
    }

    @Test
    public void testWatcherMonitorRole() throws Exception {
        // stats and get watch apis require at least monitor role:
        String token = basicAuthHeaderValue("test", new SecuredString("changeme".toCharArray()));
        try {
            watcherClient().prepareWatcherStats()
                    .putHeader("Authorization", token)
                    .get();
            fail("authentication failure should have occurred");
        } catch (AuthorizationException e) {
            assertThat(e.getMessage(), equalTo("action [cluster:monitor/watcher/stats] is unauthorized for user [test]"));
        }

        try {
            watcherClient().prepareGetWatch("_id")
                    .putHeader("Authorization", token)
                    .get();
            fail("authentication failure should have occurred");
        } catch (AuthorizationException e) {
            assertThat(e.getMessage(), equalTo("action [cluster:monitor/watcher/watch/get] is unauthorized for user [test]"));
        }

        // stats and get watch are allowed by role monitor:
        token = basicAuthHeaderValue("monitor", new SecuredString("changeme".toCharArray()));
        WatcherStatsResponse statsResponse = watcherClient().prepareWatcherStats()
                .putHeader("Authorization", token)
                .get();
        assertThat(statsResponse.getWatcherState(), equalTo(WatcherState.STARTED));
        GetWatchResponse getWatchResponse = watcherClient().prepareGetWatch("_id")
                .putHeader("Authorization", token)
                .get();
        assertThat(getWatchResponse.isFound(), is(false));

        // but put watch isn't allowed by monitor:
        try {
            watcherClient().preparePutWatch("_id")
                    .setSource(watchBuilder().trigger(schedule(interval(5, IntervalSchedule.Interval.Unit.SECONDS))))
                    .putHeader("Authorization", token)
                    .get();
            fail("authentication failure should have occurred");
        } catch (AuthorizationException e) {
            assertThat(e.getMessage(), equalTo("action [cluster:admin/watcher/watch/put] is unauthorized for user [monitor]"));
        }
    }

    @Test
    public void testWatcherAdminRole() throws Exception {
        // put, execute and delete watch apis requires watcher admin role:
        String token = basicAuthHeaderValue("test", new SecuredString("changeme".toCharArray()));
        try {
            watcherClient().preparePutWatch("_id")
                    .setSource(watchBuilder().trigger(schedule(interval(5, IntervalSchedule.Interval.Unit.SECONDS))))
                    .putHeader("Authorization", token)
                    .get();
            fail("authentication failure should have occurred");
        } catch (AuthorizationException e) {
            assertThat(e.getMessage(), equalTo("action [cluster:admin/watcher/watch/put] is unauthorized for user [test]"));
        }

        TriggerEvent triggerEvent = new ScheduleTriggerEvent(new DateTime(UTC), new DateTime(UTC));
        try {
            watcherClient().prepareExecuteWatch("_id")
                    .setTriggerEvent(triggerEvent)
                    .putHeader("Authorization", token)
                    .get();
            fail("authentication failure should have occurred");
        } catch (AuthorizationException e) {
            assertThat(e.getMessage(), equalTo("action [cluster:admin/watcher/watch/execute] is unauthorized for user [test]"));
        }

        try {
            watcherClient().prepareDeleteWatch("_id")
                    .putHeader("Authorization", token)
                    .get();
            fail("authentication failure should have occurred");
        } catch (AuthorizationException e) {
            assertThat(e.getMessage(), equalTo("action [cluster:admin/watcher/watch/delete] is unauthorized for user [test]"));
        }

        // put, execute and delete watch apis are allowed by role admin:
        token = basicAuthHeaderValue("admin", new SecuredString("changeme".toCharArray()));
        PutWatchResponse putWatchResponse = watcherClient().preparePutWatch("_id")
                .setSource(watchBuilder().trigger(schedule(interval(5, IntervalSchedule.Interval.Unit.SECONDS))))
                .putHeader("Authorization", token)
                .get();
        assertThat(putWatchResponse.getVersion(), equalTo(1l));
        ExecuteWatchResponse executeWatchResponse = watcherClient().prepareExecuteWatch("_id")
                .setTriggerEvent(triggerEvent)
                .putHeader("Authorization", token)
                .get();
        DeleteWatchResponse deleteWatchResponse = watcherClient().prepareDeleteWatch("_id")
                .putHeader("Authorization", token)
                .get();
        assertThat(deleteWatchResponse.getVersion(), equalTo(2l));
        assertThat(deleteWatchResponse.isFound(), is(true));

        // stats and get watch are also allowed by role monitor:
        token = basicAuthHeaderValue("admin", new SecuredString("changeme".toCharArray()));
        WatcherStatsResponse statsResponse = watcherClient().prepareWatcherStats()
                .putHeader("Authorization", token)
                .get();
        assertThat(statsResponse.getWatcherState(), equalTo(WatcherState.STARTED));
        GetWatchResponse getWatchResponse = watcherClient().prepareGetWatch("_id")
                .putHeader("Authorization", token)
                .get();
        assertThat(getWatchResponse.isFound(), is(false));
    }

}
