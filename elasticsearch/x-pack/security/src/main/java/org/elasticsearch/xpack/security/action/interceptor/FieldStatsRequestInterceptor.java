/*
 * ELASTICSEARCH CONFIDENTIAL
 *  __________________
 *
 * [2014] Elasticsearch Incorporated. All Rights Reserved.
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

import org.elasticsearch.action.fieldstats.FieldStatsRequest;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportRequest;

/**
 * Intercepts requests to shards to field level stats and strips fields that the user is not allowed to access from the response.
 */
public class FieldStatsRequestInterceptor extends FieldAndDocumentLevelSecurityRequestInterceptor<FieldStatsRequest> {
    @Inject
    public FieldStatsRequestInterceptor(Settings settings, ThreadPool threadPool) {
        super(settings, threadPool.getThreadContext());
    }

    @Override
    public boolean supports(TransportRequest request) {
        return request instanceof FieldStatsRequest;
    }

    @Override
    protected void disableFeatures(FieldStatsRequest request, boolean fieldLevelSecurityEnabled, boolean documentLevelSecurityEnabled) {
        if (fieldLevelSecurityEnabled) {
            request.setUseCache(false);
        }
    }
}
