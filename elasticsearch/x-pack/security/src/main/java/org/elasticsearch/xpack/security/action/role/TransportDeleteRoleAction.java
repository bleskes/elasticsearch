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

package org.elasticsearch.xpack.security.action.role;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.HandledTransportAction;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xpack.security.authz.store.NativeRolesStore;
import org.elasticsearch.xpack.security.authz.store.ReservedRolesStore;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

public class TransportDeleteRoleAction extends HandledTransportAction<DeleteRoleRequest, DeleteRoleResponse> {

    private final NativeRolesStore rolesStore;

    @Inject
    public TransportDeleteRoleAction(Settings settings, ThreadPool threadPool, ActionFilters actionFilters,
                                     IndexNameExpressionResolver indexNameExpressionResolver, NativeRolesStore rolesStore,
                                     TransportService transportService) {
        super(settings, DeleteRoleAction.NAME, threadPool, transportService, actionFilters, indexNameExpressionResolver,
                DeleteRoleRequest::new);
        this.rolesStore = rolesStore;
    }

    @Override
    protected void doExecute(DeleteRoleRequest request, ActionListener<DeleteRoleResponse> listener) {
        if (ReservedRolesStore.isReserved(request.name())) {
            listener.onFailure(new IllegalArgumentException("role [" + request.name() + "] is reserved and cannot be deleted"));
            return;
        }

        try {
            rolesStore.deleteRole(request, new ActionListener<Boolean>() {
                @Override
                public void onResponse(Boolean found) {
                    listener.onResponse(new DeleteRoleResponse(found));
                }

                @Override
                public void onFailure(Exception t) {
                    listener.onFailure(t);
                }
            });
        } catch (Exception e) {
            logger.error("failed to delete role [{}]", e, request.name());
            listener.onFailure(e);
        }
    }
}
