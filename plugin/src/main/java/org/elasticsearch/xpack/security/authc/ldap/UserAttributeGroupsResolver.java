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
import com.unboundid.ldap.sdk.LDAPInterface;
import com.unboundid.ldap.sdk.SearchScope;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.xpack.security.authc.ldap.support.LdapSession.GroupsResolver;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.function.Function;

import static org.elasticsearch.xpack.security.authc.ldap.support.LdapUtils.OBJECT_CLASS_PRESENCE_FILTER;
import static org.elasticsearch.xpack.security.authc.ldap.support.LdapUtils.searchForEntry;
import static org.elasticsearch.xpack.security.authc.ldap.support.SessionFactory.IGNORE_REFERRAL_ERRORS_SETTING;

/**
* Resolves the groups of a user based on the value of a attribute of the user's ldap entry
*/
class UserAttributeGroupsResolver implements GroupsResolver {

    private static final Setting<String> ATTRIBUTE = new Setting<>("user_group_attribute", "memberOf",
            Function.identity(), Setting.Property.NodeScope);
    private final String attribute;
    private final boolean ignoreReferralErrors;

    UserAttributeGroupsResolver(Settings settings) {
        this(ATTRIBUTE.get(settings), IGNORE_REFERRAL_ERRORS_SETTING.get(settings));
    }

    private UserAttributeGroupsResolver(String attribute, boolean ignoreReferralErrors) {
        this.attribute = Objects.requireNonNull(attribute);
        this.ignoreReferralErrors = ignoreReferralErrors;
    }

    @Override
    public void resolve(LDAPInterface connection, String userDn, TimeValue timeout, Logger logger, Collection<Attribute> attributes,
                        ActionListener<List<String>> listener) {
        if (attributes != null) {
            List<String> list = attributes.stream().filter((attr) -> attr.getName().equals(attribute))
                    .flatMap(attr -> Arrays.stream(attr.getValues())).collect(Collectors.toList());
            listener.onResponse(Collections.unmodifiableList(list));
        } else {
            searchForEntry(connection, userDn, SearchScope.BASE, OBJECT_CLASS_PRESENCE_FILTER, Math.toIntExact(timeout.seconds()),
                    ignoreReferralErrors, ActionListener.wrap((entry) -> {
                        if (entry == null || entry.hasAttribute(attribute) == false) {
                            listener.onResponse(Collections.emptyList());
                        } else {
                            listener.onResponse(Collections.unmodifiableList(Arrays.asList(entry.getAttributeValues(attribute))));
                        }
                    }, listener::onFailure), attribute);
        }
    }

    @Override
    public String[] attributes() {
        return new String[] { attribute };
    }

    public static Set<Setting<?>> getSettings() {
        return Collections.singleton(ATTRIBUTE);
    }
}
