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

package org.elasticsearch.shield.authc;

import com.google.common.base.Charsets;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.elasticsearch.common.io.Streams;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.http.HttpServerTransport;
import org.elasticsearch.node.Node;
import org.elasticsearch.test.ShieldIntegrationTest;
import org.junit.Test;

import java.io.InputStreamReader;
import java.util.Locale;

import static org.hamcrest.Matchers.*;

public class AnonymousUserTests extends ShieldIntegrationTest {

    private boolean authorizationExceptionsEnabled = randomBoolean();

    @Override
    public Settings nodeSettings(int nodeOrdinal) {
        return Settings.builder()
                .put(super.nodeSettings(nodeOrdinal))
                .put(Node.HTTP_ENABLED, true)
                .put("shield.authc.anonymous.roles", "anonymous")
                .put(AnonymousService.SETTING_AUTHORIZATION_EXCEPTION_ENABLED, authorizationExceptionsEnabled)
                .build();
    }

    @Override
    public boolean sslTransportEnabled() {
        return false;
    }

    @Override
    public String configRoles() {
        return super.configRoles() + "\n" +
                "anonymous:\n" +
                "  indices:\n" +
                "    '*': READ";
    }

    @Test
    public void testAnonymousViaHttp() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(new HttpGet(getNodeUrl() + "_nodes"))) {
            int statusCode = response.getStatusLine().getStatusCode();
            String data = Streams.copyToString(new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8));
            if (authorizationExceptionsEnabled) {
                assertThat(statusCode, is(403));
                assertThat(response.getFirstHeader("WWW-Authenticate"), nullValue());
                assertThat(data, containsString("security_exception"));
            } else {
                assertThat(statusCode, is(401));
                assertThat(response.getFirstHeader("WWW-Authenticate"), notNullValue());
                assertThat(response.getFirstHeader("WWW-Authenticate").getValue(), containsString("Basic"));
                assertThat(data, containsString("security_exception"));
            }
        }
    }

    private String getNodeUrl() {
        TransportAddress transportAddress = internalTestCluster().getInstance(HttpServerTransport.class).boundAddress().boundAddress();
        assertThat(transportAddress, is(instanceOf(InetSocketTransportAddress.class)));
        InetSocketTransportAddress inetSocketTransportAddress = (InetSocketTransportAddress) transportAddress;
        return String.format(Locale.ROOT, "http://%s:%s/", "localhost", inetSocketTransportAddress.address().getPort());
    }
}
