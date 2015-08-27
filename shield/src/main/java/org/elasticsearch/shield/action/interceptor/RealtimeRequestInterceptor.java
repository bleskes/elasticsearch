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

import org.elasticsearch.action.RealtimeRequest;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.transport.TransportRequest;

/**
 * If field level security is enabled this interceptor disables the realtime feature of get, multi get, termsvector and
 * multi termsvector requests.
 */
public class RealtimeRequestInterceptor extends FieldSecurityRequestInterceptor<RealtimeRequest> {

    @Inject
    public RealtimeRequestInterceptor(Settings settings) {
        super(settings);
    }

    @Override
    public void disableFeatures(RealtimeRequest request) {
        request.realtime(false);
    }

    @Override
    public boolean supports(TransportRequest request) {
        return request instanceof RealtimeRequest;
    }
}
