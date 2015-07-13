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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.shield.ShieldSettingsFilter;
import org.elasticsearch.shield.authc.esusers.ESUsersRealm;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Serves as a realms registry (also responsible for ordering the realms appropriately)
 */
public class Realms extends AbstractLifecycleComponent<Realms> implements Iterable<Realm> {

    private final Environment env;
    private final Map<String, Realm.Factory> factories;
    private final ShieldSettingsFilter settingsFilter;

    private List<Realm> realms = Collections.emptyList();

    @Inject
    public Realms(Settings settings, Environment env, Map<String, Realm.Factory> factories, ShieldSettingsFilter settingsFilter) {
        super(settings);
        this.env = env;
        this.factories = factories;
        this.settingsFilter = settingsFilter;
    }

    @Override
    protected void doStart() throws ElasticsearchException {
        realms = new CopyOnWriteArrayList<>(initRealms());
    }

    @Override
    protected void doStop() throws ElasticsearchException {}

    @Override
    protected void doClose() throws ElasticsearchException {}

    @Override
    public Iterator<Realm> iterator() {
        return realms.iterator();
    }

    public Realm realm(String name) {
        for (Realm realm : realms) {
            if (name.equals(realm.config.name)) {
                return realm;
            }
        }
        return null;
    }

    public Realm.Factory realmFactory(String type) {
        return factories.get(type);
    }

    protected List<Realm> initRealms() {
        Settings realmsSettings = settings.getAsSettings("shield.authc.realms");
        Set<String> internalTypes = Sets.newHashSet();
        List<Realm> realms = Lists.newArrayList();
        for (String name : realmsSettings.names()) {
            Settings realmSettings = realmsSettings.getAsSettings(name);
            String type = realmSettings.get("type");
            if (type == null) {
                throw new IllegalArgumentException("missing realm type for [" + name + "] realm");
            }
            Realm.Factory factory = factories.get(type);
            if (factory == null) {
                throw new IllegalArgumentException("unknown realm type [" + type + "] set for realm [" + name + "]");
            }
            factory.filterOutSensitiveSettings(name, settingsFilter);
            RealmConfig config = new RealmConfig(name, realmSettings, settings, env);
            if (!config.enabled()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("realm [{}/{}] is disabled", type, name);
                }
                continue;
            }
            if (factory.internal()) {
                // this is an internal realm factory, let's make sure we didn't already registered one
                // (there can only be one instance of an internal realm)
                if (internalTypes.contains(type)) {
                    throw new IllegalArgumentException("multiple [" + type + "] realms are configured. [" + type +
                            "] is an internal realm and therefore there can only be one such realm configured");
                }
                internalTypes.add(type);
            }
            realms.add(factory.create(config));
        }

        if (!realms.isEmpty()) {
            Collections.sort(realms);
            return realms;
        }

        // there is no "realms" configuration, go over all the factories and try to create defaults
        // for all the internal realms
        realms.add(factories.get(ESUsersRealm.TYPE).createDefault("default_" + ESUsersRealm.TYPE));
        return realms;
    }

    /**
     * returns the settings for the internal realm of the given type. Typically, internal realms may or may
     * not be configured. If they are not configured, they work OOTB using default settings. If they are
     * configured, there can only be one configured for an internal realm.
     */
    public static Settings internalRealmSettings(Settings settings, String realmType) {
        Settings realmsSettings = settings.getAsSettings("shield.authc.realms");
        Settings result = null;
        for (String name : realmsSettings.names()) {
            Settings realmSettings = realmsSettings.getAsSettings(name);
            String type = realmSettings.get("type");
            if (type == null) {
                throw new IllegalArgumentException("missing realm type for [" + name + "] realm");
            }
            if (type.equals(realmType)) {
                if (result != null) {
                    throw new IllegalArgumentException("multiple [" + realmType + "] realms are configured. only one [" + realmType + "] may be configured");
                }
                result = realmSettings;
            }
        }
        return result != null ? result : Settings.EMPTY;
    }

}
