/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2016] Elasticsearch Incorporated. All Rights Reserved.
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

import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.watcher.ResourceWatcherService;
import org.elasticsearch.xpack.security.authc.esnative.NativeRealm;
import org.elasticsearch.xpack.security.authc.esnative.NativeUsersStore;
import org.elasticsearch.xpack.security.authc.esnative.ReservedRealm;
import org.elasticsearch.xpack.security.authc.file.FileRealm;
import org.elasticsearch.xpack.security.authc.ldap.LdapRealm;
import org.elasticsearch.xpack.security.authc.pki.PkiRealm;
import org.elasticsearch.xpack.security.authc.support.mapper.NativeRoleMappingStore;
import org.elasticsearch.xpack.ssl.SSLService;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Provides a single entry point into dealing with all standard XPack security {@link Realm realms}.
 * This class does not handle extensions.
 * @see Realms for the component that manages configured realms (including custom extension realms)
 */
public class InternalRealms {

    /**
     * The list of all <em>internal</em> realm types, excluding {@link ReservedRealm#TYPE}.
     */
    private static final Set<String> TYPES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            NativeRealm.TYPE, FileRealm.TYPE, LdapRealm.AD_TYPE, LdapRealm.LDAP_TYPE, PkiRealm.TYPE
    )));

    /**
     * Determines whether <code>type</code> is an internal realm-type, optionally considering
     * the {@link ReservedRealm}.
     */
    public static boolean isInternalRealm(String type, boolean includeReservedRealm) {
        if (TYPES.contains(type)) {
            return true;
        }
        if (includeReservedRealm && ReservedRealm.TYPE.equals(type)) {
            return true;
        }
        return false;
    }

    /**
     * Creates {@link Realm.Factory factories} for each <em>internal</em> realm type.
     * This excludes the {@link ReservedRealm}, as it cannot be created dynamically.
     * @return A map from <em>realm-type</em> to <code>Factory</code>
     */
    public static Map<String, Realm.Factory> getFactories(
            ThreadPool threadPool, ResourceWatcherService resourceWatcherService,
            SSLService sslService, NativeUsersStore nativeUsersStore,
            NativeRoleMappingStore nativeRoleMappingStore) {

        Map<String, Realm.Factory> map = new HashMap<>();
        map.put(FileRealm.TYPE, config -> new FileRealm(config, resourceWatcherService));
        map.put(NativeRealm.TYPE, config -> new NativeRealm(config, nativeUsersStore));
        map.put(LdapRealm.AD_TYPE, config -> new LdapRealm(LdapRealm.AD_TYPE, config, sslService,
                resourceWatcherService, nativeRoleMappingStore, threadPool));
        map.put(LdapRealm.LDAP_TYPE, config -> new LdapRealm(LdapRealm.LDAP_TYPE, config,
                sslService, resourceWatcherService, nativeRoleMappingStore, threadPool));
        map.put(PkiRealm.TYPE, config -> new PkiRealm(config, sslService, resourceWatcherService,
                nativeRoleMappingStore));
        return Collections.unmodifiableMap(map);
    }

    /**
     * Provides the {@link Setting setting configuration} for each <em>internal</em> realm type.
     * This excludes the {@link ReservedRealm}, as it cannot be configured dynamically.
     * @return A map from <em>realm-type</em> to a collection of <code>Setting</code> objects.
     */
    public static Map<String,Set<Setting<?>>> getSettings() {
        Map<String, Set<Setting<?>>> map = new HashMap<>();
        map.put(FileRealm.TYPE, FileRealm.getSettings());
        map.put(NativeRealm.TYPE, NativeRealm.getSettings());
        map.put(LdapRealm.AD_TYPE, LdapRealm.getSettings(LdapRealm.AD_TYPE));
        map.put(LdapRealm.LDAP_TYPE, LdapRealm.getSettings(LdapRealm.LDAP_TYPE));
        map.put(PkiRealm.TYPE, PkiRealm.getSettings());
        return Collections.unmodifiableMap(map);
    }

    private InternalRealms() {
    }

}
