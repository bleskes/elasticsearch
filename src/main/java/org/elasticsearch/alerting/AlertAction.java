package org.elasticsearch.alerting;

public interface AlertAction {
    public boolean doAction(AlertResult alert);
    public String getActionType();
}
