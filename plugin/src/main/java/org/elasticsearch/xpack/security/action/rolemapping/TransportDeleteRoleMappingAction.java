/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2017] Elasticsearch Incorporated. All Rights Reserved.
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

package org.elasticsearch.xpack.security.action.rolemapping;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.HandledTransportAction;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.security.authc.support.mapper.NativeRoleMappingStore;

public class TransportDeleteRoleMappingAction
        extends HandledTransportAction<DeleteRoleMappingRequest, DeleteRoleMappingResponse> {

    private final NativeRoleMappingStore roleMappingStore;

    @Inject
    public TransportDeleteRoleMappingAction(Settings settings, ThreadPool threadPool,
                                            ActionFilters actionFilters,
                                            IndexNameExpressionResolver indexNameExpressionResolver,
                                            TransportService transportService,
                                            NativeRoleMappingStore roleMappingStore) {
        super(settings, DeleteRoleMappingAction.NAME, threadPool, transportService, actionFilters,
                indexNameExpressionResolver, DeleteRoleMappingRequest::new);
        this.roleMappingStore = roleMappingStore;
    }

    @Override
    protected void doExecute(DeleteRoleMappingRequest request,
                             ActionListener<DeleteRoleMappingResponse> listener) {
        roleMappingStore.deleteRoleMapping(request, new ActionListener<Boolean>() {
            @Override
            public void onResponse(Boolean found) {
                listener.onResponse(new DeleteRoleMappingResponse(found));
            }

            @Override
            public void onFailure(Exception t) {
                listener.onFailure(t);
            }
        });
    }
}
