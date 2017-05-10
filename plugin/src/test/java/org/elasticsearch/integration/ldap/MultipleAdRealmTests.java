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

package org.elasticsearch.integration.ldap;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.test.junit.annotations.Network;
import org.junit.BeforeClass;

/**
 * This tests that configurations that contain two AD realms work correctly.
 * The required behaviour is that users from both realms (directory servers) can be authenticated using
 * just their userid (the AuthenticationService tries them in order)
 */
@Network
public class MultipleAdRealmTests extends AbstractAdLdapRealmTestCase {

    private static RealmConfig secondaryRealmConfig;

    @BeforeClass
    public static void setupSecondaryRealm() {
        // Pick a secondary realm that has the inverse value for 'loginWithCommonName' compare with the primary realm
        final List<RealmConfig> configs = Arrays.stream(RealmConfig.values())
                .filter(config -> config.loginWithCommonName != AbstractAdLdapRealmTestCase.realmConfig.loginWithCommonName)
                .filter(config -> config.name().startsWith("AD"))
                .collect(Collectors.toList());
        secondaryRealmConfig = randomFrom(configs);
        ESLoggerFactory.getLogger("test")
                .info("running test with secondary realm configuration [{}], with direct group to role mapping [{}]. Settings [{}]",
                        secondaryRealmConfig, secondaryRealmConfig.mapGroupsAsRoles, secondaryRealmConfig.settings.getAsMap());

        // It's easier to test 2 realms when using file based role mapping, and for the purposes of
        // this test, there's no need to test native mappings.
        AbstractAdLdapRealmTestCase.roleMappings = realmConfig.selectRoleMappings(() -> true);
    }

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        Settings.Builder builder = Settings.builder();
        builder.put(super.nodeSettings(nodeOrdinal));

        Path store = getDataPath(TESTNODE_KEYSTORE);
        final List<RoleMappingEntry> secondaryRoleMappings = secondaryRealmConfig.selectRoleMappings(() -> true);
        final Settings secondarySettings = super.buildRealmSettings(secondaryRealmConfig, secondaryRoleMappings, store);
        secondarySettings.getAsMap().forEach((name, value) -> {
            name = name.replace(XPACK_SECURITY_AUTHC_REALMS_EXTERNAL, XPACK_SECURITY_AUTHC_REALMS_EXTERNAL + "2");
            builder.put(name, value);
        });

        return builder.build();
    }

    /**
     * Test that both realms support user login. Implementation wise, this means that if the first realm reject the authentication attempt,
     * then the second realm will be tried.
     * Because one realm is using "common name" (cn) for login, and the other uses the "userid" (sAMAccountName) [see
     * {@link #setupSecondaryRealm()}], this is simply a matter of checking that we can authenticate with both identifiers.
     */
    public void testCanAuthenticateAgainstBothRealms() throws IOException {
        assertAccessAllowed("Natasha Romanoff", "avengers");
        assertAccessAllowed("blackwidow", "avengers");
    }

}
