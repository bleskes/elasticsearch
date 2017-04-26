/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2017] Elasticsearch Incorporated. All Rights Reserved.
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

package org.elasticsearch.xpack.security.authc;

import org.elasticsearch.ElasticsearchSecurityException;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.SecureString;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.test.SecurityIntegTestCase;
import org.elasticsearch.test.SecuritySettingsSource;
import org.elasticsearch.xpack.security.action.token.CreateTokenResponse;
import org.elasticsearch.xpack.security.action.token.InvalidateTokenResponse;
import org.elasticsearch.xpack.security.client.SecurityClient;
import org.junit.After;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

public class TokenAuthIntegTests extends SecurityIntegTestCase {

    @Override
    public Settings nodeSettings(int nodeOrdinal) {
        return Settings.builder()
                .put(super.nodeSettings(nodeOrdinal))
                // turn down token expiration interval and crank up the deletion interval
                .put(TokenService.TOKEN_EXPIRATION.getKey(), TimeValue.timeValueSeconds(1L))
                .put(TokenService.DELETE_INTERVAL.getKey(), TimeValue.timeValueSeconds(1L))
                .put(TokenService.DELETE_TIMEOUT.getKey(), TimeValue.timeValueSeconds(2L))
                .build();
    }

    public void testExpiredTokensDeletedAfterExpiration() throws Exception {
        final Client client = internalClient();
        SecurityClient securityClient = new SecurityClient(client);
        CreateTokenResponse response = securityClient.prepareCreateToken()
                .setGrantType("password")
                .setUsername(SecuritySettingsSource.DEFAULT_USER_NAME)
                .setPassword(new SecureString(SecuritySettingsSource.DEFAULT_PASSWORD.toCharArray()))
                .get();
        Instant created = Instant.now();

        InvalidateTokenResponse invalidateResponse = securityClient.prepareInvalidateToken(response.getTokenString()).get();
        assertTrue(invalidateResponse.isCreated());
        assertBusy(() -> {
            SearchResponse searchResponse = client.prepareSearch(TokenService.INDEX_NAME)
                    .setSource(SearchSourceBuilder.searchSource().query(QueryBuilders.termQuery("doc_type", TokenService.DOC_TYPE)))
                    .setSize(0)
                    .setTerminateAfter(1)
                    .get();
            assertThat(searchResponse.getHits().getTotalHits(), greaterThan(0L));
        });

        AtomicBoolean deleteTriggered = new AtomicBoolean(false);
        assertBusy(() -> {
            assertTrue(Instant.now().isAfter(created.plusSeconds(1L).plusMillis(500L)));
            if (deleteTriggered.compareAndSet(false, true)) {
                // invalidate a invalid token... doesn't matter that it is bad... we just want this action to trigger the deletion
                try {
                    securityClient.prepareInvalidateToken("fooobar").execute().actionGet();
                } catch (ElasticsearchSecurityException e) {
                    assertEquals("token malformed", e.getMessage());
                }
            }
            client.admin().indices().prepareRefresh(TokenService.INDEX_NAME).get();
            SearchResponse searchResponse = client.prepareSearch(TokenService.INDEX_NAME)
                    .setSource(SearchSourceBuilder.searchSource().query(QueryBuilders.termQuery("doc_type", TokenService.DOC_TYPE)))
                    .setSize(0)
                    .setTerminateAfter(1)
                    .get();
            assertThat(searchResponse.getHits().getTotalHits(), equalTo(0L));
        }, 30, TimeUnit.SECONDS);
    }

    public void testExpireMultipleTimes() {
        CreateTokenResponse response = securityClient().prepareCreateToken()
                .setGrantType("password")
                .setUsername(SecuritySettingsSource.DEFAULT_USER_NAME)
                .setPassword(new SecureString(SecuritySettingsSource.DEFAULT_PASSWORD.toCharArray()))
                .get();
        Instant created = Instant.now();

        InvalidateTokenResponse invalidateResponse = securityClient().prepareInvalidateToken(response.getTokenString()).get();
        // if the token is expired then the API will return false for created so we need to handle that
        final boolean correctResponse = invalidateResponse.isCreated() || created.plusSeconds(1L).isBefore(Instant.now());
        assertTrue(correctResponse);
        assertFalse(securityClient().prepareInvalidateToken(response.getTokenString()).get().isCreated());
    }

    @After
    public void wipeSecurityIndex() {
        try {
            // this is a hack to clean up the .security index since only superusers can delete it and the default test user is not a
            // superuser since the role used there is a file based role since we cannot guarantee the superuser role is always available
            internalClient().admin().indices().prepareDelete(TokenService.INDEX_NAME).get();
        } catch (IndexNotFoundException e) {
            logger.warn("securirty index does not exist", e);
        }
    }
}
