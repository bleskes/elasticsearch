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

package org.elasticsearch.marvel;

import org.elasticsearch.Version;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.CollectionUtils;
import org.elasticsearch.license.plugin.LicensePlugin;
import org.elasticsearch.node.MockNode;
import org.elasticsearch.node.Node;
import org.elasticsearch.shield.ShieldPlugin;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

/**
 * Main class to easily run Marvel from a IDE.
 * <p>
 * In order to run this class set configure the following:
 * 1) Set `-Des.path.home=` to a directory containing an ES config directory
 *
 * It accepts collectors names as program arguments.
 */
public class MarvelF {

    public static void main(String[] args) throws Throwable {
        Settings.Builder settings = Settings.builder();
        settings.put("script.inline", "on");
        settings.put("security.manager.enabled", "false");
        settings.put("plugins.load_classpath_plugins", "false");
        settings.put("cluster.name", MarvelF.class.getSimpleName());
        settings.put("marvel.agent.interval", "5s");
        if (!CollectionUtils.isEmpty(args)) {
            settings.putArray("marvel.agent.collectors", args);
        }

        final CountDownLatch latch = new CountDownLatch(1);
        final Node node = new MockNode(settings.build(), Version.CURRENT, Arrays.asList(MarvelPlugin.class, LicensePlugin.class, ShieldPlugin.class));
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
