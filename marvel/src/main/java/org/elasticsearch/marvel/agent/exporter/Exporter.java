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

package org.elasticsearch.marvel.agent.exporter;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.marvel.shield.MarvelSettingsFilter;

import java.util.Collection;

public abstract class Exporter  {

    protected final String type;
    protected final Config config;
    protected final ESLogger logger;

    public Exporter(String type, Config config) {
        this.type = type;
        this.config = config;
        this.logger = config.logger(getClass());
    }

    public String type() {
        return type;
    }

    public String name() {
        return config.name;
    }

    public boolean masterOnly() {
        return false;
    }

    public abstract void export(Collection<MarvelDoc> marvelDocs) throws Exception;

    public abstract void close();

    protected String settingFQN(String setting) {
        return Exporters.EXPORTERS_SETTING + "." + config.name + "." + setting;
    }

    public static class Config {

        private final String name;
        private final boolean enabled;
        private final Settings globalSettings;
        private final Settings settings;

        public Config(String name, Settings globalSettings, Settings settings) {
            this.name = name;
            this.globalSettings = globalSettings;
            this.settings = settings;
            this.enabled = settings.getAsBoolean("enabled", true);
        }

        public String name() {
            return name;
        }

        public boolean enabled() {
            return enabled;
        }

        public Settings settings() {
            return settings;
        }

        public ESLogger logger(Class clazz) {
            return Loggers.getLogger(clazz, globalSettings);
        }
    }

    public static abstract class Factory<E extends Exporter> {

        private final String type;
        private final boolean singleton;

        public Factory(String type, boolean singleton) {
            this.type = type;
            this.singleton = singleton;
        }

        public String type() {
            return type;
        }

        public boolean singleton() {
            return singleton;
        }

        public void filterOutSensitiveSettings(String prefix, MarvelSettingsFilter filter) {
        }

        public abstract E create(Config config);
    }

}