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

package org.elasticsearch.xpack.security.authc.ldap.support;

import com.unboundid.ldap.sdk.SearchScope;

import java.util.Locale;

/**
 *
 */
public enum LdapSearchScope {

    BASE(SearchScope.BASE),
    ONE_LEVEL(SearchScope.ONE),
    SUB_TREE(SearchScope.SUB);

    private final SearchScope scope;

    LdapSearchScope(SearchScope scope) {
        this.scope = scope;
    }

    public SearchScope scope() {
        return scope;
    }

    public static LdapSearchScope resolve(String scope, LdapSearchScope defaultScope) {
        if (scope == null) {
            return defaultScope;
        }
        switch (scope.toLowerCase(Locale.ENGLISH)) {
            case "base":
            case "object": return BASE;
            case "one_level" : return ONE_LEVEL;
            case "sub_tree" : return SUB_TREE;
            default:
                throw new IllegalArgumentException("unknown search scope [" + scope + "]");
        }
    }
}
