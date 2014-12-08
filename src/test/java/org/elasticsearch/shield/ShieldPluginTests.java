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

package org.elasticsearch.shield;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.elasticsearch.http.HttpServerTransport;
import org.elasticsearch.shield.authc.support.SecuredString;
import org.elasticsearch.shield.authc.support.UsernamePasswordToken;
import org.elasticsearch.test.ShieldIntegrationTest;
import org.elasticsearch.test.ShieldSettingsSource;
import org.elasticsearch.test.rest.client.http.HttpRequestBuilder;
import org.elasticsearch.test.rest.client.http.HttpResponse;
import org.junit.Test;

import java.io.IOException;

import static org.elasticsearch.rest.RestStatus.OK;
import static org.elasticsearch.rest.RestStatus.UNAUTHORIZED;
import static org.elasticsearch.shield.authc.support.UsernamePasswordToken.basicAuthHeaderValue;
import static org.hamcrest.Matchers.*;

public class ShieldPluginTests extends ShieldIntegrationTest {

    @Test
    public void testThatPluginIsLoaded() throws IOException {
        HttpServerTransport httpServerTransport = internalCluster().getDataNodeInstance(HttpServerTransport.class);
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            logger.info("Executing unauthorized request to /_shield infos");
            HttpResponse response = new HttpRequestBuilder(httpClient).httpTransport(httpServerTransport).method("GET").path("/_shield").execute();
            assertThat(response.getStatusCode(), is(UNAUTHORIZED.getStatus()));

            logger.info("Executing authorized request to /_shield infos");
            response = new HttpRequestBuilder(httpClient).httpTransport(httpServerTransport).method("GET").path("/_shield").addHeader(UsernamePasswordToken.BASIC_AUTH_HEADER,
                    basicAuthHeaderValue(ShieldSettingsSource.DEFAULT_USER_NAME, new SecuredString(ShieldSettingsSource.DEFAULT_PASSWORD.toCharArray()))).execute();
            assertThat(response.getStatusCode(), is(OK.getStatus()));
            assertThat(response.getBody(), allOf(containsString("build_hash"), containsString("build_timestamp"), containsString("build_version")));
        }
    }
}
