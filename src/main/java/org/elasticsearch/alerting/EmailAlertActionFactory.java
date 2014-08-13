package org.elasticsearch.alerting;

import java.util.List;

public class EmailAlertActionFactory implements AlertActionFactory{

    @Override
    public AlertAction createAction(Object parameters) {
        EmailAlertAction action = new EmailAlertAction();
        if (parameters instanceof List){
            for (String emailAddress : (List<String>)parameters) {
                action.addEmailAddress(emailAddress);
            }
        }
        return action;
    }
}
