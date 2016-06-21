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

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;

/**
 *
 */
public class RealmConfig {

    final String name;
    final boolean enabled;
    final int order;
    final Settings settings;

    private final Environment env;
    private final Settings globalSettings;

    public RealmConfig(String name, Settings settings, Settings globalSettings) {
        this(name, settings, globalSettings, new Environment(globalSettings));
    }

    public RealmConfig(String name, Settings settings, Settings globalSettings, Environment env) {
        this.name = name;
        this.settings = settings;
        this.globalSettings = globalSettings;
        this.env = env;
        enabled = settings.getAsBoolean("enabled", true);
        order = settings.getAsInt("order", Integer.MAX_VALUE);
    }
    
    public String name() {
        return name;
    }

    public boolean enabled() {
        return enabled;
    }
    
    public int order() {
        return order;
    }

    public Settings settings() {
        return settings;
    }

    public Settings globalSettings() {
        return globalSettings;
    }

    public ESLogger logger(Class clazz) {
        return Loggers.getLogger(clazz, globalSettings);
    }

    public Environment env() {
        return env;
    }
}
