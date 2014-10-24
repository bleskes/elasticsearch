package org.elasticsearch.alerting;

import org.elasticsearch.alerting.actions.AlertActionManager;
import org.elasticsearch.alerting.rest.AlertRestHandler;
import org.elasticsearch.alerting.scheduler.AlertScheduler;
import org.elasticsearch.alerting.triggers.TriggerManager;
import org.elasticsearch.common.inject.AbstractModule;

public class AlertingModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(AlertManager.class).asEagerSingleton();
        bind(TriggerManager.class).asEagerSingleton();
        bind(AlertScheduler.class).asEagerSingleton();
        bind(AlertActionManager.class).asEagerSingleton();
        bind(AlertRestHandler.class).asEagerSingleton();
    }

}
