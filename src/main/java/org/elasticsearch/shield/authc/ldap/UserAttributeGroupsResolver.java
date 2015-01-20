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
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.shield.authc.support.ldap.AbstractLdapConnection;
import org.elasticsearch.shield.authc.support.ldap.ClosableNamingEnumeration;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import java.util.LinkedList;
import java.util.List;

/**
*
*/
class UserAttributeGroupsResolver implements AbstractLdapConnection.GroupsResolver {

    private final String attribute;

    public UserAttributeGroupsResolver(Settings settings) {
        this(settings.get("user_group_attribute", "memberOf"));
    }

    public UserAttributeGroupsResolver(String attribute) {
        this.attribute = attribute;
    }

    @Override
    public List<String> resolve(DirContext ctx, String userDn, TimeValue timeout, ESLogger logger) {
        List<String> groupDns = new LinkedList<>();
        try {
            Attributes results = ctx.getAttributes(userDn, new String[] { attribute });
            try (ClosableNamingEnumeration<? extends Attribute> ae = new ClosableNamingEnumeration<>(results.getAll())) {
                while (ae.hasMore()) {
                    Attribute attr = ae.next();
                    for (NamingEnumeration attrEnum = attr.getAll(); attrEnum.hasMore(); ) {
                        Object val = attrEnum.next();
                        if (val instanceof String) {
                            String stringVal = (String) val;
                            groupDns.add(stringVal);
                        }
                    }
                }
            }
        } catch (NamingException | LdapException e) {
            throw new LdapException("could not look up group attributes for user", userDn, e);
        }
        return groupDns;
    }
}
