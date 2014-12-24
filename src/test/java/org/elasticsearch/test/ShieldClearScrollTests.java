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

package org.elasticsearch.test;

import org.elasticsearch.ExceptionsHelper;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.ClearScrollResponse;
import org.elasticsearch.action.search.MultiSearchRequestBuilder;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.shield.authc.support.SecuredString;
import org.elasticsearch.shield.authz.AuthorizationException;
import org.elasticsearch.test.junit.annotations.TestLogging;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.elasticsearch.shield.authc.support.UsernamePasswordToken.basicAuthHeaderValue;
import static org.elasticsearch.test.ElasticsearchIntegrationTest.ClusterScope;
import static org.elasticsearch.test.ElasticsearchIntegrationTest.Scope;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@ClusterScope(scope = Scope.SUITE)
public class ShieldClearScrollTests extends ShieldIntegrationTest {

    private List<String> scrollIds;

    @Override
    protected String configUsers() {
        return super.configUsers() +
            "allowed_user:{plain}change_me\n" +
            "denied_user:{plain}change_me\n" ;
    }

    @Override
    protected String configUsersRoles() {
        return super.configUsersRoles() +
            "allowed_role:allowed_user\n" +
            "denied_role:denied_user\n";
    }

    @Override
    protected String configRoles() {
        return super.configRoles() +
            // note the new line here.. we need to fix this in another PR
            // as this throws another exception in the constructor and then we fuck up
            "\nallowed_role:\n" +
            "  cluster:\n" +
            "    - cluster:admin/indices/scroll/clear_all \n" +
            "denied_role:\n" +
            "  indices:\n" +
            "    '*': ALL\n";
    }

    @Before
    public void indexRandomDocuments() {
        BulkRequestBuilder bulkRequestBuilder = client().prepareBulk().setRefresh(true);
        for (int i = 0; i < randomIntBetween(10, 50); i++) {
            bulkRequestBuilder.add(client().prepareIndex("index", "type", String.valueOf(i)).setSource("{ \"foo\" : \"bar\" }"));
        }
        BulkResponse bulkItemResponses = bulkRequestBuilder.get();
        assertThat(bulkItemResponses.hasFailures(), is(false));

        MultiSearchRequestBuilder multiSearchRequestBuilder = client().prepareMultiSearch();
        int count = randomIntBetween(5, 15);
        for (int i = 0; i < count; i++) {
            multiSearchRequestBuilder.add(client().prepareSearch("index").setTypes("type").setScroll("10m").setSize(1));
        }
        MultiSearchResponse multiSearchResponse = multiSearchRequestBuilder.get();
        scrollIds = getScrollIds(multiSearchResponse);
    }

    @Test
    @TestLogging("shield:TRACE")
    public void testThatClearingAllScrollIdsWorks() throws Exception {
        String shieldUser = "allowed_user:change_me";
        String basicAuth = basicAuthHeaderValue("allowed_user", new SecuredString("change_me".toCharArray()));
        ClearScrollResponse clearScrollResponse = internalCluster().transportClient().prepareClearScroll()
            .putHeader("shield.user", shieldUser)
            .putHeader("Authorization", basicAuth)
            .addScrollId("_all").get();
        assertThat(clearScrollResponse.isSucceeded(), is(true));

        assertThatScrollIdsDoNotExist(scrollIds);
    }

    @Test
    public void testThatClearingAllScrollIdsRequirePermissions() throws Exception {
        String shieldUser = "denied_user:change_me";
        String basicAuth = basicAuthHeaderValue("denied_user", new SecuredString("change_me".toCharArray()));
        try {
            internalCluster().transportClient().prepareClearScroll()
                .putHeader("shield.user", shieldUser)
                .putHeader("Authorization", basicAuth)
                .addScrollId("_all").get();
            fail("Expected AuthorizationException but did not happen");
        } catch (AuthorizationException exc) {
            // yay, we weren't allowed!
        }

        // deletion of scroll ids should work
        ClearScrollResponse clearByIdScrollResponse = client().prepareClearScroll().setScrollIds(scrollIds).get();
        assertThat(clearByIdScrollResponse.isSucceeded(), is(true));

        // test with each id, that they do not exist
        assertThatScrollIdsDoNotExist(scrollIds);
    }

    private void assertThatScrollIdsDoNotExist(List<String> scrollIds) {
        for (String scrollId : scrollIds) {
            try {
                client().prepareSearchScroll(scrollId).get();
                fail("Expected SearchPhaseExecutionException but did not happen");
            } catch (SearchPhaseExecutionException expectedException) {
                assertThat(ExceptionsHelper.detailedMessage(expectedException), containsString("SearchContextMissingException"));
            }
        }
    }

    private List<String> getScrollIds(MultiSearchResponse multiSearchResponse) {
        List<String> ids = new ArrayList<>();
        for (MultiSearchResponse.Item item : multiSearchResponse) {
            ids.add(item.getResponse().getScrollId());
        }
        return ids;
    }
}
