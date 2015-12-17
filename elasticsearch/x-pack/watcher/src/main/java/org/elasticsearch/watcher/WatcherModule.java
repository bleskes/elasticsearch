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

package org.elasticsearch.watcher;


import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.multibindings.Multibinder;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.watcher.support.WatcherIndexTemplateRegistry;
import org.elasticsearch.watcher.support.WatcherIndexTemplateRegistry.TemplateConfig;
import org.elasticsearch.watcher.support.validation.WatcherSettingsValidation;


public class WatcherModule extends AbstractModule {

    public static final String HISTORY_TEMPLATE_NAME = "watch_history";
    public static final String TRIGGERED_TEMPLATE_NAME = "triggered_watches";
    public static final String WATCHES_TEMPLATE_NAME = "watches";
    public static final Setting<Settings> HISTORY_TEMPLATE_SETTING = Setting.groupSetting("watcher.history.index.", true, Setting.Scope.CLUSTER);
    public static final Setting<Settings> TRIGGERED_TEMPLATE_SETTING = Setting.groupSetting("watcher.triggered_watches.index.", true, Setting.Scope.CLUSTER);
    public static final Setting<Settings> WATCHES_TEMPLATE_SETTING = Setting.groupSetting("watcher.watches.index.", true, Setting.Scope.CLUSTER);


    public final static TemplateConfig[] TEMPLATE_CONFIGS = new TemplateConfig[]{
            new TemplateConfig(TRIGGERED_TEMPLATE_NAME, TRIGGERED_TEMPLATE_SETTING),
            new TemplateConfig(HISTORY_TEMPLATE_NAME, HISTORY_TEMPLATE_SETTING),
            new TemplateConfig(WATCHES_TEMPLATE_NAME, WATCHES_TEMPLATE_SETTING)
    };

    protected final Settings settings;

    public WatcherModule(Settings settings) {
        this.settings = settings;
    }

    @Override
    protected void configure() {
        bind(WatcherLifeCycleService.class).asEagerSingleton();
        bind(WatcherSettingsValidation.class).asEagerSingleton();

        bind(WatcherIndexTemplateRegistry.class).asEagerSingleton();
        Multibinder<TemplateConfig> multibinder
                = Multibinder.newSetBinder(binder(), TemplateConfig.class);
        for (TemplateConfig templateConfig : TEMPLATE_CONFIGS) {
            multibinder.addBinding().toInstance(templateConfig);
        }
    }

}
