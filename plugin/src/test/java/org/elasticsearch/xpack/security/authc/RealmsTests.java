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

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.env.Environment;
import org.elasticsearch.xpack.security.user.User;
import org.elasticsearch.xpack.security.authc.esnative.ReservedRealm;
import org.elasticsearch.xpack.security.authc.esnative.NativeRealm;
import org.elasticsearch.xpack.security.authc.file.FileRealm;
import org.elasticsearch.xpack.security.authc.ldap.LdapRealm;
import org.elasticsearch.license.XPackLicenseState;
import org.elasticsearch.license.XPackLicenseState.AllowedRealmType;
import org.elasticsearch.test.ESTestCase;
import org.junit.Before;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RealmsTests extends ESTestCase {
    private Map<String, Realm.Factory> factories;
    private XPackLicenseState licenseState;
    private ThreadContext threadContext;
    private ReservedRealm reservedRealm;

    @Before
    public void init() throws Exception {
        factories = new HashMap<>();
        factories.put(FileRealm.TYPE, config -> new DummyRealm(FileRealm.TYPE, config));
        factories.put(NativeRealm.TYPE, config -> new DummyRealm(NativeRealm.TYPE, config));
        for (int i = 0; i < randomIntBetween(1, 5); i++) {
            String name = "type_" + i;
            factories.put(name, config -> new DummyRealm(name, config));
        }
        licenseState = mock(XPackLicenseState.class);
        threadContext = new ThreadContext(Settings.EMPTY);
        reservedRealm = mock(ReservedRealm.class);
        when(licenseState.isAuthAllowed()).thenReturn(true);
        when(licenseState.allowedRealmType()).thenReturn(AllowedRealmType.ALL);
        when(reservedRealm.type()).thenReturn(ReservedRealm.TYPE);
    }

    public void testWithSettings() throws Exception {
        Settings.Builder builder = Settings.builder()
                .put("path.home", createTempDir());
        List<Integer> orders = new ArrayList<>(factories.size() - 2);
        for (int i = 0; i < factories.size() - 2; i++) {
            orders.add(i);
        }
        Collections.shuffle(orders, random());
        Map<Integer, Integer> orderToIndex = new HashMap<>();
        for (int i = 0; i < factories.size() - 2; i++) {
            builder.put("xpack.security.authc.realms.realm_" + i + ".type", "type_" + i);
            builder.put("xpack.security.authc.realms.realm_" + i + ".order", orders.get(i));
            orderToIndex.put(orders.get(i), i);
        }
        Settings settings = builder.build();
        Environment env = new Environment(settings);
        Realms realms = new Realms(settings, env, factories, licenseState, threadContext, reservedRealm);

        Iterator<Realm> iterator = realms.iterator();
        assertThat(iterator.hasNext(), is(true));
        Realm realm = iterator.next();
        assertThat(realm, is(reservedRealm));

        int i = 0;
        while (iterator.hasNext()) {
            realm = iterator.next();
            assertThat(realm.order(), equalTo(i));
            int index = orderToIndex.get(i);
            assertThat(realm.type(), equalTo("type_" + index));
            assertThat(realm.name(), equalTo("realm_" + index));
            i++;
        }
    }

    public void testWithSettingsWithMultipleInternalRealmsOfSameType() throws Exception {
        Settings settings = Settings.builder()
                .put("xpack.security.authc.realms.realm_1.type", FileRealm.TYPE)
                .put("xpack.security.authc.realms.realm_1.order", 0)
                .put("xpack.security.authc.realms.realm_2.type", FileRealm.TYPE)
                .put("xpack.security.authc.realms.realm_2.order", 1)
                .put("path.home", createTempDir())
                .build();
        Environment env = new Environment(settings);
        try {
            new Realms(settings, env, factories, licenseState, threadContext, reservedRealm);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("multiple [file] realms are configured"));
        }
    }

    public void testWithEmptySettings() throws Exception {
        Realms realms = new Realms(Settings.EMPTY, new Environment(Settings.builder().put("path.home", createTempDir()).build()),
                factories, licenseState, threadContext, reservedRealm);
        Iterator<Realm> iter = realms.iterator();
        assertThat(iter.hasNext(), is(true));
        Realm realm = iter.next();
        assertThat(realm, is(reservedRealm));
        assertThat(iter.hasNext(), is(true));
        realm = iter.next();
        assertThat(realm.type(), equalTo(FileRealm.TYPE));
        assertThat(realm.name(), equalTo("default_" + FileRealm.TYPE));
        assertThat(iter.hasNext(), is(true));
        realm = iter.next();
        assertThat(realm.type(), equalTo(NativeRealm.TYPE));
        assertThat(realm.name(), equalTo("default_" + NativeRealm.TYPE));
        assertThat(iter.hasNext(), is(false));
    }

    public void testUnlicensedWithOnlyCustomRealms() throws Exception {
        Settings.Builder builder = Settings.builder()
                .put("path.home", createTempDir());
        List<Integer> orders = new ArrayList<>(factories.size() - 2);
        for (int i = 0; i < factories.size() - 2; i++) {
            orders.add(i);
        }
        Collections.shuffle(orders, random());
        Map<Integer, Integer> orderToIndex = new HashMap<>();
        for (int i = 0; i < factories.size() - 2; i++) {
            builder.put("xpack.security.authc.realms.realm_" + i + ".type", "type_" + i);
            builder.put("xpack.security.authc.realms.realm_" + i + ".order", orders.get(i));
            orderToIndex.put(orders.get(i), i);
        }
        Settings settings = builder.build();
        Environment env = new Environment(settings);
        Realms realms = new Realms(settings, env, factories, licenseState, threadContext, reservedRealm);

        // this is the iterator when licensed
        Iterator<Realm> iter = realms.iterator();
        assertThat(iter.hasNext(), is(true));
        Realm realm = iter.next();
        assertThat(realm, is(reservedRealm));
        int i = 0;
        while (iter.hasNext()) {
            realm = iter.next();
            assertThat(realm.order(), equalTo(i));
            int index = orderToIndex.get(i);
            assertThat(realm.type(), equalTo("type_" + index));
            assertThat(realm.name(), equalTo("realm_" + index));
            i++;
        }

        when(licenseState.allowedRealmType()).thenReturn(AllowedRealmType.DEFAULT);

        iter = realms.iterator();
        assertThat(iter.hasNext(), is(true));
        realm = iter.next();
        assertThat(realm, is(reservedRealm));
        assertThat(iter.hasNext(), is(true));
        realm = iter.next();
        assertThat(realm.type(), equalTo(FileRealm.TYPE));
        assertThat(realm.name(), equalTo("default_" + FileRealm.TYPE));
        assertThat(iter.hasNext(), is(true));
        realm = iter.next();
        assertThat(realm.type(), equalTo(NativeRealm.TYPE));
        assertThat(realm.name(), equalTo("default_" + NativeRealm.TYPE));
        assertThat(iter.hasNext(), is(false));

        when(licenseState.allowedRealmType()).thenReturn(AllowedRealmType.NATIVE);

        iter = realms.iterator();
        assertThat(iter.hasNext(), is(true));
        realm = iter.next();
        assertThat(realm, is(reservedRealm));
        assertThat(iter.hasNext(), is(true));
        realm = iter.next();
        assertThat(realm.type(), equalTo(FileRealm.TYPE));
        assertThat(realm.name(), equalTo("default_" + FileRealm.TYPE));
        assertThat(iter.hasNext(), is(true));
        realm = iter.next();
        assertThat(realm.type(), equalTo(NativeRealm.TYPE));
        assertThat(realm.name(), equalTo("default_" + NativeRealm.TYPE));
        assertThat(iter.hasNext(), is(false));
    }

    public void testUnlicensedWithInternalRealms() throws Exception {
        factories.put(LdapRealm.LDAP_TYPE, config -> new DummyRealm(LdapRealm.LDAP_TYPE, config));
        assertThat(factories.get("type_0"), notNullValue());
        Settings.Builder builder = Settings.builder()
                .put("path.home", createTempDir())
                .put("xpack.security.authc.realms.foo.type", "ldap")
                .put("xpack.security.authc.realms.foo.order", "0")
                .put("xpack.security.authc.realms.custom.type", "type_0")
                .put("xpack.security.authc.realms.custom.order", "1");
        Settings settings = builder.build();
        Environment env = new Environment(settings);
        Realms realms = new Realms(settings, env, factories, licenseState, threadContext, reservedRealm );
        Iterator<Realm> iter = realms.iterator();
        assertThat(iter.hasNext(), is(true));
        Realm realm = iter.next();
        assertThat(realm, is(reservedRealm));

        int i = 0;
        // this is the iterator when licensed
        List<String> types = new ArrayList<>();
        while (iter.hasNext()) {
            realm = iter.next();
            i++;
            types.add(realm.type());
        }
        assertThat(types, contains("ldap", "type_0"));

        when(licenseState.allowedRealmType()).thenReturn(AllowedRealmType.DEFAULT);
        iter = realms.iterator();
        assertThat(iter.hasNext(), is(true));
        realm = iter.next();
        assertThat(realm, is(reservedRealm));
        i = 0;
        while (iter.hasNext()) {
            realm = iter.next();
            assertThat(realm.type, is("ldap"));
            i++;
        }
        assertThat(i, is(1));

        when(licenseState.allowedRealmType()).thenReturn(AllowedRealmType.NATIVE);
        iter = realms.iterator();
        assertThat(iter.hasNext(), is(true));
        realm = iter.next();
        assertThat(realm, is(reservedRealm));
        assertThat(iter.hasNext(), is(true));
        realm = iter.next();
        assertThat(realm.type(), equalTo(FileRealm.TYPE));
        assertThat(realm.name(), equalTo("default_" + FileRealm.TYPE));
        assertThat(iter.hasNext(), is(true));
        realm = iter.next();
        assertThat(realm.type(), equalTo(NativeRealm.TYPE));
        assertThat(realm.name(), equalTo("default_" + NativeRealm.TYPE));
        assertThat(iter.hasNext(), is(false));
    }

    public void testUnlicensedWithNativeRealms() throws Exception {
        factories.put(LdapRealm.LDAP_TYPE, config -> new DummyRealm(LdapRealm.LDAP_TYPE, config));
        final String type = randomFrom(FileRealm.TYPE, NativeRealm.TYPE);
        Settings.Builder builder = Settings.builder()
                .put("path.home", createTempDir())
                .put("xpack.security.authc.realms.foo.type", "ldap")
                .put("xpack.security.authc.realms.foo.order", "0")
                .put("xpack.security.authc.realms.native.type", type)
                .put("xpack.security.authc.realms.native.order", "1");
        Settings settings = builder.build();
        Environment env = new Environment(settings);
        Realms realms = new Realms(settings, env, factories, licenseState, threadContext, reservedRealm );
        Iterator<Realm> iter = realms.iterator();
        assertThat(iter.hasNext(), is(true));
        Realm realm = iter.next();
        assertThat(realm, is(reservedRealm));
        assertThat(iter.hasNext(), is(true));
        realm = iter.next();
        assertThat(realm.type(), is("ldap"));
        assertThat(iter.hasNext(), is(true));
        realm = iter.next();
        assertThat(realm.type(), is(type));
        assertThat(iter.hasNext(), is(false));

        when(licenseState.allowedRealmType()).thenReturn(AllowedRealmType.NATIVE);
        iter = realms.iterator();
        assertThat(iter.hasNext(), is(true));
        realm = iter.next();
        assertThat(realm, is(reservedRealm));
        assertThat(iter.hasNext(), is(true));
        realm = iter.next();
        assertThat(realm.type(), is(type));
        assertThat(iter.hasNext(), is(false));
    }

    public void testDisabledRealmsAreNotAdded() throws Exception {
        Settings.Builder builder = Settings.builder()
                .put("path.home", createTempDir());
        List<Integer> orders = new ArrayList<>(factories.size() - 2);
        for (int i = 0; i < factories.size() - 2; i++) {
            orders.add(i);
        }
        Collections.shuffle(orders, random());
        Map<Integer, Integer> orderToIndex = new HashMap<>();
        for (int i = 0; i < factories.size() - 2; i++) {
            builder.put("xpack.security.authc.realms.realm_" + i + ".type", "type_" + i);
            builder.put("xpack.security.authc.realms.realm_" + i + ".order", orders.get(i));
            boolean enabled = randomBoolean();
            builder.put("xpack.security.authc.realms.realm_" + i + ".enabled", enabled);
            if (enabled) {
                orderToIndex.put(orders.get(i), i);
                logger.error("put [{}] -> [{}]", orders.get(i), i);
            }
        }
        Settings settings = builder.build();
        Environment env = new Environment(settings);
        Realms realms = new Realms(settings, env, factories, licenseState, threadContext, reservedRealm );
        Iterator<Realm> iterator = realms.iterator();
        Realm realm = iterator.next();
        assertThat(realm, is(reservedRealm));
        assertThat(iterator.hasNext(), is(true));

        int count = 0;
        while (iterator.hasNext()) {
            realm = iterator.next();
            Integer index = orderToIndex.get(realm.order());
            if (index == null) {
                // Default realms are inserted when factories size is 1 and enabled is false
                assertThat(realm.type(), equalTo(FileRealm.TYPE));
                assertThat(realm.name(), equalTo("default_" + FileRealm.TYPE));
                assertThat(iterator.hasNext(), is(true));
                realm = iterator.next();
                assertThat(realm.type(), equalTo(NativeRealm.TYPE));
                assertThat(realm.name(), equalTo("default_" + NativeRealm.TYPE));
                assertThat(iterator.hasNext(), is(false));
            } else {
                assertThat(realm.type(), equalTo("type_" + index));
                assertThat(realm.name(), equalTo("realm_" + index));
                assertThat(settings.getAsBoolean("xpack.security.authc.realms.realm_" + index + ".enabled", true), equalTo(Boolean.TRUE));
                count++;
            }
        }

        assertThat(count, equalTo(orderToIndex.size()));
    }

    public void testAuthcAuthzDisabled() throws Exception {
        Settings settings = Settings.builder()
                .put("path.home", createTempDir())
                .put("xpack.security.authc.realms.realm_1.type", FileRealm.TYPE)
                .put("xpack.security.authc.realms.realm_1.order", 0)
                .build();
        Environment env = new Environment(settings);
        Realms realms = new Realms(settings, env, factories, licenseState, threadContext, reservedRealm );

        assertThat(realms.iterator().hasNext(), is(true));

        when(licenseState.isAuthAllowed()).thenReturn(false);
        assertThat(realms.iterator().hasNext(), is(false));
    }

    public void testUsageStats() throws Exception {
        // test realms with duplicate values
        Settings.Builder builder = Settings.builder()
                .put("path.home", createTempDir())
                .put("xpack.security.authc.realms.foo.type", "type_0")
                .put("xpack.security.authc.realms.foo.order", "0")
                .put("xpack.security.authc.realms.bar.type", "type_0")
                .put("xpack.security.authc.realms.bar.order", "1");
        Settings settings = builder.build();
        Environment env = new Environment(settings);
        Realms realms = new Realms(settings, env, factories, licenseState, threadContext, reservedRealm );

        Map<String, Object> usageStats = realms.usageStats();
        assertThat(usageStats.size(), is(factories.size()));

        // first check type_0
        assertThat(usageStats.get("type_0"), instanceOf(Map.class));
        Map<String, Object> type0Map = (Map<String, Object>) usageStats.get("type_0");
        assertThat(type0Map, hasEntry("enabled", true));
        assertThat(type0Map, hasEntry("available", true));
        assertThat((Iterable<? extends String>) type0Map.get("name"), contains("foo", "bar"));
        assertThat((Iterable<? extends Integer>) type0Map.get("order"), contains(0, 1));

        for (Entry<String, Object> entry : usageStats.entrySet()) {
            String type = entry.getKey();
            if ("type_0".equals(type)) {
                continue;
            }

            Map<String, Object> typeMap = (Map<String, Object>) entry.getValue();
            assertThat(typeMap, hasEntry("enabled", false));
            assertThat(typeMap, hasEntry("available", true));
            assertThat(typeMap.size(), is(2));
        }

        // disable ALL using license
        when(licenseState.isAuthAllowed()).thenReturn(false);
        when(licenseState.allowedRealmType()).thenReturn(AllowedRealmType.NONE);
        usageStats = realms.usageStats();
        assertThat(usageStats.size(), is(factories.size()));
        for (Entry<String, Object> entry : usageStats.entrySet()) {
            Map<String, Object> typeMap = (Map<String, Object>) entry.getValue();
            assertThat(typeMap, hasEntry("enabled", false));
            assertThat(typeMap, hasEntry("available", false));
            assertThat(typeMap.size(), is(2));
        }

        // check native or internal realms enabled only
        when(licenseState.isAuthAllowed()).thenReturn(true);
        when(licenseState.allowedRealmType()).thenReturn(randomFrom(AllowedRealmType.NATIVE, AllowedRealmType.DEFAULT));
        usageStats = realms.usageStats();
        assertThat(usageStats.size(), is(factories.size()));
        for (Entry<String, Object> entry : usageStats.entrySet()) {
            final String type = entry.getKey();
            Map<String, Object> typeMap = (Map<String, Object>) entry.getValue();
            if (FileRealm.TYPE.equals(type) || NativeRealm.TYPE.equals(type)) {
                assertThat(typeMap, hasEntry("enabled", true));
                assertThat(typeMap, hasEntry("available", true));
                assertThat((Iterable<? extends String>) typeMap.get("name"), contains("default_" + type));
            } else {
                assertThat(typeMap, hasEntry("enabled", false));
                assertThat(typeMap, hasEntry("available", false));
                assertThat(typeMap.size(), is(2));
            }
        }
    }

    static class DummyRealm extends Realm {

        DummyRealm(String type, RealmConfig config) {
            super(type, config);
        }

        @Override
        public boolean supports(AuthenticationToken token) {
            return false;
        }

        @Override
        public AuthenticationToken token(ThreadContext threadContext) {
            return null;
        }

        @Override
        public User authenticate(AuthenticationToken token) {
            return null;
        }

        @Override
        public User lookupUser(String username) {
            return null;
        }

        @Override
        public boolean userLookupSupported() {
            return false;
        }
    }
}
