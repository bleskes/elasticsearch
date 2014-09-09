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

package org.elasticsearch.shield.authc.ldap;

import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

/**
 *
 */
public class ApacheDsRule implements MethodRule {

    private ApacheDsEmbedded ldap;

    @Override
    public Statement apply(final Statement base, final FrameworkMethod method, final Object target) {

        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    ldap = new ApacheDsEmbedded("o=sevenSeas", "seven-seas.ldif", target.getClass().getName());
                    ldap.startServer();
                    base.evaluate();
                } finally {
                    ldap.stopAndCleanup();
                }
            }
        };
    }

    public String getUrl() {
        return ldap.getUrl();
    }
}
