package org.elasticsearch.plugin.alerting;

import org.elasticsearch.alerting.AlertManager;
import org.elasticsearch.alerting.scheduler.AlertScheduler;
import org.elasticsearch.alerting.AlertingModule;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.plugins.AbstractPlugin;

import java.util.Collection;

public class AlertingPlugin extends AbstractPlugin {
    @Override public String name() {
        return "alerting-plugin";
    }

    @Override public String description() {
        return "Alerting Plugin Description";
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
}
