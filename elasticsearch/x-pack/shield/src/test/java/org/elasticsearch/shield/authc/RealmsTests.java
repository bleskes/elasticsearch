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

package org.elasticsearch.shield.authc;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.env.Environment;
import org.elasticsearch.shield.User;
import org.elasticsearch.shield.authc.esnative.NativeRealm;
import org.elasticsearch.shield.authc.file.FileRealm;
import org.elasticsearch.shield.authc.ldap.LdapRealm;
import org.elasticsearch.shield.license.ShieldLicenseState;
import org.elasticsearch.test.ESTestCase;
import org.junit.Before;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isOneOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class RealmsTests extends ESTestCase {
    private Map<String, Realm.Factory> factories;
    private ShieldLicenseState shieldLicenseState;

    @Before
    public void init() throws Exception {
        factories = new HashMap<>();
        factories.put(FileRealm.TYPE, new DummyRealm.Factory(FileRealm.TYPE, true));
        factories.put(NativeRealm.TYPE, new DummyRealm.Factory(NativeRealm.TYPE, true));
        for (int i = 0; i < randomIntBetween(1, 5); i++) {
            DummyRealm.Factory factory = new DummyRealm.Factory("type_" + i, rarely());
            factories.put("type_" + i, factory);
        }
        shieldLicenseState = mock(ShieldLicenseState.class);
        when(shieldLicenseState.customRealmsEnabled()).thenReturn(true);
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
        Realms realms = new Realms(settings, env, factories, shieldLicenseState);
        realms.start();
        int i = 0;
        for (Realm realm : realms) {
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
            new Realms(settings, env, factories, shieldLicenseState).start();
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("multiple [file] realms are configured"));
        }
    }

    public void testWithEmptySettings() throws Exception {
        Realms realms = new Realms(Settings.EMPTY, new Environment(Settings.builder().put("path.home", createTempDir()).build()),
                factories, shieldLicenseState);
        realms.start();
        Iterator<Realm> iter = realms.iterator();
        assertThat(iter.hasNext(), is(true));
        Realm realm = iter.next();
        assertThat(realm, notNullValue());
        assertThat(realm.type(), equalTo(NativeRealm.TYPE));
        assertThat(realm.name(), equalTo("default_" + NativeRealm.TYPE));
        assertThat(iter.hasNext(), is(true));
        realm = iter.next();
        assertThat(realm.type(), equalTo(FileRealm.TYPE));
        assertThat(realm.name(), equalTo("default_" + FileRealm.TYPE));
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
        Realms realms = new Realms(settings, env, factories, shieldLicenseState);
        realms.start();
        int i = 0;
        // this is the iterator when licensed
        for (Realm realm : realms) {
            assertThat(realm.order(), equalTo(i));
            int index = orderToIndex.get(i);
            assertThat(realm.type(), equalTo("type_" + index));
            assertThat(realm.name(), equalTo("realm_" + index));
            i++;
        }

        i = 0;
        when(shieldLicenseState.customRealmsEnabled()).thenReturn(false);
        for (Realm realm : realms) {
            assertThat(realm.type, isOneOf(FileRealm.TYPE, NativeRealm.TYPE));
            i++;
        }
        assertThat(i, is(2));
    }

    public void testUnlicensedWithInternalRealms() throws Exception {
        factories.put(LdapRealm.TYPE, new DummyRealm.Factory(LdapRealm.TYPE, false));
        assertThat(factories.get("type_0"), notNullValue());
        Settings.Builder builder = Settings.builder()
                .put("path.home", createTempDir())
                .put("xpack.security.authc.realms.foo.type", "ldap")
                .put("xpack.security.authc.realms.foo.order", "0")
                .put("xpack.security.authc.realms.custom.type", "type_0")
                .put("xpack.security.authc.realms.custom.order", "1");
        Settings settings = builder.build();
        Environment env = new Environment(settings);
        Realms realms = new Realms(settings, env, factories, shieldLicenseState);
        realms.start();
        int i = 0;
        // this is the iterator when licensed
        List<String> types = new ArrayList<>();
        for (Realm realm : realms) {
            i++;
            types.add(realm.type());
        }
        assertThat(types, contains("ldap", "type_0"));

        i = 0;
        when(shieldLicenseState.customRealmsEnabled()).thenReturn(false);
        for (Realm realm : realms) {
            assertThat(realm.type, is("ldap"));
            i++;
        }
        assertThat(i, is(1));
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
        Realms realms = new Realms(settings, env, factories, shieldLicenseState);
        realms.start();
        Iterator<Realm> iterator = realms.iterator();

        int count = 0;
        while (iterator.hasNext()) {
            Realm realm = iterator.next();
            Integer index = orderToIndex.get(realm.order());
            if (index == null) {
                // Default realms are inserted when factories size is 1 and enabled is false
                assertThat(realm.type(), equalTo(NativeRealm.TYPE));
                assertThat(realm.name(), equalTo("default_" + NativeRealm.TYPE));
                assertThat(iterator.hasNext(), is(true));
                realm = iterator.next();
                assertThat(realm.type(), equalTo(FileRealm.TYPE));
                assertThat(realm.name(), equalTo("default_" + FileRealm.TYPE));
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

    static class DummyRealm extends Realm {

        public DummyRealm(String type, RealmConfig config) {
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

        static class Factory extends Realm.Factory<DummyRealm> {

            public Factory(String type, boolean internal) {
                super(type, internal);
            }

            @Override
            public DummyRealm create(RealmConfig config) {
                return new DummyRealm(type(), config);
            }

            @Override
            public DummyRealm createDefault(String name) {
                if (type().equals(NativeRealm.TYPE) || type().equals(FileRealm.TYPE)) {
                    return new DummyRealm(type(), new RealmConfig(name, Settings.EMPTY,
                            Settings.builder().put("path.home", createTempDir()).build()));
                }
                return null;
            }
        }
    }
}
