package org.elasticsearch.alerting;

public interface AlertAction {
    public boolean doAction(String alertName, AlertResult alert);
}
