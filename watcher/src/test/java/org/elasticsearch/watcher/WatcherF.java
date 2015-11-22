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

package org.elasticsearch.watcher;

import org.elasticsearch.Version;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.SuppressForbidden;
import org.elasticsearch.license.plugin.LicensePlugin;
import org.elasticsearch.node.MockNode;
import org.elasticsearch.node.Node;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

/**
 * Main class to easily run Watcher from a IDE.
 * It sets all the options to run the Watcher plugin and access it from Sense, but doesn't run with Shield.
 *
 * In order to run this class set configure the following:
 * 1) Set `-Des.path.home=` to a directory containing an ES config directory
 */
public class WatcherF {

    @SuppressForbidden(reason = "not really code or a test")
    public static void main(String[] args) throws Throwable {
        Settings.Builder settings = Settings.builder();
        settings.put("http.cors.enabled", "true");
        settings.put("http.cors.allow-origin", "*");
        settings.put("script.inline", "on");
        settings.put("shield.enabled", "false");
        settings.put("security.manager.enabled", "false");
        settings.put("cluster.name", WatcherF.class.getSimpleName());

        // this is for the `test-watcher-integration` group level integration in HipChat
        settings.put("watcher.actions.hipchat.service.account.integration.profile", "integration");
        settings.put("watcher.actions.hipchat.service.account.integration.auth_token", "huuS9v7ccuOy3ZBWWWr1vt8Lqu3sQnLUE81nrLZU");
        settings.put("watcher.actions.hipchat.service.account.integration.room", "test-watcher");

        // this is for the Watcher Test account in HipChat
        settings.put("watcher.actions.hipchat.service.account.user.profile", "user");
        settings.put("watcher.actions.hipchat.service.account.user.auth_token", "FYVx16oDH78ZW9r13wtXbcszyoyA7oX5tiMWg9X0");

        // this is for the `test-watcher-v1` notification token
        settings.put("watcher.actions.hipchat.service.account.v1.profile", "v1");
        settings.put("watcher.actions.hipchat.service.account.v1.auth_token", "a734baf62df618b96dda55b323fc30");

        System.setProperty("es.watcher.actions.slack.service.account.a1.url", "https://hooks.slack.com/services/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");

        final CountDownLatch latch = new CountDownLatch(1);
        final Node node = new MockNode(settings.build(), Version.CURRENT, Arrays.asList(WatcherPlugin.class, LicensePlugin.class));
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                node.close();
                latch.countDown();
            }
        });
        node.start();
        latch.await();
    }

}
