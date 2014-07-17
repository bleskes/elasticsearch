package org.elasticsearch.shield;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.shield.authc.AuthenticationService;
import org.elasticsearch.shield.authz.AuthorizationService;

public class SecurityActionFilter {

}

///**
// *
// */
//public class SecurityActionFilter implements ActionFilter {
//
//    private final AuthenticationService authenticationService;
//    private final AuthorizationService authorizationService;
//
//    @Inject
//    public SecurityActionFilter(AuthenticationService authenticationService, AuthorizationService authorizationService) {
//        this.authenticationService = authenticationService;
//        this.authorizationService = authorizationService;
//    }
//
//    @Override
//    public void process(String action, ActionRequest actionRequest, ActionListener actionListener, ActionFilterChain actionFilterChain) {
//        User user = authenticationService.authenticate(action, actionRequest);
//        authorizationService.authorize(user, action, actionRequest);
//        actionFilterChain.continueProcessing(action, actionRequest, actionListener);
//    }
//
//    @Override
//    public int order() {
//        return Integer.MIN_VALUE;
//    }
//}
