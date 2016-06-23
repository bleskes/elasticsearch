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

package org.elasticsearch.xpack.security.authc.pki;

import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Response;
import org.elasticsearch.common.network.NetworkModule;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.test.ESIntegTestCase.ClusterScope;
import org.elasticsearch.test.SecurityIntegTestCase;
import org.elasticsearch.test.SecuritySettingsSource;
import org.elasticsearch.xpack.security.authc.support.SecuredString;
import org.elasticsearch.xpack.security.authc.support.UsernamePasswordToken;

import java.util.Collections;

import static org.hamcrest.Matchers.is;

@ClusterScope(numClientNodes = 0, supportsDedicatedMasters = false, numDataNodes = 1)
public class PkiWithoutSSLTests extends SecurityIntegTestCase {
    @Override
    public boolean sslTransportEnabled() {
        return false;
    }

    @Override
    public Settings nodeSettings(int nodeOrdinal) {
        return Settings.builder()
                .put(super.nodeSettings(nodeOrdinal))
                .put(NetworkModule.HTTP_ENABLED.getKey(), true)
                .put("xpack.security.authc.realms.pki1.type", "pki")
                .put("xpack.security.authc.realms.pki1.order", "0")
                .build();
    }

    public void testThatTransportClientWorks() {
        Client client = internalCluster().transportClient();
        assertGreenClusterState(client);
    }

    public void testThatHttpWorks() throws Exception {
        try (Response response = getRestClient().performRequest("GET", "/_nodes", Collections.emptyMap(), null,
                new BasicHeader(UsernamePasswordToken.BASIC_AUTH_HEADER,
                        UsernamePasswordToken.basicAuthHeaderValue(SecuritySettingsSource.DEFAULT_USER_NAME,
                                new SecuredString(SecuritySettingsSource.DEFAULT_PASSWORD.toCharArray()))))) {
            assertThat(response.getStatusLine().getStatusCode(), is(200));
        }
    }
}
