/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated. All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elasticsearch Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Elasticsearch Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elasticsearch Incorporated.
 */

package org.elasticsearch.example;

import org.elasticsearch.example.realm.CustomAuthenticationFailureHandler;
import org.elasticsearch.example.realm.CustomRealm;
import org.elasticsearch.example.realm.CustomRealmFactory;
import org.elasticsearch.xpack.security.authc.AuthenticationModule;
import org.elasticsearch.xpack.extensions.XPackExtension;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collection;

public class ExampleRealmExtension extends XPackExtension {
    @Override
    public String name() {
        return "custom realm example";
    }

    @Override
    public String description() {
        return "a very basic implementation of a custom realm to validate it works";
    }

    public void onModule(AuthenticationModule authenticationModule) {
        authenticationModule.addCustomRealm(CustomRealm.TYPE, CustomRealmFactory.class);
        authenticationModule.setAuthenticationFailureHandler(CustomAuthenticationFailureHandler.class);
        // check that the extension's policy works.
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            System.getSecurityManager().checkPrintJobAccess();
            return null;
        });
    }

    @Override
    public Collection<String> getRestHeaders() {
        return Arrays.asList(CustomRealm.USER_HEADER, CustomRealm.PW_HEADER);
    }
}
