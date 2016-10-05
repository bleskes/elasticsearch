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
package org.elasticsearch.xpack.security.action.interceptor;

import org.elasticsearch.ElasticsearchSecurityException;
import org.elasticsearch.action.bulk.BulkItemRequest;
import org.elasticsearch.action.bulk.BulkShardRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.license.XPackLicenseState;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportRequest;
import org.elasticsearch.xpack.security.authz.AuthorizationService;
import org.elasticsearch.xpack.security.authz.accesscontrol.IndicesAccessControl;
import org.elasticsearch.xpack.security.user.User;

/**
 * Similar to {@link UpdateRequestInterceptor}, but checks if there are update requests embedded in a bulk request.
 */
public class BulkShardRequestInterceptor extends AbstractComponent implements RequestInterceptor<BulkShardRequest> {

    private final ThreadContext threadContext;
    private final XPackLicenseState licenseState;

    @Inject
    public BulkShardRequestInterceptor(Settings settings, ThreadPool threadPool, XPackLicenseState licenseState) {
        super(settings);
        this.threadContext = threadPool.getThreadContext();
        this.licenseState = licenseState;
    }

    public void intercept(BulkShardRequest request, User user) {
        if (licenseState.isDocumentAndFieldLevelSecurityAllowed() == false) {
            return;
        }
        IndicesAccessControl indicesAccessControl = threadContext.getTransient(AuthorizationService.INDICES_PERMISSIONS_KEY);

        for (BulkItemRequest bulkItemRequest : request.items()) {
            IndicesAccessControl.IndexAccessControl indexAccessControl = indicesAccessControl.getIndexPermissions(bulkItemRequest.index());
            if (indexAccessControl != null) {
                boolean fls = indexAccessControl.getFieldPermissions().hasFieldLevelSecurity();
                boolean dls = indexAccessControl.getQueries() != null;
                if (fls || dls) {
                    if (bulkItemRequest.request() instanceof UpdateRequest) {
                        throw new ElasticsearchSecurityException("Can't execute a bulk request with update requests embedded if " +
                                "field or document level security is enabled", RestStatus.BAD_REQUEST);
                    }
                }
            }
            logger.trace("intercepted bulk request for index [{}] without any update requests, continuing execution",
                    bulkItemRequest.index());
        }
    }

    @Override
    public boolean supports(TransportRequest request) {
        return request instanceof BulkShardRequest;
    }
}
