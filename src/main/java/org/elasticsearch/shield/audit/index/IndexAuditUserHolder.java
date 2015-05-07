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

package org.elasticsearch.shield.audit.index;

import org.elasticsearch.action.bulk.BulkAction;
import org.elasticsearch.shield.User;
import org.elasticsearch.shield.authz.Permission;
import org.elasticsearch.shield.authz.Privilege;

/**
 *
 */
public class IndexAuditUserHolder {

    private static final String NAME = "__indexing_audit_user";
    private static final String[] ROLE_NAMES = new String[] { "__indexing_audit_role" };

    private final User user;
    private final Permission.Global.Role role;

    public IndexAuditUserHolder(String indexName) {

        // append the index name with the '*' wildcard so that the principal can write to
        // any index that starts with the given name. this allows us to rollover over
        // audit indices hourly, daily, weekly, etc.
        String indexPattern = indexName + "*";

        this.role = Permission.Global.Role.builder(ROLE_NAMES[0])
                    .add(Privilege.Index.CREATE_INDEX, indexPattern)
                    .add(Privilege.Index.INDEX, indexPattern)
                    .add(Privilege.Index.action(BulkAction.NAME), indexPattern)
                    .build();

        this.user = new User.Simple(NAME, ROLE_NAMES);
    }

    public User user() {
        return user;
    }

    public Permission.Global.Role role() {
        return role;
    }
}
