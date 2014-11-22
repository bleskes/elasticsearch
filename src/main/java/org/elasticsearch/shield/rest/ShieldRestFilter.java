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

package org.elasticsearch.shield.rest;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.rest.*;
import org.elasticsearch.shield.authc.AuthenticationService;

/**
 *
 */
public class ShieldRestFilter extends RestFilter {

    private final AuthenticationService service;

    @Inject
    public ShieldRestFilter(AuthenticationService service, RestController controller) {
        this.service = service;
        controller.registerFilter(this);
    }

    @Override
    public int order() {
        return Integer.MIN_VALUE;
    }

    @Override
    public void process(RestRequest request, RestChannel channel, RestFilterChain filterChain) throws Exception {

        // CORS - allow for preflight unauthenticated OPTIONS request
        if (request.method() != RestRequest.Method.OPTIONS) {
            service.authenticate(request);
        }

        filterChain.continueProcessing(request, channel);
    }
}
