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
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.marvel.agent.exporter.MarvelTemplateUtils;
import org.elasticsearch.marvel.agent.settings.MarvelSettings;
import org.elasticsearch.marvel.test.MarvelIntegTestCase;
import org.elasticsearch.rest.RestStatus;

import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertAcked;
import static org.hamcrest.Matchers.is;

public class SecuredClientTests extends MarvelIntegTestCase {

    public void testAllowedAccess() {
        SecuredClient securedClient = internalCluster().getInstance(SecuredClient.class);

        assertAccessIsAllowed(securedClient.admin().cluster().prepareHealth());
        assertAccessIsAllowed(securedClient.admin().cluster().prepareClusterStats());
        assertAccessIsAllowed(securedClient.admin().cluster().prepareState());
        assertAccessIsAllowed(securedClient.admin().cluster().prepareNodesInfo());
        assertAccessIsAllowed(securedClient.admin().cluster().prepareNodesStats());
        assertAccessIsAllowed(securedClient.admin().cluster().prepareNodesHotThreads());

        assertAccessIsAllowed(securedClient.admin().indices().prepareGetSettings());
        assertAccessIsAllowed(securedClient.admin().indices().prepareSegments());
        assertAccessIsAllowed(securedClient.admin().indices().prepareRecoveries());
        assertAccessIsAllowed(securedClient.admin().indices().prepareStats());

        assertAccessIsAllowed(securedClient.admin().indices().prepareDelete(MarvelSettings.MARVEL_INDICES_PREFIX));
        assertAccessIsAllowed(securedClient.admin().indices().prepareCreate(MarvelSettings.MARVEL_INDICES_PREFIX + "test"));

        assertAccessIsAllowed(securedClient.admin().indices().preparePutTemplate("foo").setSource(MarvelTemplateUtils.loadDefaultTemplate()));
        assertAccessIsAllowed(securedClient.admin().indices().prepareGetTemplates("foo"));
    }

    public void testDeniedAccess() {
        SecuredClient securedClient = internalCluster().getInstance(SecuredClient.class);
        assertAcked(securedClient.admin().indices().preparePutTemplate("foo").setSource(MarvelTemplateUtils.loadDefaultTemplate()).get());

        if (shieldEnabled) {
            assertAccessIsDenied(securedClient.admin().indices().prepareDeleteTemplate("foo"));
            assertAccessIsDenied(securedClient.admin().cluster().prepareGetRepositories());
        } else {
            assertAccessIsAllowed(securedClient.admin().indices().prepareDeleteTemplate("foo"));
            assertAccessIsAllowed(securedClient.admin().cluster().prepareGetRepositories());
        }
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

