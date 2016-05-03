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

package org.elasticsearch.shield.authc.ldap;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.shield.authc.activedirectory.ActiveDirectorySessionFactoryTests;
import org.elasticsearch.shield.support.NoOpLogger;
import org.elasticsearch.test.junit.annotations.Network;

import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;

@Network
public class UserAttributeGroupsResolverTests extends GroupsResolverTestCase {

    public static final String BRUCE_BANNER_DN = "cn=Bruce Banner,CN=Users,DC=ad,DC=test,DC=elasticsearch,DC=com";

    public void testResolve() throws Exception {
        //falling back on the 'memberOf' attribute
        UserAttributeGroupsResolver resolver = new UserAttributeGroupsResolver(Settings.EMPTY);
        List<String> groups = resolver.resolve(ldapConnection, BRUCE_BANNER_DN, TimeValue.timeValueSeconds(20), NoOpLogger.INSTANCE);
        assertThat(groups, containsInAnyOrder(
                containsString("Avengers"),
                containsString("SHIELD"),
                containsString("Geniuses"),
                containsString("Philanthropists")));
    }

    public void testResolveCustomGroupAttribute() throws Exception {
        Settings settings = Settings.builder()
                .put("user_group_attribute", "seeAlso")
                .build();
        UserAttributeGroupsResolver resolver = new UserAttributeGroupsResolver(settings);
        List<String> groups = resolver.resolve(ldapConnection, BRUCE_BANNER_DN, TimeValue.timeValueSeconds(20), NoOpLogger.INSTANCE);
        assertThat(groups, hasItems(containsString("Avengers")));  //seeAlso only has Avengers
    }

    public void testResolveInvalidGroupAttribute() throws Exception {
        Settings settings = Settings.builder()
                .put("user_group_attribute", "doesntExist")
                .build();
        UserAttributeGroupsResolver resolver = new UserAttributeGroupsResolver(settings);
        List<String> groups = resolver.resolve(ldapConnection, BRUCE_BANNER_DN, TimeValue.timeValueSeconds(20), NoOpLogger.INSTANCE);
        assertThat(groups, empty());
    }

    @Override
    protected String ldapUrl() {
        return ActiveDirectorySessionFactoryTests.AD_LDAP_URL;
    }

    @Override
    protected String bindDN() {
        return BRUCE_BANNER_DN;
    }

    @Override
    protected String bindPassword() {
        return ActiveDirectorySessionFactoryTests.PASSWORD;
    }
}