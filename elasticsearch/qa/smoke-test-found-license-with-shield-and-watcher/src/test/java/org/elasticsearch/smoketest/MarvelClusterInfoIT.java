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

package org.elasticsearch.smoketest;

import org.elasticsearch.Version;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.xpack.XPackPlugin;
import org.hamcrest.Matcher;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class MarvelClusterInfoIT extends ESIntegTestCase {

    static final String ADMIN_USER_PW = "test_admin:changeme";

    @Override
    protected Settings externalClusterClientSettings() {
        return Settings.builder()
                .put("shield.user", ADMIN_USER_PW)
                .build();
    }

    @Override
    protected Collection<Class<? extends Plugin>> transportClientPlugins() {
        return Collections.singletonList(XPackPlugin.class);
    }

    public void testMarvelClusterInfoCollectorWorks() throws Exception {
        final String clusterUUID = client().admin().cluster().prepareState().setMetaData(true).get().getState().metaData().clusterUUID();
        assertTrue(Strings.hasText(clusterUUID));
        awaitIndexExists(".marvel-es-data");
        ensureYellow(".marvel-es-data");
        awaitMarvelDocsCount(equalTo(1L), "cluster_info");
        GetResponse response = client().prepareGet(".marvel-es-data", "cluster_info", clusterUUID).get();
        assertTrue(".marvel-es-data" + " document does not exist", response.isExists());
        Map<String, Object> source = response.getSource();
        assertThat((String) source.get("cluster_name"), equalTo(cluster().getClusterName()));
        assertThat((String) source.get("version"), equalTo(Version.CURRENT.toString()));

        Object licenseObj = source.get("license");
        assertThat(licenseObj, nullValue());
    }

    protected void awaitMarvelDocsCount(Matcher<Long> matcher, String... types) throws Exception {
        flush();
        refresh();
        assertBusy(new Runnable() {
            @Override
            public void run() {
                assertMarvelDocsCount(matcher, types);
            }
        }, 30, TimeUnit.SECONDS);
    }

    protected void assertMarvelDocsCount(Matcher<Long> matcher, String... types) {
        try {
            long count = client().prepareSearch(".marvel-es-data").setSize(0)
                    .setTypes(types).get().getHits().totalHits();
            logger.trace("--> searched for [{}] documents, found [{}]", Strings.arrayToCommaDelimitedString(types), count);
            assertThat(count, matcher);
        } catch (IndexNotFoundException e) {
            assertThat(0L, matcher);
        }
    }

    protected void awaitIndexExists(final String... indices) throws Exception {
        assertBusy(new Runnable() {
            @Override
            public void run() {
                assertIndicesExists(indices);
            }
        }, 30, TimeUnit.SECONDS);
    }

    protected void assertIndicesExists(String... indices) {
        logger.trace("checking if index exists [{}]", Strings.arrayToCommaDelimitedString(indices));
        assertThat(client().admin().indices().prepareExists(indices).get().isExists(), is(true));
    }
}
