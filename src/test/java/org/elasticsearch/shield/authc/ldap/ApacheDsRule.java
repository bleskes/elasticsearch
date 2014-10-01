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

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;

/**
 *
 */
public class ApacheDsRule extends ExternalResource {

    private final ESLogger logger = Loggers.getLogger(getClass());

    private ApacheDsEmbedded ldap;
    private final TemporaryFolder temporaryFolder;

    public ApacheDsRule(TemporaryFolder temporaryFolder) {
        this.temporaryFolder = temporaryFolder;
    }

    @Override
    protected void before() throws Throwable {
        ldap = new ApacheDsEmbedded("o=sevenSeas", "seven-seas.ldif", temporaryFolder.newFolder());
        ldap.startServer();
    }

    @Override
    protected void after() {
        try {
            ldap.stopAndCleanup();
        } catch (Exception e) {
            logger.error("failed to stop and cleanup the embedded ldap server", e);
        }
    }

    public String getUrl() {
        return ldap.getUrl();
    }
}
