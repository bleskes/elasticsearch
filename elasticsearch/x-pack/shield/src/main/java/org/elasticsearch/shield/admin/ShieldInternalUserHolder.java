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

package org.elasticsearch.shield.admin;

import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateAction;
import org.elasticsearch.shield.User;
import org.elasticsearch.shield.authz.Permission;
import org.elasticsearch.shield.authz.Privilege;

/**
 * User holder for the shield internal user that manages the {@code .shield}
 * index. Has permission to monitor the cluster as well as all actions that deal
 * with the shield admin index.
 */
public class ShieldInternalUserHolder {

    private static final String NAME = "__es_internal_user";
    private static final String[] ROLES = new String[] { "__es_internal_role" };
    public static final Permission.Global.Role ROLE = Permission.Global.Role.builder(ROLES[0])
            .cluster(Privilege.Cluster.get(new Privilege.Name(PutIndexTemplateAction.NAME, "cluster:admin/shield/realm/cache/clear*", "cluster:admin/shield/roles/cache/clear*")))
            .add(Privilege.Index.ALL, ShieldTemplateService.SHIELD_ADMIN_INDEX_NAME)
            .build();
    private static final User SHIELD_INTERNAL_USER = new User(NAME, ROLES);

    public User user() {
        return SHIELD_INTERNAL_USER;
    }

    public static boolean isShieldInternalUser(User user) {
        return SHIELD_INTERNAL_USER.equals(user);
    }
}
