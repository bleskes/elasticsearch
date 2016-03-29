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
package org.elasticsearch.license.plugin.consumer;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsModule;
import org.elasticsearch.plugins.Plugin;

import java.util.ArrayList;
import java.util.Collection;

public abstract class TestConsumerPluginBase extends Plugin {

    private final boolean isEnabled;

    public TestConsumerPluginBase(Settings settings) {
        this.isEnabled = TransportClient.CLIENT_TYPE.equals(settings.get(Client.CLIENT_TYPE_SETTING_S.getKey())) == false;
    }

    @Override
    public String name() {
        return pluginName();
    }

    @Override
    public String description() {
        return "test licensing consumer plugin";
    }


    @Override
    public Collection<Class<? extends LifecycleComponent>> nodeServices() {
        Collection<Class<? extends LifecycleComponent>> services = new ArrayList<>();
        if (isEnabled) {
            services.add(service());
        }
        return services;
    }

    public void onModule(SettingsModule module) {
        try {
            module.registerSetting(Setting.simpleString("_trial_license_duration_in_seconds", Setting.Property.NodeScope));
            module.registerSetting(Setting.simpleString("_grace_duration_in_seconds", Setting.Property.NodeScope));
        } catch (IllegalArgumentException ex) {
            // already loaded
        }
    }

    public abstract Class<? extends TestPluginServiceBase> service();

    protected abstract String pluginName();

    public abstract String id();
}
