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

package org.elasticsearch.xpack.security.authc.ldap;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPInterface;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.support.PlainActionFuture;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.xpack.security.authc.ldap.support.LdapSession.GroupsResolver;
import org.elasticsearch.test.ESTestCase;
import org.junit.After;
import org.junit.Before;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

public abstract class GroupsResolverTestCase extends ESTestCase {

    LDAPConnection ldapConnection;

    protected abstract String ldapUrl();

    protected abstract String bindDN();

    protected abstract String bindPassword();

    @Before
    public void setUpLdapConnection() throws Exception {
        Path keystore = getDataPath("../ldap/support/ldaptrust.jks");
        this.ldapConnection = LdapTestUtils.openConnection(ldapUrl(), bindDN(), bindPassword(), keystore);
    }

    @After
    public void tearDownLdapConnection() throws Exception {
        if (ldapConnection != null) {
            ldapConnection.close();
        }
    }

    protected static List<String> resolveBlocking(GroupsResolver resolver, LDAPInterface ldapConnection, String dn, TimeValue timeLimit,
                                                  Logger logger, Collection<Attribute> attributes) {
        PlainActionFuture<List<String>> future = new PlainActionFuture<>();
        resolver.resolve(ldapConnection, dn, timeLimit, logger, attributes, future);
        return future.actionGet();
    }
}
