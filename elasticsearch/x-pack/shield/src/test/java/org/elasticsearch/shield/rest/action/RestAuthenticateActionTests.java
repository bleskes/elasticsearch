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

package org.elasticsearch.shield.rest.action;

import org.elasticsearch.common.network.NetworkModule;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.shield.authc.support.SecuredString;
import org.elasticsearch.test.ShieldIntegTestCase;
import org.elasticsearch.test.ShieldSettingsSource;
import org.elasticsearch.test.rest.client.http.HttpResponse;
import org.elasticsearch.test.rest.json.JsonPath;
import org.junit.BeforeClass;

import java.util.List;

import static org.elasticsearch.shield.authc.support.UsernamePasswordToken.basicAuthHeaderValue;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class RestAuthenticateActionTests extends ShieldIntegTestCase {

    private static boolean anonymousEnabled;

    @BeforeClass
    public static void maybeEnableAnonymous() {
        anonymousEnabled = randomBoolean();
    }

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        Settings.Builder builder = Settings.builder()
                .put(super.nodeSettings(nodeOrdinal))
                .put(NetworkModule.HTTP_ENABLED.getKey(), true);

        if (anonymousEnabled) {
            builder.put("shield.authc.anonymous.username", "anon")
                   .putArray("shield.authc.anonymous.roles", ShieldSettingsSource.DEFAULT_ROLE, "foo")
                   .put("shield.authc.anonymous.authz_exception", false);
        }
        return builder.build();
    }

    public void testAuthenticateApi() throws Exception {
        HttpResponse response = httpClient().method("GET").path("/_shield/authenticate")
                .addHeader("Authorization", basicAuthHeaderValue(ShieldSettingsSource.DEFAULT_USER_NAME,
                        new SecuredString(ShieldSettingsSource.DEFAULT_PASSWORD.toCharArray())))
                .execute();

        assertThat(response.getStatusCode(), is(200));
        JsonPath jsonPath = new JsonPath(response.getBody());
        assertThat(jsonPath.evaluate("username").toString(), equalTo(ShieldSettingsSource.DEFAULT_USER_NAME));
        List<String> roles = (List<String>) jsonPath.evaluate("roles");
        assertThat(roles.size(), is(1));
        assertThat(roles, contains(ShieldSettingsSource.DEFAULT_ROLE));
    }

    public void testAuthenticateApiWithoutAuthentication() throws Exception {
        HttpResponse response = httpClient().method("GET").path("/_shield/authenticate")
                .execute();

        if (anonymousEnabled) {
            assertThat(response.getStatusCode(), is(200));
            JsonPath jsonPath = new JsonPath(response.getBody());
            assertThat(jsonPath.evaluate("username").toString(), equalTo("anon"));
            List<String> roles = (List<String>) jsonPath.evaluate("roles");
            assertThat(roles.size(), is(2));
            assertThat(roles, contains(ShieldSettingsSource.DEFAULT_ROLE, "foo"));
        } else {
            assertThat(response.getStatusCode(), is(401));
        }
    }
}
