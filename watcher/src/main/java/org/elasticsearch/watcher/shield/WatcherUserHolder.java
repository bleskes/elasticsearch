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

package org.elasticsearch.watcher.shield;

import org.elasticsearch.shield.User;
import org.elasticsearch.shield.authz.Permission;
import org.elasticsearch.shield.authz.Privilege;

/**
 *
 */
public class WatcherUserHolder {

    static final String NAME = "__watcher_user";
    static final String[] ROLE_NAMES = new String[] { "__watcher_role" };

    public static final Permission.Global.Role ROLE = Permission.Global.Role.builder(ROLE_NAMES[0])
        .cluster(Privilege.Cluster.action("indices:admin/template/put"))

            // for now, the watches will be executed under the watcher user, meaning, all actions
            // taken as part of the execution will be executed on behalf of this user. this includes
            // the index action, search input and search transform. For this reason the watcher user
            // requires full access to all indices in the cluster.
            //
            // at later phases we'll want to execute the watch on behalf of the user who registers
            // it. this will require some work to attache/persist that user to/with the watch.
        .add(Privilege.Index.ALL, "*")

        .build();

    final User user = new User.Simple(NAME, ROLE_NAMES);

}
