/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2016] Elasticsearch Incorporated. All Rights Reserved.
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

package org.elasticsearch.xpack.security.authc.esnative;

import org.elasticsearch.ElasticsearchSecurityException;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.common.settings.SecureString;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.test.NativeRealmIntegTestCase;
import org.elasticsearch.xpack.security.client.SecurityClient;
import org.elasticsearch.xpack.security.user.KibanaUser;

import static java.util.Collections.singletonMap;
import static org.elasticsearch.xpack.security.authc.support.UsernamePasswordToken.basicAuthHeaderValue;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

/**
 * Integration tests for the built in realm with default passwords disabled
 */
public class ReservedRealmNoDefaultPasswordIntegTests extends NativeRealmIntegTestCase {

    private static final SecureString DEFAULT_PASSWORD = new SecureString("changeme".toCharArray());

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        Settings.Builder builder = Settings.builder()
                .put(super.nodeSettings(nodeOrdinal))
                .put(ReservedRealm.ACCEPT_DEFAULT_PASSWORD_SETTING.getKey(), false);
        return builder.build();
    }

    /**
     * This ensures that if a user is explicitly enabled, thus creating an entry in the security index, but no password is ever set,
     * then the user is treated as having a default password, and cannot login.
     */
    public void testEnablingUserWithoutPasswordCannotLogin() throws Exception {
        final SecurityClient c = securityClient();
        c.prepareSetEnabled(KibanaUser.NAME, true).get();

        ElasticsearchSecurityException elasticsearchSecurityException = expectThrows(ElasticsearchSecurityException.class, () -> client()
                .filterWithHeader(singletonMap("Authorization", basicAuthHeaderValue(KibanaUser.NAME, DEFAULT_PASSWORD)))
                .admin()
                .cluster()
                .prepareHealth()
                .get());
        assertThat(elasticsearchSecurityException.getMessage(), containsString("authenticate"));

        final SecureString newPassword = new SecureString("not-the-default-password".toCharArray());
        c.prepareChangePassword(KibanaUser.NAME, newPassword.clone().getChars()).get();

        ClusterHealthResponse response = client()
                .filterWithHeader(singletonMap("Authorization", basicAuthHeaderValue(KibanaUser.NAME, newPassword)))
                .admin()
                .cluster()
                .prepareHealth()
                .get();

        assertThat(response.getClusterName(), is(cluster().getClusterName()));
    }
}
