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

package org.elasticsearch.shield.authz.store;

import org.elasticsearch.shield.authz.Permission;
import org.elasticsearch.shield.authz.Privilege;

/**
 *
 */
public interface RolesStore {

    Permission.Global permission(String role);

    static interface Writable extends RolesStore {

        void set(String role, Privilege.Index privilege, String... indices);

        void grant(String role, Privilege.Index privilege, String... indices);

        void revoke(String role, Privilege.Index privileges, String... indices);

        void grant(String role, Privilege.Cluster privilege);

        void revoke(String role, Privilege.Cluster privileges);
    }

}
