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

package org.elasticsearch.shield.tribe;

import org.apache.lucene.util.LuceneTestCase;
import org.elasticsearch.Version;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.license.plugin.LicensePlugin;
import org.elasticsearch.node.MockNode;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.plugins.PluginsService;
import org.elasticsearch.shield.ShieldPlugin;
import org.elasticsearch.test.ESTestCase;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.containsString;

/**
 * This class tests different scenarios around tribe node configuration, to make sure that we properly validate
 * tribes settings depending on how they will load shield or not. Main goal is to make sure that all tribes will run
 * shield too if the tribe node does.
 */
public class TribeShieldLoadedTests extends ESTestCase {

    @Test
    @AwaitsFix(bugUrl = "https://github.com/elastic/elasticsearch/issues/13212")
    // we really need to support loading plugins at the node level this way which should flow the plugins down to the tribe service, right now it doesnt!
    public void testShieldLoadedOnBothTribeNodeAndClients() {
        //all good if the plugin is loaded on both tribe node and tribe clients, no matter how it gets loaded (manually or from classpath)
        Settings.Builder builder = defaultSettings();

        try (Node node = new MockNode(builder.build(), Version.CURRENT, Arrays.asList(ShieldPlugin.class, LicensePlugin.class))) {
            node.start();
        }
    }

    //this test causes leaking threads to be left behind
    @LuceneTestCase.AwaitsFix(bugUrl = "https://github.com/elasticsearch/elasticsearch/issues/9107")
    @Test
    public void testShieldLoadedOnTribeNodeOnly() {
        //startup failure if any of the tribe clients doesn't have shield installed
        Settings.Builder builder = defaultSettings();

        try {
            NodeBuilder.nodeBuilder().settings(builder.build()).build();
            fail("node initialization should have failed due to missing shield plugin");
        } catch(Throwable t) {
            assertThat(t.getMessage(), containsString("Missing mandatory plugins [shield]"));
        }
    }

    @LuceneTestCase.AwaitsFix(bugUrl = "https://github.com/elasticsearch/elasticsearch/issues/9107")
    @Test
    public void testShieldMustBeLoadedOnAllTribes() {
        //startup failure if any of the tribe clients doesn't have shield installed
        Settings.Builder builder = addTribeSettings(defaultSettings(), "t2");

        try {
            NodeBuilder.nodeBuilder().settings(builder.build()).build();
            fail("node initialization should have failed due to missing shield plugin");
        } catch(Throwable t) {
            assertThat(t.getMessage(), containsString("Missing mandatory plugins [shield]"));
        }
    }

    private static Settings.Builder defaultSettings() {
        return addTribeSettings(Settings.builder()
                .put("node.name", "tribe_node")
                .put("path.home", createTempDir())
                .putArray("plugin.mandatory", ShieldPlugin.NAME, LicensePlugin.NAME), "t1");
    }

    private static Settings.Builder addTribeSettings(Settings.Builder settingsBuilder, String tribe) {
        String tribePrefix = "tribe." + tribe + ".";
        return settingsBuilder.put(tribePrefix + "cluster.name", "non_existing_cluster")
                .put(tribePrefix + "discovery.type", "local")
                .put(tribePrefix + "discovery.initial_state_timeout", 0)
                .putArray(tribePrefix + "plugin.mandatory", ShieldPlugin.NAME, LicensePlugin.NAME);
    }
}
