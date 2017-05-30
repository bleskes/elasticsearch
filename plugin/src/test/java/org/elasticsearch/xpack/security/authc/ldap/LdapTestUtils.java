/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2017] Elasticsearch Incorporated. All Rights Reserved.
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

import java.nio.file.Path;

import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.ldap.sdk.LDAPURL;
import org.apache.lucene.util.LuceneTestCase;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.security.authc.ldap.support.LdapUtils;
import org.elasticsearch.xpack.security.authc.ldap.support.SessionFactory;
import org.elasticsearch.xpack.ssl.SSLService;
import org.elasticsearch.xpack.ssl.VerificationMode;

public class LdapTestUtils {

    private LdapTestUtils() {
        // Utility class
    }

    public static LDAPConnection openConnection(String url, String bindDN, String bindPassword, Path keystore) throws Exception {
        boolean useGlobalSSL = ESTestCase.randomBoolean();
        Settings.Builder builder = Settings.builder().put("path.home", LuceneTestCase.createTempDir());
        if (useGlobalSSL) {
            builder.put("xpack.ssl.keystore.path", keystore)
                    .put("xpack.ssl.keystore.password", "changeit");

            // fake realm to load config with certificate verification mode
            builder.put("xpack.security.authc.realms.bar.ssl.keystore.path", keystore);
            builder.put("xpack.security.authc.realms.bar.ssl.keystore.password", "changeit");
            builder.put("xpack.security.authc.realms.bar.ssl.verification_mode", VerificationMode.CERTIFICATE);
        } else {
            // fake realms so ssl will get loaded
            builder.put("xpack.security.authc.realms.foo.ssl.keystore.path", keystore);
            builder.put("xpack.security.authc.realms.foo.ssl.keystore.password", "changeit");
            builder.put("xpack.security.authc.realms.foo.ssl.verification_mode", VerificationMode.FULL);
            builder.put("xpack.security.authc.realms.bar.ssl.keystore.path", keystore);
            builder.put("xpack.security.authc.realms.bar.ssl.keystore.password", "changeit");
            builder.put("xpack.security.authc.realms.bar.ssl.verification_mode", VerificationMode.CERTIFICATE);
        }
        Settings settings = builder.build();
        Environment env = new Environment(settings);
        SSLService sslService = new SSLService(settings, env);

        LDAPURL ldapurl = new LDAPURL(url);
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
        return new LDAPConnection(sslService.sslSocketFactory(connectionSettings), options, ldapurl.getHost(),
                ldapurl.getPort(), bindDN, bindPassword);
    }
}
