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

package org.elasticsearch.integration;

import org.elasticsearch.test.junit.annotations.Network;
import org.junit.Test;

import java.io.IOException;

import static org.elasticsearch.test.ElasticsearchIntegrationTest.ClusterScope;
import static org.elasticsearch.test.ElasticsearchIntegrationTest.Scope.SUITE;

/**
 * This tests the mapping of multiple groups to a role
 */
@Network
@ClusterScope(scope = SUITE)
public class MultiGroupMappingTests extends AbstractAdLdapRealmTests {

    @Override
    protected String configRoles() {
        return super.configRoles() +
                "\n" +
                "MarvelCharacters:\n" +
                "  cluster: NONE\n" +
                "  indices:\n" +
                "    'marvel_comics': ALL\n";
    }

    @Override
    protected String configRoleMappings() {
        return "MarvelCharacters:  \n" +
                "  - \"CN=SHIELD,CN=Users,DC=ad,DC=test,DC=elasticsearch,DC=com\"\n" +
                "  - \"CN=Avengers,CN=Users,DC=ad,DC=test,DC=elasticsearch,DC=com\"\n" +
                "  - \"CN=Gods,CN=Users,DC=ad,DC=test,DC=elasticsearch,DC=com\"\n" +
                "  - \"CN=Philanthropists,CN=Users,DC=ad,DC=test,DC=elasticsearch,DC=com\"\n" +
                "  - \"cn=SHIELD,ou=people,dc=oldap,dc=test,dc=elasticsearch,dc=com\"\n" +
                "  - \"cn=Avengers,ou=people,dc=oldap,dc=test,dc=elasticsearch,dc=com\"\n" +
                "  - \"cn=Gods,ou=people,dc=oldap,dc=test,dc=elasticsearch,dc=com\"\n" +
                "  - \"cn=Philanthropists,ou=people,dc=oldap,dc=test,dc=elasticsearch,dc=com\"";
    }

    @Test
    public void testGroupMapping() throws IOException {
        String asgardian = "odin";
        String shieldPhilanthropist = realmConfig.loginWithCommonName ? "Bruce Banner" : "hulk";
        String shield = realmConfig.loginWithCommonName ? "Phil Coulson" : "phil";
        String shieldAsgardianPhilanthropist = "thor";
        String noGroupUser = "jarvis";

        assertAccessAllowed(asgardian, "marvel_comics");
        assertAccessAllowed(shieldAsgardianPhilanthropist, "marvel_comics");
        assertAccessAllowed(shieldPhilanthropist, "marvel_comics");
        assertAccessAllowed(shield, "marvel_comics");
        assertAccessDenied(noGroupUser, "marvel_comics");
    }
}
