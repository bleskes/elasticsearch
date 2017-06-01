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

package org.elasticsearch.xpack.security.authc;

import org.elasticsearch.Version;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.XPackSettings;
import org.elasticsearch.xpack.security.authc.Authentication.RealmRef;
import org.elasticsearch.xpack.security.crypto.CryptoService;
import org.elasticsearch.xpack.security.user.User;

import java.io.IOException;

import static org.elasticsearch.test.VersionUtils.getPreviousVersion;
import static org.elasticsearch.test.VersionUtils.randomVersion;
import static org.elasticsearch.test.VersionUtils.randomVersionBetween;

public class AuthenticationTests extends ESTestCase {
    private static final Settings SSL_ENABLED = Settings.builder()
            .put(XPackSettings.TRANSPORT_SSL_ENABLED.getKey(), true)
            .build();
    private static final Settings SSL_DISABLED = Settings.builder()
            .put(XPackSettings.TRANSPORT_SSL_ENABLED.getKey(), false)
            .build();

    public void testShouldSignWithSigningDisabled() {
        assertFalse(Authentication.shouldSign(randomFrom(SSL_ENABLED, SSL_DISABLED), randomVersion(random()), false));
    }

    /**
     * 5.4.0 is wrong and only wants to sign if SSL is disabled *and* it is
     * talking to a version before 5.4.0. So when it talks to 5.4.1 it will
     * expect headers to be unsigned no matter what.
     */
    public void testShouldSign5_4_0Compat() {
        assertFalse(Authentication.shouldSign(randomFrom(SSL_ENABLED, SSL_DISABLED), Version.V_5_4_0, randomBoolean()));
    }

    /**
     * Versions before 5.4.0 always signed the headers so we should assume that all headers are signed.
     */
    public void testShouldSignPreUnsignedOverSsl() {
        Version version = randomVersionBetween(random(),
                Version.CURRENT.minimumCompatibilityVersion(), getPreviousVersion(Version.V_5_4_0));
        assertTrue(Authentication.shouldSign(randomFrom(SSL_ENABLED, SSL_DISABLED), version, true));
    }

    /**
     * Versions after 5.4.1 should only sign headers if they are not over ssl.
     */
    public void testShouldSignPostUnsignedOverSsl() {
        Version version = randomVersionBetween(random(), Version.V_5_4_1, Version.CURRENT);
        assertTrue(Authentication.shouldSign(SSL_DISABLED, version, true));
        assertFalse(Authentication.shouldSign(SSL_ENABLED, version, true));
    }

    public void testDeserializeHeaderNormal() throws IOException {
        Version version = randomValueOtherThan(Version.V_5_4_0, () -> randomVersion(random()));

        Authentication auth = new Authentication(new User("me"), new RealmRef("realm", "type", "node"), null, version);
        deserializeHeaderTestCase(auth, version, randomBoolean());

        Version differentVersion = randomValueOtherThan(version, () -> randomVersion(random()));
        Authentication brokenAuth =  new Authentication(new User("me"), new RealmRef("realm", "type", "node"), null, differentVersion);
        /* Never sign or we might get an IllegalArgumentException because the signed text looks wrong. It'll look wrong because we're
         * intentionally causing a version mismatch and that mismatch might cause us to unsign using an old style that won't match what
         * is expected. */
        IllegalStateException e = expectThrows(IllegalStateException.class, () ->
                deserializeHeaderTestCase(brokenAuth, version, false));
        assertEquals("version mismatch. expected [" + version + "] but got [" + differentVersion + "]", e.getMessage());
    }

    /**
     * 5.4.0 is "funny" and instead of sticking its own version in the
     * authentication header it sticks the version of the remote node.
     * We have to special case it.
     */
    public void testDeserializeHeader5_4_0Compat() throws IOException {
        Authentication auth = new Authentication(new User("me"), new RealmRef("realm", "type", "node"), null, Version.CURRENT);
        deserializeHeaderTestCase(auth, Version.V_5_4_0, randomBoolean());
    }


    private void deserializeHeaderTestCase(Authentication auth, Version remoteVersion, boolean sign) throws IOException {
        Environment emptyEnvironment = new Environment(Settings.builder()
                .put(Environment.PATH_HOME_SETTING.getKey(), createTempDir().toAbsolutePath())
                .build());
        CryptoService crypto = new CryptoService(Settings.EMPTY, emptyEnvironment);

        String header = auth.encode();
        if (sign) {
            header = crypto.sign(header, auth.getVersion());
        }

        Authentication deserialized = Authentication.deserializeHeader(header, crypto, sign, remoteVersion);
        assertEquals(auth.getUser(), deserialized.getUser());
        assertEquals(auth.getAuthenticatedBy(), deserialized.getAuthenticatedBy());
        assertEquals(auth.getLookedUpBy(), deserialized.getLookedUpBy());
        assertEquals(remoteVersion, deserialized.getVersion());
    }
}
