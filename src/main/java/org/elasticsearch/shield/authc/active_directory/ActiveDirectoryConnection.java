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

package org.elasticsearch.shield.authc.active_directory;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.shield.authc.support.ldap.AbstractLdapConnection;

import javax.naming.directory.DirContext;

/**
 * An Ldap Connection customized for active directory.
 */
public class ActiveDirectoryConnection extends AbstractLdapConnection {

    /**
     * This object is intended to be constructed by the LdapConnectionFactory
     */
    ActiveDirectoryConnection(ESLogger logger, DirContext ctx, String boundName, String groupSearchDN, TimeValue timeout) {
        super(logger, ctx, boundName, new ActiveDirectoryGroupsResolver(logger, groupSearchDN), timeout);
    }

}
