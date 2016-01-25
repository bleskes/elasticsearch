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

package org.elasticsearch.marvel.shield;

import org.elasticsearch.action.admin.indices.template.get.GetIndexTemplatesAction;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateAction;
import org.elasticsearch.marvel.agent.settings.MarvelSettings;
import org.elasticsearch.shield.User;
import org.elasticsearch.shield.authz.permission.Role;
import org.elasticsearch.shield.authz.privilege.ClusterPrivilege;
import org.elasticsearch.shield.authz.privilege.IndexPrivilege;
import org.elasticsearch.shield.authz.privilege.Privilege;

/**
 *
 */
public class InternalMarvelUser extends User {

    static final String NAME = "__marvel_user";
    static final String[] ROLE_NAMES = new String[] { "__marvel_role" };

    public static final InternalMarvelUser INSTANCE = new InternalMarvelUser(NAME, ROLE_NAMES);

    public static final Role ROLE = Role.builder(ROLE_NAMES[0])
            .cluster(ClusterPrivilege.get(new Privilege.Name(
                    PutIndexTemplateAction.NAME + "*",
                    GetIndexTemplatesAction.NAME + "*",
                    ClusterPrivilege.MONITOR.name().toString())))

            // we need all monitoring access
            .add(IndexPrivilege.MONITOR, "*")

            // and full access to .marvel-es-* and .marvel-es-data indices
            .add(IndexPrivilege.ALL, MarvelSettings.MARVEL_INDICES_PREFIX + "*")

            // note, we don't need _license permission as we're taking the licenses
            // directly form the license service.

            .build();

    InternalMarvelUser(String username, String[] roles) {
        super(username, roles);
    }
}
