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

import org.elasticsearch.shield.authc.support.SecuredString;

/**
 * This factory holds settings needed for authenticating to LDAP and creating LdapConnections.
 * Each created LdapConnection needs to be closed or else connections will pill up consuming resources.
 *
 * A standard looking usage pattern could look like this:
 <pre>
    try (LdapConnection session = ldapFac.bindXXX(...);
        ...do stuff with the session
    }
 </pre>
 */
public interface LdapConnectionFactory {

    static final String URLS_SETTING = "url"; //comma separated

    /**
     * Password authenticated bind
     * @param user name of the user to authenticate the connection with.
     */
    LdapConnection bind(String user, SecuredString password) ;

}
