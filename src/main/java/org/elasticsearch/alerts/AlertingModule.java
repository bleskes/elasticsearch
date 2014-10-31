package org.elasticsearch.alerts;

import org.elasticsearch.alerts.actions.AlertActionManager;
import org.elasticsearch.alerts.actions.AlertActionRegistry;
import org.elasticsearch.alerts.rest.AlertRestHandler;
import org.elasticsearch.alerts.scheduler.AlertScheduler;
import org.elasticsearch.alerts.triggers.TriggerManager;
import org.elasticsearch.common.inject.AbstractModule;

public class AlertingModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(AlertsStore.class).asEagerSingleton();
        bind(AlertManager.class).asEagerSingleton();
        bind(AlertActionManager.class).asEagerSingleton();
        bind(TriggerManager.class).asEagerSingleton();
        bind(AlertScheduler.class).asEagerSingleton();
        bind(AlertActionRegistry.class).asEagerSingleton();
        bind(AlertRestHandler.class).asEagerSingleton();
    }

}
