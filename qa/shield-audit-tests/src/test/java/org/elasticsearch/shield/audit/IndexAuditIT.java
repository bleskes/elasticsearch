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

package org.elasticsearch.shield.audit;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.shield.ShieldPlugin;
import org.elasticsearch.shield.authc.support.SecuredString;
import org.elasticsearch.shield.authc.support.UsernamePasswordToken;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.test.rest.client.http.HttpResponse;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

public class IndexAuditIT extends ESIntegTestCase {
    private static final String USER = "test_user";
    private static final String PASS = "changeme";

    public void testShieldIndexAuditTrailWorking() throws Exception {
        HttpResponse response = httpClient().path("/_cluster/health")
                .addHeader("Authorization", UsernamePasswordToken.basicAuthHeaderValue(USER, new SecuredString(PASS.toCharArray())))
                .execute();
        assertThat(response.getStatusCode(), is(200));

        boolean found = awaitBusy(() -> {
            if (client().admin().cluster().prepareState().get().getState().getMetaData().getIndices().size() < 1) {
                return false;
            }
            client().admin().indices().prepareRefresh().get();
            return client().prepareSearch(".shield_audit_log*").setQuery(QueryBuilders.matchQuery("principal", USER)).get().getHits().totalHits() > 0;
        }, 5L, TimeUnit.SECONDS);

        assertThat(found, is(true));

        SearchResponse searchResponse = client().prepareSearch(".shield_audit_log*").setQuery(QueryBuilders.matchQuery("principal", USER)).get();
        assertThat(searchResponse.getHits().getHits().length, greaterThan(0));
        assertThat((String) searchResponse.getHits().getAt(0).sourceAsMap().get("principal"), is(USER));
    }

    @Override
    protected Settings externalClusterClientSettings() {
        return Settings.builder()
                .put("shield.user", USER + ":" + PASS)
                .build();
    }

    @Override
    protected Collection<Class<? extends Plugin>> transportClientPlugins() {
        return Collections.<Class<? extends Plugin>>singleton(ShieldPlugin.class);
    }
}
