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

import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.internal.InternalNode;
import org.elasticsearch.watcher.client.WatchSourceBuilder;
import org.elasticsearch.watcher.support.init.proxy.ScriptServiceProxy;
import org.elasticsearch.watcher.support.template.ScriptTemplate;
import org.elasticsearch.watcher.test.AbstractWatcherIntegrationTests;
import org.elasticsearch.watcher.trigger.TriggerBuilders;
import org.junit.Test;

import java.net.InetSocketAddress;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.watcher.actions.ActionBuilders.indexAction;
import static org.elasticsearch.watcher.client.WatchSourceBuilder.watchSourceBuilder;
import static org.elasticsearch.watcher.condition.ConditionBuilders.scriptCondition;
import static org.elasticsearch.watcher.input.InputBuilders.httpInput;
import static org.elasticsearch.watcher.trigger.schedule.Schedules.interval;

/**
 */
public class HttpInputIntegrationTest extends AbstractWatcherIntegrationTests {

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        return ImmutableSettings.builder()
                .put(InternalNode.HTTP_ENABLED, true)
                .put(super.nodeSettings(nodeOrdinal))
                .build();
    }

    @Override
    protected boolean shieldEnabled() {
        return false;
    }

    @Test
    public void testHttpInput() throws Exception {
        ScriptServiceProxy sc = scriptService();
        client().prepareIndex("index", "type", "id").setSource("{}").setRefresh(true).get();

        InetSocketAddress address = internalTestCluster().httpAddresses()[0];
        String body = jsonBuilder().startObject().field("size", 1).endObject().string();
        WatchSourceBuilder source = watchSourceBuilder()
                .trigger(TriggerBuilders.schedule(interval("5s")))
                .input(httpInput()
                                .setHost(address.getHostName())
                                .setPort(address.getPort())
                                .setPath(new ScriptTemplate(sc, "/index/_search"))
                                .setBody(new ScriptTemplate(sc, body))
                )
                .condition(scriptCondition("ctx.payload.hits.total == 1"))
                .addAction(indexAction("idx", "action"));
        watcherClient().preparePutWatch("_name")
                .source(source)
                .get();

        if (timeWarped()) {
            timeWarp().scheduler().trigger("_name");
            refresh();
        }
        assertWatchWithMinimumPerformedActionsCount("_name", 1, false);
    }

}
