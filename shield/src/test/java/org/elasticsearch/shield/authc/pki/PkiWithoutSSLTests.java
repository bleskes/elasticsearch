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

package org.elasticsearch.shield.authc.pki;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.http.HttpServerTransport;
import org.elasticsearch.node.Node;
import org.elasticsearch.shield.authc.support.SecuredString;
import org.elasticsearch.shield.authc.support.UsernamePasswordToken;
import org.elasticsearch.test.ElasticsearchIntegrationTest.ClusterScope;
import org.elasticsearch.test.ShieldIntegrationTest;
import org.elasticsearch.test.ShieldSettingsSource;
import org.elasticsearch.test.rest.client.http.HttpRequestBuilder;
import org.elasticsearch.test.rest.client.http.HttpResponse;
import org.junit.Test;

import static org.hamcrest.Matchers.is;

@ClusterScope(numClientNodes = 0, numDataNodes = 1)
public class PkiWithoutSSLTests extends ShieldIntegrationTest {

    @Override
    public boolean sslTransportEnabled() {
        return false;
    }

    @Override
    public Settings nodeSettings(int nodeOrdinal) {
        return Settings.builder()
                .put(super.nodeSettings(nodeOrdinal))
                .put(Node.HTTP_ENABLED, true)
                .put("shield.authc.realms.pki1.type", "pki")
                .put("shield.authc.realms.pki1.order", "0")
                .build();
    }

    @Test
    public void testThatTransportClientWorks() {
        Client client = internalTestCluster().transportClient();
        assertGreenClusterState(client);
    }

    @Test
    public void testThatHttpWorks() throws Exception {
        HttpServerTransport httpServerTransport = internalTestCluster().getDataNodeInstance(HttpServerTransport.class);
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpRequestBuilder requestBuilder = new HttpRequestBuilder(httpClient)
                    .httpTransport(httpServerTransport)
                    .method("GET")
                    .path("/_nodes");
            requestBuilder.addHeader(UsernamePasswordToken.BASIC_AUTH_HEADER, UsernamePasswordToken.basicAuthHeaderValue(ShieldSettingsSource.DEFAULT_USER_NAME, new SecuredString(ShieldSettingsSource.DEFAULT_PASSWORD.toCharArray())));
            HttpResponse response = requestBuilder.execute();
            assertThat(response.getStatusCode(), is(200));
        }
    }
}
