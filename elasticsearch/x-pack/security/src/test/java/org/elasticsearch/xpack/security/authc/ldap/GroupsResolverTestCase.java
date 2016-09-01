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

package org.elasticsearch.xpack.security.authc.ldap;

import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.ldap.sdk.LDAPURL;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.xpack.security.authc.ldap.support.SessionFactory;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.ssl.SSLService;
import org.junit.After;
import org.junit.Before;

import java.nio.file.Path;

public abstract class GroupsResolverTestCase extends ESTestCase {

    protected LDAPConnection ldapConnection;

    protected abstract String ldapUrl();

    protected abstract String bindDN();

    protected abstract String bindPassword();

    @Before
    public void setUpLdapConnection() throws Exception {
        Path keystore = getDataPath("../ldap/support/ldaptrust.jks");
        boolean useGlobalSSL = randomBoolean();
        Settings.Builder builder = Settings.builder().put("path.home", createTempDir());
        if (useGlobalSSL) {
            builder.put("xpack.ssl.keystore.path", keystore)
                    .put("xpack.ssl.keystore.password", "changeit");
        } else {
            // fake a realm so ssl will get loaded
            builder.put("xpack.security.authc.realms.foo.ssl.keystore.path", keystore);
            builder.put("xpack.security.authc.realms.foo.ssl.keystore.password", "changeit");
        }
        Settings settings = builder.build();
        Environment env = new Environment(settings);
        SSLService sslService = new SSLService(settings, env);

        LDAPURL ldapurl = new LDAPURL(ldapUrl());
        LDAPConnectionOptions options = new LDAPConnectionOptions();
        options.setFollowReferrals(true);
        options.setAllowConcurrentSocketFactoryUse(true);
        options.setConnectTimeoutMillis(Math.toIntExact(SessionFactory.TIMEOUT_DEFAULT.millis()));
        options.setResponseTimeoutMillis(SessionFactory.TIMEOUT_DEFAULT.millis());

        Settings connectionSettings;
        if (useGlobalSSL) {
            connectionSettings = Settings.EMPTY;
        } else {
            connectionSettings = Settings.builder().put("keystore.path", keystore)
                    .put("keystore.password", "changeit").build();
        }

        ldapConnection = new LDAPConnection(sslService.sslSocketFactory(connectionSettings), options, ldapurl.getHost(),
                ldapurl.getPort(), bindDN(), bindPassword());
    }

    @After
    public void tearDownLdapConnection() throws Exception {
        ldapConnection.close();
    }
}
