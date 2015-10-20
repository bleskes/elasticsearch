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
 * This tests the group to role mappings from LDAP sources provided by the super class - available from super.realmConfig.
 * The super class will provide appropriate group mappings via configGroupMappings()
 */
@Network
public class GroupMappingTests extends AbstractAdLdapRealmTestCase {
    public void testAuthcAuthz() throws IOException {
        String avenger = realmConfig.loginWithCommonName ? "Natasha Romanoff" : "blackwidow";
        assertAccessAllowed(avenger, "avengers");
    }

    public void testGroupMapping() throws IOException {
        String asgardian = "odin";
        String shieldPhilanthropist = realmConfig.loginWithCommonName ? "Bruce Banner" : "hulk";
        String shield = realmConfig.loginWithCommonName ? "Phil Coulson" : "phil";
        String shieldAsgardianPhilanthropist = "thor";
        String noGroupUser = "jarvis";

        assertAccessAllowed(asgardian, ASGARDIAN_INDEX);
        assertAccessAllowed(shieldAsgardianPhilanthropist, ASGARDIAN_INDEX);
        assertAccessDenied(shieldPhilanthropist, ASGARDIAN_INDEX);
        assertAccessDenied(shield, ASGARDIAN_INDEX);
        assertAccessDenied(noGroupUser, ASGARDIAN_INDEX);

        assertAccessAllowed(shieldPhilanthropist, PHILANTHROPISTS_INDEX);
        assertAccessAllowed(shieldAsgardianPhilanthropist, PHILANTHROPISTS_INDEX);
        assertAccessDenied(asgardian, PHILANTHROPISTS_INDEX);
        assertAccessDenied(shield, PHILANTHROPISTS_INDEX);
        assertAccessDenied(noGroupUser, PHILANTHROPISTS_INDEX);

        assertAccessAllowed(shield, SHIELD_INDEX);
        assertAccessAllowed(shieldPhilanthropist, SHIELD_INDEX);
        assertAccessAllowed(shieldAsgardianPhilanthropist, SHIELD_INDEX);
        assertAccessDenied(asgardian, SHIELD_INDEX);
        assertAccessDenied(noGroupUser, SHIELD_INDEX);
    }
}
