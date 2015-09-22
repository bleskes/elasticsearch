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

import org.elasticsearch.marvel.agent.settings.MarvelSettings;
import org.elasticsearch.shield.User;
import org.elasticsearch.shield.authz.Permission;
import org.elasticsearch.shield.authz.Privilege;
import org.elasticsearch.transport.TransportMessage;

/**
 *
 */
public class MarvelInternalUserHolder {

    static final String NAME = "__marvel_user";
    static final String[] ROLE_NAMES = new String[] { "__marvel_role" };

    public static final Permission.Global.Role ROLE = Permission.Global.Role.builder(ROLE_NAMES[0])
            .cluster(Privilege.Cluster.action("indices:admin/template/put"))

            // we need all monitoring access
            .cluster(Privilege.Cluster.MONITOR)
            .add(Privilege.Index.MONITOR, "*")

            // and full access to .marvel-* and .marvel-data indices
            .add(Privilege.Index.ALL, MarvelSettings.MARVEL_INDICES_PREFIX + "*")

            // note, we don't need _licenses permission as we're taking the licenses
            // directly form the license service.

            .build();

    final User user = new User.Simple(NAME, ROLE_NAMES);

    public void bindUser(TransportMessage<?> message) {

    }
}
