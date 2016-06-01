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

package org.elasticsearch.xpack.security.authz.store;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.xpack.security.authz.permission.Role;

import java.util.HashMap;
import java.util.Map;

/**
 * A composite roles store that combines built in roles, file-based roles, and index-based roles. Checks the built in roles first, then the
 * file roles, and finally the index roles.
 */
public class CompositeRolesStore implements RolesStore {

    private final FileRolesStore fileRolesStore;
    private final NativeRolesStore nativeRolesStore;
    private final ReservedRolesStore reservedRolesStore;

    @Inject
    public CompositeRolesStore(FileRolesStore fileRolesStore, NativeRolesStore nativeRolesStore, ReservedRolesStore reservedRolesStore) {
        this.fileRolesStore = fileRolesStore;
        this.nativeRolesStore = nativeRolesStore;
        this.reservedRolesStore = reservedRolesStore;
    }
    
    public Role role(String role) {
        // builtins first
        Role builtIn = reservedRolesStore.role(role);
        if (builtIn != null) {
            return builtIn;
        }

        // Try the file next, then the index if it isn't there
        Role fileRole = fileRolesStore.role(role);
        if (fileRole != null) {
            return fileRole;
        }

        return nativeRolesStore.role(role);
    }

    @Override
    public Map<String, Object> usageStats() {
        Map<String, Object> usage = new HashMap<>(2);
        usage.put("file", fileRolesStore.usageStats());
        usage.put("native", nativeRolesStore.usageStats());
        return usage;
    }
}
