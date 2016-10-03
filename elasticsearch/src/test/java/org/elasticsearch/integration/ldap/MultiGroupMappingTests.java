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

package org.elasticsearch.integration.ldap;

import org.elasticsearch.test.junit.annotations.Network;

import java.io.IOException;

/**
 * This tests the mapping of multiple groups to a role
 */
@Network
public class MultiGroupMappingTests extends AbstractAdLdapRealmTestCase {
    @Override
    protected String configRoles() {
        return super.configRoles() +
                "\n" +
                "MarvelCharacters:\n" +
                "  cluster: [ NONE ]\n" +
                "  indices:\n" +
                "    - names: 'marvel_comics'\n" +
                "      privileges: [ all ]\n";
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

    public void testGroupMapping() throws IOException {
        String asgardian = "odin";
        String securityPhilanthropist = realmConfig.loginWithCommonName ? "Bruce Banner" : "hulk";
        String security = realmConfig.loginWithCommonName ? "Phil Coulson" : "phil";
        String securityAsgardianPhilanthropist = "thor";
        String noGroupUser = "jarvis";

        assertAccessAllowed(asgardian, "marvel_comics");
        assertAccessAllowed(securityAsgardianPhilanthropist, "marvel_comics");
        assertAccessAllowed(securityPhilanthropist, "marvel_comics");
        assertAccessAllowed(security, "marvel_comics");
        assertAccessDenied(noGroupUser, "marvel_comics");
    }
}
