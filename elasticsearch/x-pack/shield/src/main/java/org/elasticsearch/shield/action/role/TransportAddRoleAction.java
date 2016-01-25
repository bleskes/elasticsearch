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

package org.elasticsearch.shield.action.role;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.HandledTransportAction;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.authz.esnative.ESNativeRolesStore;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

public class TransportAddRoleAction extends HandledTransportAction<AddRoleRequest, AddRoleResponse> {

    private final ESNativeRolesStore rolesStore;

    @Inject
    public TransportAddRoleAction(Settings settings, ThreadPool threadPool, ActionFilters actionFilters,
                                  IndexNameExpressionResolver indexNameExpressionResolver,
                                  ESNativeRolesStore rolesStore, TransportService transportService) {
        super(settings, AddRoleAction.NAME, threadPool, transportService, actionFilters, indexNameExpressionResolver, AddRoleRequest::new);
        this.rolesStore = rolesStore;
    }

    @Override
    protected void doExecute(AddRoleRequest request, ActionListener<AddRoleResponse> listener) {
        rolesStore.addRole(request, new ActionListener<Boolean>() {
            @Override
            public void onResponse(Boolean created) {
                if (created) {
                    logger.info("added role [{}]", request.name());
                } else {
                    logger.info("updated role [{}]", request.name());
                }
                listener.onResponse(new AddRoleResponse(created));
            }

            @Override
            public void onFailure(Throwable t) {
                listener.onFailure(t);
            }
        });
    }
}
