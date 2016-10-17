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

package org.elasticsearch.xpack.security.authz.accesscontrol;

import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.xpack.security.authz.IndicesAndAliasesResolver;
import org.elasticsearch.xpack.security.authz.permission.FieldPermissions;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.unmodifiableSet;

/**
 * Encapsulates the field and document permissions per concrete index based on the current request.
 */
public class IndicesAccessControl {

    public static final IndicesAccessControl ALLOW_ALL = new IndicesAccessControl(true, Collections.emptyMap());
    public static final IndicesAccessControl ALLOW_NO_INDICES = new IndicesAccessControl(true,
            Collections.singletonMap(IndicesAndAliasesResolver.NO_INDEX_PLACEHOLDER,
                    new IndicesAccessControl.IndexAccessControl(true, new FieldPermissions(), null)));

    private final boolean granted;
    private final Map<String, IndexAccessControl> indexPermissions;

    public IndicesAccessControl(boolean granted, Map<String, IndexAccessControl> indexPermissions) {
        this.granted = granted;
        this.indexPermissions = indexPermissions;
    }

    /**
     * @return The document and field permissions for an index if exist, otherwise <code>null</code> is returned.
     *         If <code>null</code> is being returned this means that there are no field or document level restrictions.
     */
    @Nullable
    public IndexAccessControl getIndexPermissions(String index) {
        return indexPermissions.get(index);
    }

    /**
     * @return Whether any role / permission group is allowed to access all indices.
     */
    public boolean isGranted() {
        return granted;
    }

    /**
     * Encapsulates the field and document permissions for an index.
     */
    public static class IndexAccessControl {

        private final boolean granted;
        private final FieldPermissions fieldPermissions;
        private final Set<BytesReference> queries;

        public IndexAccessControl(boolean granted, FieldPermissions fieldPermissions, Set<BytesReference> queries) {
            this.granted = granted;
            this.fieldPermissions = fieldPermissions;
            this.queries = queries;
        }

        /**
         * @return Whether any role / permission group is allowed to this index.
         */
        public boolean isGranted() {
            return granted;
        }

        /**
         * @return The allowed fields for this index permissions.
         */
        public FieldPermissions getFieldPermissions() {
            return fieldPermissions;
        }

        /**
         * @return The allowed documents expressed as a query for this index permission. If <code>null</code> is returned
         *         then this means that there are no document level restrictions
         */
        @Nullable
        public Set<BytesReference> getQueries() {
            return queries;
        }

        public IndexAccessControl merge(IndexAccessControl other) {
            if (other.isGranted() == false) {
                // nothing to merge
                return this;
            }

            final boolean granted = this.granted;
            if (granted == false) {
                // we do not support negatives, so if the current isn't granted - just return other
                assert other.isGranted();
                return other;
            }

            FieldPermissions newPermissions = FieldPermissions.merge(this.fieldPermissions, other.fieldPermissions);

            Set<BytesReference> queries = null;
            if (this.queries != null && other.getQueries() != null) {
                queries = new HashSet<>();
                if (this.queries != null) {
                    queries.addAll(this.queries);
                }
                if (other.getQueries() != null) {
                    queries.addAll(other.getQueries());
                }
                queries = unmodifiableSet(queries);
            }
            return new IndexAccessControl(granted, newPermissions, queries);
        }


    }
}
