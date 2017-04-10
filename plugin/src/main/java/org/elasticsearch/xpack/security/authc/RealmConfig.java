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

import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.ThreadContext;
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
    private final ThreadContext threadContext;

    public RealmConfig(String name, Settings settings, Settings globalSettings,
                       ThreadContext threadContext) {
        this(name, settings, globalSettings, new Environment(globalSettings), threadContext);
    }

    public RealmConfig(String name, Settings settings, Settings globalSettings, Environment env,
                       ThreadContext threadContext) {
        this.name = name;
        this.settings = settings;
        this.globalSettings = globalSettings;
        this.env = env;
        enabled = RealmSettings.ENABLED_SETTING.get(settings);
        order = RealmSettings.ORDER_SETTING.get(settings);
        this.threadContext = threadContext;
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

    public Logger logger(Class clazz) {
        return Loggers.getLogger(clazz, globalSettings);
    }

    public Environment env() {
        return env;
    }

    public ThreadContext threadContext() {
        return threadContext;
    }
}
