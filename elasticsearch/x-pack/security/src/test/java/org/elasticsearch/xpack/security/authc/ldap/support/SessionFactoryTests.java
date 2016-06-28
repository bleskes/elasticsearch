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

package org.elasticsearch.xpack.security.authc.ldap.support;

import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.util.ssl.HostNameSSLSocketVerifier;
import com.unboundid.util.ssl.TrustAllSSLSocketVerifier;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xpack.security.authc.RealmConfig;
import org.elasticsearch.xpack.security.authc.support.SecuredString;
import org.elasticsearch.test.ESTestCase;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

public class SessionFactoryTests extends ESTestCase {
    public void testConnectionFactoryReturnsCorrectLDAPConnectionOptionsWithDefaultSettings() {
        LDAPConnectionOptions options = SessionFactory.connectionOptions(Settings.EMPTY);
        assertThat(options.followReferrals(), is(equalTo(true)));
        assertThat(options.allowConcurrentSocketFactoryUse(), is(equalTo(true)));
        assertThat(options.getConnectTimeoutMillis(), is(equalTo(5000)));
        assertThat(options.getResponseTimeoutMillis(), is(equalTo(5000L)));
        assertThat(options.getSSLSocketVerifier(), is(instanceOf(HostNameSSLSocketVerifier.class)));
    }

    public void testConnectionFactoryReturnsCorrectLDAPConnectionOptions() {
        Settings settings = Settings.builder()
                .put(SessionFactory.TIMEOUT_TCP_CONNECTION_SETTING, "10ms")
                .put(SessionFactory.HOSTNAME_VERIFICATION_SETTING, "false")
                .put(SessionFactory.TIMEOUT_TCP_READ_SETTING, "20ms")
                .put(SessionFactory.FOLLOW_REFERRALS_SETTING, "false")
                .build();
        LDAPConnectionOptions options = SessionFactory.connectionOptions(settings);
        assertThat(options.followReferrals(), is(equalTo(false)));
        assertThat(options.allowConcurrentSocketFactoryUse(), is(equalTo(true)));
        assertThat(options.getConnectTimeoutMillis(), is(equalTo(10)));
        assertThat(options.getResponseTimeoutMillis(), is(equalTo(20L)));
        assertThat(options.getSSLSocketVerifier(), is(instanceOf(TrustAllSSLSocketVerifier.class)));
    }

    public void testSessionFactoryDoesNotSupportUnauthenticated() {
        assertThat(createSessionFactory().supportsUnauthenticatedSession(), is(false));
    }

    public void testUnauthenticatedSessionThrowsUnsupportedOperationException() throws Exception {
        try {
            createSessionFactory().unauthenticatedSession(randomAsciiOfLength(5));
            fail("session factory should throw an unsupported operation exception");
        } catch (UnsupportedOperationException e) {
            // expected...
        }
    }

    private SessionFactory createSessionFactory() {
        Settings global = Settings.builder().put("path.home", createTempDir()).build();
        return new SessionFactory(new RealmConfig("_name", Settings.builder().put("url", "ldap://localhost:389").build(), global), null) {

            @Override
            protected LdapSession getSession(String user, SecuredString password) {
                return null;
            }
        }.init();
    }
}
