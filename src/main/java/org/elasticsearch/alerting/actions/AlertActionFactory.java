package org.elasticsearch.alerting.actions;

public interface AlertActionFactory {
    AlertAction createAction(Object parameters);
}
