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

package org.elasticsearch.xpack.security;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.security.authc.pki.PkiRealm;
import org.elasticsearch.xpack.ssl.SSLService;

import java.nio.file.Path;

public class PkiRealmBootstrapCheckTests extends ESTestCase {

    public void testPkiRealmBootstrapDefault() throws Exception {
        Settings settings = getDefaultSettings();
        assertFalse(new PkiRealmBootstrapCheck(Settings.EMPTY, new SSLService(settings, new Environment(settings))).check());
    }

    public void testBootstrapCheckWithPkiRealm() throws Exception {
        Settings settings = Settings.builder()
                .put("xpack.security.authc.realms.test_pki.type", PkiRealm.TYPE)
                .put(getDefaultSettings())
                .build();
        Environment env = new Environment(settings);
        assertFalse(new PkiRealmBootstrapCheck(settings, new SSLService(settings, env)).check());

        // disable client auth default
        settings = Settings.builder().put(settings)
                .put("xpack.ssl.client_authentication", "none")
                .build();
        env = new Environment(settings);
        assertTrue(new PkiRealmBootstrapCheck(settings, new SSLService(settings, env)).check());

        // enable ssl for http
        settings = Settings.builder().put(settings)
                .put("xpack.security.http.ssl.enabled", true)
                .build();
        env = new Environment(settings);
        assertTrue(new PkiRealmBootstrapCheck(settings, new SSLService(settings, env)).check());

        // enable client auth for http
        settings = Settings.builder().put(settings)
                .put("xpack.security.http.ssl.client_authentication", randomFrom("required", "optional"))
                .build();
        env = new Environment(settings);
        assertFalse(new PkiRealmBootstrapCheck(settings, new SSLService(settings, env)).check());

        // disable http ssl
        settings = Settings.builder().put(settings)
                .put("xpack.security.http.ssl.enabled", false)
                .build();
        env = new Environment(settings);
        assertTrue(new PkiRealmBootstrapCheck(settings, new SSLService(settings, env)).check());

        // set transport client auth
        settings = Settings.builder().put(settings)
                .put("xpack.security.transport.client_authentication", randomFrom("required", "optional"))
                .build();
        env = new Environment(settings);
        assertTrue(new PkiRealmBootstrapCheck(settings, new SSLService(settings, env)).check());

        // test with transport profile
        settings = Settings.builder().put(settings)
                .put("xpack.security.transport.client_authentication", "none")
                .put("transport.profiles.foo.xpack.security.ssl.enabled", true)
                .put("transport.profiles.foo.xpack.security.ssl.client_authentication", randomFrom("required", "optional"))
                .build();
        env = new Environment(settings);
        assertFalse(new PkiRealmBootstrapCheck(settings, new SSLService(settings, env)).check());
    }

    public void testBootstrapCheckWithDisabledRealm() throws Exception {
        Settings settings = Settings.builder()
                .put("xpack.security.authc.realms.test_pki.type", PkiRealm.TYPE)
                .put("xpack.security.authc.realms.test_pki.enabled", false)
                .put("xpack.ssl.client_authentication", "none")
                .put(getDefaultSettings())
                .build();
        Environment env = new Environment(settings);
        assertFalse(new PkiRealmBootstrapCheck(settings, new SSLService(settings, env)).check());
    }

    public void testBootstrapCheckNoSSL() throws Exception {
        Settings settings = Settings.builder()
                .put(getDefaultSettings())
                .put("xpack.security.transport.ssl.enabled", false)
                .build();
        Environment env = new Environment(settings);
        assertFalse(new PkiRealmBootstrapCheck(settings, new SSLService(settings, env)).check());
    }

    private Settings getDefaultSettings() {
        Path testnodeStore = getDataPath("/org/elasticsearch/xpack/security/transport/ssl/certs/simple/testnode.jks");
        return Settings.builder()
                .put("path.home", createTempDir())
                .put("xpack.ssl.keystore.path", testnodeStore)
                .put("xpack.ssl.keystore.password", "testnode")
                .put("xpack.security.transport.ssl.enabled", true)
                .build();
    }
}
