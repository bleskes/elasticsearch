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

import org.elasticsearch.action.FailedNodeException;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.nodes.TransportNodesAction;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.authz.store.NativeRolesStore;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

import java.util.List;

/**
 *
 */
public class TransportClearRolesCacheAction extends TransportNodesAction<ClearRolesCacheRequest, ClearRolesCacheResponse,
        ClearRolesCacheRequest.Node, ClearRolesCacheResponse.Node> {

    private final NativeRolesStore rolesStore;

    @Inject
    public TransportClearRolesCacheAction(Settings settings, ClusterName clusterName, ThreadPool threadPool,
                                          ClusterService clusterService, TransportService transportService, ActionFilters actionFilters,
                                          NativeRolesStore rolesStore, IndexNameExpressionResolver indexNameExpressionResolver) {
        super(settings, ClearRolesCacheAction.NAME, clusterName, threadPool, clusterService, transportService,
              actionFilters, indexNameExpressionResolver, ClearRolesCacheRequest::new, ClearRolesCacheRequest.Node::new,
              ThreadPool.Names.MANAGEMENT, ClearRolesCacheResponse.Node.class);
        this.rolesStore = rolesStore;
    }

    @Override
    protected ClearRolesCacheResponse newResponse(ClearRolesCacheRequest request,
                                                  List<ClearRolesCacheResponse.Node> responses, List<FailedNodeException> failures) {
        return new ClearRolesCacheResponse(clusterName, responses, failures);
    }

    @Override
    protected ClearRolesCacheRequest.Node newNodeRequest(String nodeId, ClearRolesCacheRequest request) {
        return new ClearRolesCacheRequest.Node(request, nodeId);
    }

    @Override
    protected ClearRolesCacheResponse.Node newNodeResponse() {
        return new ClearRolesCacheResponse.Node();
    }

    @Override
    protected ClearRolesCacheResponse.Node nodeOperation(ClearRolesCacheRequest.Node request) {
        if (request.names == null || request.names.length == 0) {
            rolesStore.invalidateAll();
        } else {
            for (String role : request.names) {
                rolesStore.invalidate(role);
            }
        }
        return new ClearRolesCacheResponse.Node(clusterService.localNode());
    }

    @Override
    protected boolean accumulateExceptions() {
        return false;
    }
}
