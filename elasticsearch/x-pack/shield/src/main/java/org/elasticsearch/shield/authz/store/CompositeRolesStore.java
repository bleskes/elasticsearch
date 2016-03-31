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

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.shield.authz.permission.Role;

/**
 * A composite roles store that combines file-based and index-based roles
 * lookups. Checks the file first, then the index.
 */
public class CompositeRolesStore implements RolesStore {

    private final FileRolesStore fileRolesStore;
    private final NativeRolesStore nativeRolesStore;
    
    @Inject
    public CompositeRolesStore(FileRolesStore fileRolesStore, NativeRolesStore nativeRolesStore) {
        this.fileRolesStore = fileRolesStore;
        this.nativeRolesStore = nativeRolesStore;
    }
    
    public Role role(String role) {
        // Try the file first, then the index if it isn't there
        Role fileRole = fileRolesStore.role(role);
        if (fileRole != null) {
            return fileRole;
        }

        return nativeRolesStore.role(role);
    }
}
