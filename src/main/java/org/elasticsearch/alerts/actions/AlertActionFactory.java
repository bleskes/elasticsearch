package org.elasticsearch.alerts.actions;

public interface AlertActionFactory {
    AlertAction createAction(Object parameters);
}
