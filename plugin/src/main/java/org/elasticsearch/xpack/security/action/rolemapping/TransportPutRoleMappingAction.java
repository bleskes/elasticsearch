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

public class TransportPutRoleMappingAction
        extends HandledTransportAction<PutRoleMappingRequest, PutRoleMappingResponse> {

    private final NativeRoleMappingStore roleMappingStore;

    @Inject
    public TransportPutRoleMappingAction(Settings settings, ThreadPool threadPool,
                                         ActionFilters actionFilters,
                                         IndexNameExpressionResolver indexNameExpressionResolver,
                                         TransportService transportService,
                                         NativeRoleMappingStore roleMappingStore) {
        super(settings, PutRoleMappingAction.NAME, threadPool, transportService, actionFilters,
                indexNameExpressionResolver, PutRoleMappingRequest::new);
        this.roleMappingStore = roleMappingStore;
    }

    @Override
    protected void doExecute(final PutRoleMappingRequest request,
                             final ActionListener<PutRoleMappingResponse> listener) {
        roleMappingStore.putRoleMapping(request, ActionListener.wrap(
                created -> listener.onResponse(new PutRoleMappingResponse(created)),
                listener::onFailure
        ));
    }
}
