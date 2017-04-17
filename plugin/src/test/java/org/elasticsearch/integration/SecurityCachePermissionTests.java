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

package org.elasticsearch.integration;

import org.elasticsearch.ElasticsearchSecurityException;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.settings.SecureString;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.TermsLookup;
import org.elasticsearch.test.SecurityIntegTestCase;
import org.elasticsearch.test.SecuritySettingsSource;
import org.junit.Before;

import static java.util.Collections.singletonMap;
import static org.elasticsearch.xpack.security.authc.support.UsernamePasswordToken.basicAuthHeaderValue;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

public class SecurityCachePermissionTests extends SecurityIntegTestCase {

    private final String READ_ONE_IDX_USER = "read_user";
    
    @Override
    public String configUsers() {
        return super.configUsers()
                + READ_ONE_IDX_USER + ":" + SecuritySettingsSource.DEFAULT_PASSWORD_HASHED + "\n";
    }

    @Override
    public String configRoles() {
        return super.configRoles()
                + "\nread_one_idx:\n"
                + "  indices:\n"
                + "    'data':\n"
                + "      - read\n";
    }

    @Override
    public String configUsersRoles() {
        return super.configUsersRoles()
                + "read_one_idx:" + READ_ONE_IDX_USER + "\n";
    }

    @Before
    public void loadData() {
        index("data", "a", "1", "{ \"name\": \"John\", \"token\": \"token1\" }");
        index("tokens", "tokens", "1", "{ \"group\": \"1\", \"tokens\": [\"token1\", \"token2\"] }");
        refresh();
    }

    public void testThatTermsFilterQueryDoesntLeakData() {
        SearchResponse response = client().prepareSearch("data").setTypes("a").setQuery(QueryBuilders.constantScoreQuery(
                QueryBuilders.termsLookupQuery("token", new TermsLookup("tokens", "tokens", "1", "tokens"))))
                .execute().actionGet();
        assertThat(response.isTimedOut(), is(false));
        assertThat(response.getHits().hits().length, is(1));

        // Repeat with unauthorized user!!!!
        try {
            response = client().filterWithHeader(singletonMap("Authorization", basicAuthHeaderValue(READ_ONE_IDX_USER,
                    new SecureString("changeme".toCharArray()))))
                    .prepareSearch("data").setTypes("a").setQuery(QueryBuilders.constantScoreQuery(
                    QueryBuilders.termsLookupQuery("token", new TermsLookup("tokens", "tokens", "1", "tokens"))))
                    .execute().actionGet();
            fail("search phase exception should have been thrown! response was:\n" + response.toString());
        } catch (ElasticsearchSecurityException e) {
            assertThat(e.toString(), containsString("ElasticsearchSecurityException[action"));
            assertThat(e.toString(), containsString("unauthorized"));
        }
    }
}
