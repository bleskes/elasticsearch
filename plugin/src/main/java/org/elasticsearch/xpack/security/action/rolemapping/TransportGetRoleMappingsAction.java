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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.HandledTransportAction;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.security.authc.support.mapper.ExpressionRoleMapping;
import org.elasticsearch.xpack.security.authc.support.mapper.NativeRoleMappingStore;

public class TransportGetRoleMappingsAction
        extends HandledTransportAction<GetRoleMappingsRequest, GetRoleMappingsResponse> {

    private final NativeRoleMappingStore roleMappingStore;

    @Inject
    public TransportGetRoleMappingsAction(Settings settings, ThreadPool threadPool,
                                          ActionFilters actionFilters,
                                          IndexNameExpressionResolver indexNameExpressionResolver,
                                          TransportService transportService,
                                          NativeRoleMappingStore nativeRoleMappingStore) {
        super(settings, GetRoleMappingsAction.NAME, threadPool, transportService, actionFilters,
                indexNameExpressionResolver, GetRoleMappingsRequest::new);
        this.roleMappingStore = nativeRoleMappingStore;
    }

    @Override
    protected void doExecute(final GetRoleMappingsRequest request,
                             final ActionListener<GetRoleMappingsResponse> listener) {
        final Set<String> names;
        if (request.getNames() == null || request.getNames().length == 0) {
            names = null;
        } else {
            names = new HashSet<>(Arrays.asList(request.getNames()));
        }
        this.roleMappingStore.getRoleMappings(names, ActionListener.wrap(
                mappings -> {
                    ExpressionRoleMapping[] array = mappings.toArray(
                            new ExpressionRoleMapping[mappings.size()]
                    );
                    listener.onResponse(new GetRoleMappingsResponse(array));
                },
                listener::onFailure
        ));
    }
}
