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

package org.elasticsearch.marvel.shield;

import org.elasticsearch.ElasticsearchSecurityException;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.common.network.NetworkModule;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.marvel.MarvelSettings;
import org.elasticsearch.marvel.agent.exporter.MarvelTemplateUtils;
import org.elasticsearch.marvel.test.MarvelIntegTestCase;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.shield.InternalClient;

import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertAcked;
import static org.hamcrest.Matchers.is;

public class MarvelInternalClientTests extends MarvelIntegTestCase {

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        return Settings.builder()
                .put(super.nodeSettings(nodeOrdinal))
                .put(NetworkModule.HTTP_ENABLED.getKey(), false)
                .put(MarvelSettings.INTERVAL.getKey(), "-1")
                .build();
    }

    public void testAllowedAccess() {
        InternalClient internalClient = internalCluster().getInstance(InternalClient.class);

        assertAccessIsAllowed(internalClient.admin().cluster().prepareHealth());
        assertAccessIsAllowed(internalClient.admin().cluster().prepareClusterStats());
        assertAccessIsAllowed(internalClient.admin().cluster().prepareState());
        assertAccessIsAllowed(internalClient.admin().cluster().prepareNodesInfo());
        assertAccessIsAllowed(internalClient.admin().cluster().prepareNodesStats());
        assertAccessIsAllowed(internalClient.admin().cluster().prepareNodesHotThreads());

        assertAccessIsAllowed(internalClient.admin().indices().prepareGetSettings());
        assertAccessIsAllowed(internalClient.admin().indices().prepareSegments());
        assertAccessIsAllowed(internalClient.admin().indices().prepareRecoveries());
        assertAccessIsAllowed(internalClient.admin().indices().prepareStats());

        assertAccessIsAllowed(internalClient.admin().indices().prepareDelete(MONITORING_INDICES_PREFIX + "*"));
        assertAccessIsAllowed(internalClient.admin().indices().prepareCreate(MONITORING_INDICES_PREFIX + "test"));

        assertAccessIsAllowed(internalClient.admin().indices().preparePutTemplate("foo")
                .setSource(MarvelTemplateUtils.loadTimestampedIndexTemplate()));
        assertAccessIsAllowed(internalClient.admin().indices().prepareGetTemplates("foo"));
    }

    public void testAllowAllAccess() {
        InternalClient internalClient = internalCluster().getInstance(InternalClient.class);
        assertAcked(internalClient.admin().indices().preparePutTemplate("foo")
                .setSource(MarvelTemplateUtils.loadDataIndexTemplate()).get());

        assertAccessIsAllowed(internalClient.admin().indices().prepareDeleteTemplate("foo"));
        assertAccessIsAllowed(internalClient.admin().cluster().prepareGetRepositories());
    }

    public void assertAccessIsAllowed(ActionRequestBuilder request) {
        try {
            request.get();
        } catch (IndexNotFoundException e) {
            // Ok
        } catch (ElasticsearchSecurityException e) {
            fail("unexpected security exception: " + e.getMessage());
        }
    }

    public void assertAccessIsDenied(ActionRequestBuilder request) {
        try {
            request.get();
            fail("expected a security exception");
        } catch (IndexNotFoundException e) {
            // Ok
        } catch (ElasticsearchSecurityException e) {
            // expected
            assertThat(e.status(), is(RestStatus.FORBIDDEN));
        }
    }
}

