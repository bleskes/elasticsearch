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
package org.elasticsearch.shield.action.interceptor;

import org.elasticsearch.ElasticsearchSecurityException;
import org.elasticsearch.action.IndicesRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.shield.User;
import org.elasticsearch.shield.authz.InternalAuthorizationService;
import org.elasticsearch.shield.authz.accesscontrol.IndicesAccessControl;
import org.elasticsearch.transport.TransportRequest;

/**
 * Simular to {@link UpdateRequestInterceptor}, but checks if there are update requests embedded in a bulk request.
 */
public class BulkRequestInterceptor extends AbstractComponent implements RequestInterceptor<BulkRequest> {

    @Inject
    public BulkRequestInterceptor(Settings settings) {
        super(settings);
    }

    public void intercept(BulkRequest request, User user) {
        IndicesAccessControl indicesAccessControl = ((TransportRequest) request).getFromContext(InternalAuthorizationService.INDICES_PERMISSIONS_KEY);
        for (IndicesRequest indicesRequest : request.subRequests()) {
            for (String index : indicesRequest.indices()) {
                IndicesAccessControl.IndexAccessControl indexAccessControl = indicesAccessControl.getIndexPermissions(index);
                if (indexAccessControl != null) {
                    boolean fls = indexAccessControl.getFields() != null;
                    boolean dls = indexAccessControl.getQueries() != null;
                    if (fls || dls) {
                        logger.debug("intercepted request for index [{}] with field level or document level security enabled, disabling features", index);
                        if (indicesRequest instanceof UpdateRequest) {
                            throw new ElasticsearchSecurityException("Can't execute an bulk request with update requests embedded if field or document level security is enabled", RestStatus.BAD_REQUEST);
                        }
                    }
                }
                logger.trace("intercepted request for index [{}] with neither field level or document level security not enabled, doing nothing", index);
            }
        }
    }
    
    @Override
    public boolean supports(TransportRequest request) {
        return request instanceof BulkRequest;
    }
}
