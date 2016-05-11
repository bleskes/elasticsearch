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

package org.elasticsearch.shield.authc.activedirectory;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.authc.RealmConfig;
import org.elasticsearch.shield.authc.ldap.support.LdapSearchScope;
import org.elasticsearch.shield.authc.support.DnRoleMapper;
import org.elasticsearch.test.junit.annotations.Network;

import java.util.Map;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;

@Network
public class ActiveDirectoryRealmUsageTests extends AbstractActiveDirectoryIntegTests {

    public void testUsageStats() throws Exception {
        String loadBalanceType = randomFrom("failover", "round_robin");
        Settings settings = Settings.builder()
                .put(buildAdSettings(AD_LDAP_URL, AD_DOMAIN, "CN=Bruce Banner, CN=Users,DC=ad,DC=test,DC=elasticsearch,DC=com",
                        LdapSearchScope.BASE, false))
                .put("load_balance.type", loadBalanceType)
                .build();
        RealmConfig config = new RealmConfig("ad-test", settings, globalSettings);
        ActiveDirectorySessionFactory sessionFactory = new ActiveDirectorySessionFactory(config, clientSSLService).init();
        ActiveDirectoryRealm realm = new ActiveDirectoryRealm(config, sessionFactory, mock(DnRoleMapper.class));

        Map<String, Object> stats = realm.usageStats();
        assertThat(stats, is(notNullValue()));
        assertThat(stats, hasEntry("type", "active_directory"));
        assertThat(stats, hasEntry("name", "ad-test"));
        assertThat(stats, hasEntry("order", realm.order()));
        assertThat(stats, hasEntry("size", "small"));
        assertThat(stats, hasEntry("ssl", true));
        assertThat(stats, hasEntry("load_balance_type", loadBalanceType));
    }
}
