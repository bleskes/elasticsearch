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

package org.elasticsearch.alerts;

import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.test.ElasticsearchIntegrationTest;

import static org.hamcrest.core.IsEqual.equalTo;

/**
 */
public class ConfigTest extends ElasticsearchIntegrationTest {

    public void testOverridingSettings() throws Exception {
        TimeValue tv = TimeValue.timeValueMillis(10000);
        Settings oldSettings = ImmutableSettings.builder()
                .put("foo", tv)
                .put("bar", 1)
                .put("baz", true)
                .build();
        ConfigurationManager configurationManager = new ConfigurationManager(oldSettings, client());

        ClusterState clusterState = internalCluster().clusterService().state();
        boolean isReady = configurationManager.isReady(clusterState);
        assertTrue(isReady); //Should always be ready on a clean start
        Settings newSettings = ImmutableSettings.builder()
                .build();
        assertThat(configurationManager.getOverriddenIntValue("bar", newSettings, 0), equalTo(1));
        assertThat(configurationManager.getOverriddenBooleanValue("baz", newSettings, false), equalTo(true));
        assertThat(configurationManager.getOverriddenTimeValue("foo", newSettings, TimeValue.timeValueMillis(0)).getMillis(), equalTo(tv.getMillis()));
        TimeValue tv2 = TimeValue.timeValueMillis(0);
        newSettings = ImmutableSettings.builder()
                .put("foo", tv2)
                .put("bar", 100)
                .put("baz", false)
                .build();
        assertThat(configurationManager.getOverriddenIntValue("bar", newSettings, 0), equalTo(100));
        assertThat(configurationManager.getOverriddenBooleanValue("baz", newSettings, true), equalTo(false));
        assertThat(configurationManager.getOverriddenTimeValue("foo", newSettings, TimeValue.timeValueMillis(1)).getMillis(), equalTo(tv2.getMillis()));

    }

    public void testLoadingSettings() throws Exception {
        TimeValue tv = TimeValue.timeValueMillis(10000);
        Settings oldSettings = ImmutableSettings.builder()
                .put("foo", tv)
                .put("bar", 1)
                .put("baz", true)
                .build();

        TimeValue tv2 = TimeValue.timeValueMillis(10);
        Settings newSettings = ImmutableSettings.builder()
                .put("foo", tv2)
                .put("bar", 100)
                .put("baz", false)
                .build();

        XContentBuilder jsonBuilder = XContentFactory.jsonBuilder();
        jsonBuilder.startObject();
        newSettings.toXContent(jsonBuilder, ToXContent.EMPTY_PARAMS);
        jsonBuilder.endObject();
        IndexResponse indexResponse = client()
                .prepareIndex(ConfigurationManager.CONFIG_INDEX, ConfigurationManager.CONFIG_TYPE, "global")
                .setSource(jsonBuilder)
                .get();
        assertTrue(indexResponse.isCreated());
        ConfigurationManager configurationManager = new ConfigurationManager(oldSettings, client());
        assertTrue(configurationManager.isReady(internalCluster().clusterService().state()));
        Settings loadedSettings = configurationManager.getGlobalConfig();
        assertThat(loadedSettings.get("foo"), equalTo(newSettings.get("foo")));
        assertThat(loadedSettings.get("bar"), equalTo(newSettings.get("bar")));
        assertThat(loadedSettings.get("baz"), equalTo(newSettings.get("baz")));
    }
}
