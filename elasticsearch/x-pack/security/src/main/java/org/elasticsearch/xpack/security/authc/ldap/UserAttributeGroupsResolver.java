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
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPInterface;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.xpack.security.authc.ldap.support.LdapSession.GroupsResolver;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.elasticsearch.xpack.security.authc.ldap.support.LdapUtils.OBJECT_CLASS_PRESENCE_FILTER;
import static org.elasticsearch.xpack.security.authc.ldap.support.LdapUtils.searchForEntry;

/**
* Resolves the groups of a user based on the value of a attribute of the user's ldap entry
*/
class UserAttributeGroupsResolver implements GroupsResolver {

    private final String attribute;

    UserAttributeGroupsResolver(Settings settings) {
        this(settings.get("user_group_attribute", "memberOf"));
    }

    private UserAttributeGroupsResolver(String attribute) {
        this.attribute = Objects.requireNonNull(attribute);
    }

    @Override
    public List<String> resolve(LDAPInterface connection, String userDn, TimeValue timeout, ESLogger logger,
                                Collection<Attribute> attributes) throws LDAPException {
        if (attributes != null) {
            for (Attribute attribute : attributes) {
                if (attribute.getName().equals(attribute)) {
                    String[] values = attribute.getValues();
                    return Arrays.asList(values);
                }
            }
            return Collections.emptyList();
        }

        SearchRequest request = new SearchRequest(userDn, SearchScope.BASE, OBJECT_CLASS_PRESENCE_FILTER, attribute);
        request.setTimeLimitSeconds(Math.toIntExact(timeout.seconds()));
        SearchResultEntry result = searchForEntry(connection, request, logger);
        if (result == null) {
            return Collections.emptyList();
        }
        Attribute attributeReturned = result.getAttribute(attribute);
        if (attributeReturned == null) {
            return Collections.emptyList();
        }
        String[] values = attributeReturned.getValues();
        return Arrays.asList(values);
    }

    @Override
    public String[] attributes() {
        return new String[] { attribute };
    }
}
