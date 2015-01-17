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

package org.elasticsearch.shield.authc.support.ldap;

import org.elasticsearch.shield.authc.ldap.LdapException;

import javax.naming.directory.SearchControls;

/**
 *
 */
public enum SearchScope {

    BASE(SearchControls.OBJECT_SCOPE),
    ONE_LEVEL(SearchControls.ONELEVEL_SCOPE),
    SUB_TREE(SearchControls.SUBTREE_SCOPE);

    private final int scope;

    SearchScope(int scope) {
        this.scope = scope;
    }

    public int scope() {
        return scope;
    }

    public static SearchScope resolve(String scope, SearchScope defaultScope) {
        if (scope == null) {
            return defaultScope;
        }
        switch (scope.toLowerCase()) {
            case "base":
            case "object": return BASE;
            case "one_level" : return ONE_LEVEL;
            case "sub_tree" : return SUB_TREE;
            default:
                throw new LdapException("Unknown search scope [" + scope + "]");
        }
    }
}
