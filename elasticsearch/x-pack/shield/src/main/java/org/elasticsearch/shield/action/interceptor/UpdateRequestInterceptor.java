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
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportRequest;

/**
 * A request interceptor that fails update request if field or document level security is enabled.
 * <p>
 * It can be dangerous for users if document where to be update via a role that has fls or dls enabled,
 * because only the fields that a role can see would be used to perform the update and without knowing the user may
 * remove the other fields, not visible for him, from the document being updated.
 */
public class UpdateRequestInterceptor extends FieldAndDocumentLevelSecurityRequestInterceptor<UpdateRequest> {

    @Inject
    public UpdateRequestInterceptor(Settings settings, ThreadPool threadPool) {
        super(settings, threadPool.getThreadContext());
    }

    @Override
    protected void disableFeatures(UpdateRequest updateRequest, boolean fieldLevelSecurityEnabled, boolean documentLevelSecurityEnabled) {
        throw new ElasticsearchSecurityException("Can't execute an update request if field or document level security is enabled",
                RestStatus.BAD_REQUEST);
    }

    @Override
    public boolean supports(TransportRequest request) {
        return request instanceof UpdateRequest;
    }
}
