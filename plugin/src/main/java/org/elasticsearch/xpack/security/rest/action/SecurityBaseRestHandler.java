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

package org.elasticsearch.xpack.security.rest.action;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.license.LicenseUtils;
import org.elasticsearch.license.XPackLicenseState;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestRequest;

import java.io.IOException;

import static org.elasticsearch.xpack.XPackPlugin.SECURITY;

/**
 * Base class for security rest handlers. This handler takes care of ensuring that the license
 * level is valid so that security can be used!
 */
public abstract class SecurityBaseRestHandler extends BaseRestHandler {

    protected final XPackLicenseState licenseState;

    /**
     * @param settings the node's settings
     * @param licenseState the license state that will be used to determine if security is licensed
     */
    protected SecurityBaseRestHandler(Settings settings, XPackLicenseState licenseState) {
        super(settings);
        this.licenseState = licenseState;
    }

    /**
     * Calls the {@link #innerPrepareRequest(RestRequest, NodeClient)} method and then checks the
     * license state. If the license state allows auth, the result from
     * {@link #innerPrepareRequest(RestRequest, NodeClient)} is returned, otherwise a default error
     * response will be returned indicating that security is not licensed.
     *
     * Note: the implementing rest handler is called before the license is checked so that we do not
     * trip the unused parameters check
     */
    protected final RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        RestChannelConsumer consumer = innerPrepareRequest(request, client);
        if (licenseState.isAuthAllowed()) {
            return consumer;
        } else {
            return channel -> channel.sendResponse(new BytesRestResponse(channel, LicenseUtils.newComplianceException(SECURITY)));
        }
    }

    /**
     * Implementers should implement this method as they normally would for
     * {@link BaseRestHandler#prepareRequest(RestRequest, NodeClient)} and ensure that all request
     * parameters are consumed prior to returning a value. The returned value is not guaranteed to
     * be executed unless security is licensed and all request parameters are known
     */
    protected abstract RestChannelConsumer innerPrepareRequest(RestRequest request, NodeClient client) throws IOException;
}
