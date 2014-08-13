package org.elasticsearch.alerting;

public interface AlertActionFactory {
    AlertAction createAction(Object parameters);
}
