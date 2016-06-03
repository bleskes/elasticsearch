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

package org.elasticsearch.shield.user;

import org.elasticsearch.client.ElasticsearchResponse;
import org.elasticsearch.client.ElasticsearchResponseException;
import org.elasticsearch.common.network.NetworkModule;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.authz.InternalAuthorizationService;
import org.elasticsearch.test.ShieldIntegTestCase;

import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class AnonymousUserIntegTests extends ShieldIntegTestCase {
    private boolean authorizationExceptionsEnabled = randomBoolean();

    @Override
    public Settings nodeSettings(int nodeOrdinal) {
        return Settings.builder()
                .put(super.nodeSettings(nodeOrdinal))
                .put(NetworkModule.HTTP_ENABLED.getKey(), true)
                .put(AnonymousUser.ROLES_SETTING.getKey(), "anonymous")
                .put(InternalAuthorizationService.ANONYMOUS_AUTHORIZATION_EXCEPTION_SETTING.getKey(), authorizationExceptionsEnabled)
                .build();
    }

    @Override
    public String configRoles() {
        return super.configRoles() + "\n" +
                "anonymous:\n" +
                "  indices:\n" +
                "    - names: '*'\n" +
                "      privileges: [ READ ]\n";
    }

    public void testAnonymousViaHttp() throws Exception {
        try {
            getRestClient().performRequest("GET", "/_nodes", Collections.emptyMap(), null);
            fail("request should have failed");
        } catch(ElasticsearchResponseException e) {
            int statusCode = e.getElasticsearchResponse().getStatusLine().getStatusCode();
            ElasticsearchResponse response = e.getElasticsearchResponse();
            if (authorizationExceptionsEnabled) {
                assertThat(statusCode, is(403));
                assertThat(response.getFirstHeader("WWW-Authenticate"), nullValue());
                assertThat(e.getResponseBody(), containsString("security_exception"));
            } else {
                assertThat(statusCode, is(401));
                assertThat(response.getFirstHeader("WWW-Authenticate"), notNullValue());
                assertThat(response.getFirstHeader("WWW-Authenticate"), containsString("Basic"));
                assertThat(e.getResponseBody(), containsString("security_exception"));
            }
        }
    }
}
