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

package org.elasticsearch.xpack.rest.action;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.license.XPackInfoResponse;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.action.RestBuilderListener;
import org.elasticsearch.xpack.XPackClient;
import org.elasticsearch.xpack.action.XPackInfoRequest;
import org.elasticsearch.xpack.rest.XPackRestHandler;

import java.io.IOException;
import java.util.EnumSet;

import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestRequest.Method.HEAD;
import static org.elasticsearch.rest.RestStatus.OK;

public class RestXPackInfoAction extends XPackRestHandler {
    public RestXPackInfoAction(Settings settings, RestController controller) {
        super(settings);
        controller.registerHandler(HEAD, URI_BASE, this);
        controller.registerHandler(GET, URI_BASE, this);
    }

    @Override
    public RestChannelConsumer doPrepareRequest(RestRequest request, XPackClient client) throws IOException {

        // we piggyback verbosity on "human" output
        boolean verbose = request.paramAsBoolean("human", true);

        EnumSet<XPackInfoRequest.Category> categories = XPackInfoRequest.Category
                .toSet(request.paramAsStringArray("categories", new String[] { "_all" }));
        return channel ->
                client.prepareInfo()
                        .setVerbose(verbose)
                        .setCategories(categories)
                        .execute(new RestBuilderListener<XPackInfoResponse>(channel) {
                            @Override
                            public RestResponse buildResponse(XPackInfoResponse infoResponse, XContentBuilder builder) throws Exception {

                                builder.startObject();

                                if (infoResponse.getBuildInfo() != null) {
                                    builder.field("build", infoResponse.getBuildInfo(), request);
                                }

                                if (infoResponse.getLicenseInfo() != null) {
                                    builder.field("license", infoResponse.getLicenseInfo(), request);
                                } else if (categories.contains(XPackInfoRequest.Category.LICENSE)) {
                                    // if the user requested the license info, and there is no license, we should send
                                    // back an explicit null value (indicating there is no license). This is different
                                    // than not adding the license info at all
                                    builder.nullField("license");
                                }

                                if (infoResponse.getFeatureSetsInfo() != null) {
                                    builder.field("features", infoResponse.getFeatureSetsInfo(), request);
                                }

                                if (verbose) {
                                    builder.field("tagline", "You know, for X");
                                }

                                builder.endObject();
                                return new BytesRestResponse(OK, builder);
                            }
                        });
    }
}
