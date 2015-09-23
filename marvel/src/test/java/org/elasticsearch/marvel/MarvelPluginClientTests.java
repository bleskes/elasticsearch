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

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.test.ESTestCase;
import org.junit.Test;

import java.util.Collection;

import static org.hamcrest.Matchers.is;

public class MarvelPluginClientTests extends ESTestCase {

    @Test
    public void testModulesWithClientSettings() {
        Settings settings = Settings.builder()
                .put(Client.CLIENT_TYPE_SETTING, TransportClient.CLIENT_TYPE)
                .build();

        MarvelPlugin plugin = new MarvelPlugin(settings);
        assertThat(plugin.isEnabled(), is(false));
        Collection<Module> modules = plugin.nodeModules();
        assertThat(modules.size(), is(0));
    }

    @Test
    public void testModulesWithNodeSettings() {
        // these settings mimic what ES does when running as a node...
        Settings settings = Settings.builder()
                .put(Client.CLIENT_TYPE_SETTING, "node")
                .build();
        MarvelPlugin plugin = new MarvelPlugin(settings);
        assertThat(plugin.isEnabled(), is(true));
        Collection<Module> modules = plugin.nodeModules();
        assertThat(modules.size(), is(6));
    }

}
