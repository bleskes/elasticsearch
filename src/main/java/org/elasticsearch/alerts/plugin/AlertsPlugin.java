package org.elasticsearch.alerts.plugin;

import org.elasticsearch.alerts.AlertManager;
import org.elasticsearch.alerts.scheduler.AlertScheduler;
import org.elasticsearch.alerts.AlertingModule;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.AbstractPlugin;

import java.util.Collection;
import static org.elasticsearch.common.settings.ImmutableSettings.settingsBuilder;

public class AlertsPlugin extends AbstractPlugin {

    public static final String NAME = "alerts";

    @Override public String name() {
        return NAME;
    }

    @Override public String description() {
        return "Elasticsearch Alerts";
    }

    @Override
    public Collection<java.lang.Class<? extends LifecycleComponent>> services() {
        Collection<java.lang.Class<? extends LifecycleComponent>> services = Lists.newArrayList();
        services.add(AlertManager.class);
        services.add(AlertScheduler.class);
        return services;
    }

    @Override
    public Collection<Class<? extends Module>> modules() {
        Collection<Class<? extends Module>> modules = Lists.newArrayList();
        modules.add(AlertingModule.class);
        return modules;
    }

    @Override
    public Settings additionalSettings() {
        return settingsBuilder()
                .put("threadpool."+ NAME + ".type","cached")
                .build();
    }

}
