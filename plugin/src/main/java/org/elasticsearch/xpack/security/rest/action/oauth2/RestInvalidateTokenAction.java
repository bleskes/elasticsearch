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

package org.elasticsearch.xpack.security.rest.action.oauth2;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ConstructingObjectParser;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.license.LicenseUtils;
import org.elasticsearch.license.XPackLicenseState;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.rest.action.RestBuilderListener;
import org.elasticsearch.xpack.XPackPlugin;
import org.elasticsearch.xpack.security.action.token.InvalidateTokenAction;
import org.elasticsearch.xpack.security.action.token.InvalidateTokenRequest;
import org.elasticsearch.xpack.security.action.token.InvalidateTokenResponse;

import java.io.IOException;

import static org.elasticsearch.rest.RestRequest.Method.DELETE;

/**
 * Rest handler for handling access token invalidation requests
 */
public final class RestInvalidateTokenAction extends BaseRestHandler {

    static final ConstructingObjectParser<String, Void> PARSER =
            new ConstructingObjectParser<>("invalidate_token", a -> ((String) a[0]));
    static {
        PARSER.declareString(ConstructingObjectParser.constructorArg(), new ParseField("token"));
    }

    private final XPackLicenseState licenseState;

    public RestInvalidateTokenAction(Settings settings, XPackLicenseState xPackLicenseState,
                                     RestController controller) {
        super(settings);
        this.licenseState = xPackLicenseState;
        controller.registerHandler(DELETE, "/_xpack/security/oauth2/token", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request,
                                                 NodeClient client)throws IOException {
        // this API shouldn't be available if security is disabled by license
        if (licenseState.isAuthAllowed() == false) {
            return channel ->
                    channel.sendResponse(new BytesRestResponse(channel, LicenseUtils.newComplianceException(XPackPlugin.SECURITY)));
        }

        try (XContentParser parser = request.contentParser()) {
            final String token = PARSER.parse(parser, null);
            final InvalidateTokenRequest tokenRequest = new InvalidateTokenRequest(token);
            return channel -> client.execute(InvalidateTokenAction.INSTANCE, tokenRequest,
                    new RestBuilderListener<InvalidateTokenResponse>(channel) {
                        @Override
                        public RestResponse buildResponse(InvalidateTokenResponse invalidateResp,
                                                          XContentBuilder builder) throws Exception {
                            return new BytesRestResponse(RestStatus.OK, builder.startObject()
                                    .field("created", invalidateResp.isCreated())
                                    .endObject());
                        }
                    });
        }
    }
}
