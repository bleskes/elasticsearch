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

import java.util.Collection;

import static org.hamcrest.Matchers.is;

public class MarvelPluginClientTests extends ESTestCase {

    public void testModulesWithClientSettings() {
        Settings settings = Settings.builder()
                .put(Client.CLIENT_TYPE_SETTING_S.getKey(), TransportClient.CLIENT_TYPE)
                .build();

        Monitoring plugin = new Monitoring(settings);
        assertThat(plugin.isEnabled(), is(true));
        assertThat(plugin.isTransportClient(), is(true));
    }

    public void testModulesWithNodeSettings() {
        // these settings mimic what ES does when running as a node...
        Settings settings = Settings.builder()
                .put(Client.CLIENT_TYPE_SETTING_S.getKey(), "node")
                .build();
        Monitoring plugin = new Monitoring(settings);
        assertThat(plugin.isEnabled(), is(true));
        assertThat(plugin.isTransportClient(), is(false));
    }
}
