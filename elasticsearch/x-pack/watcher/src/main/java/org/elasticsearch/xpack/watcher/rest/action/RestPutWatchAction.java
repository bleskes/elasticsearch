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

package org.elasticsearch.xpack.watcher.rest.action;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.rest.action.support.RestBuilderListener;
import org.elasticsearch.xpack.watcher.client.WatcherClient;
import org.elasticsearch.xpack.watcher.rest.WatcherRestHandler;
import org.elasticsearch.xpack.watcher.transport.actions.put.PutWatchRequest;
import org.elasticsearch.xpack.watcher.transport.actions.put.PutWatchResponse;

import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.rest.RestRequest.Method.PUT;
import static org.elasticsearch.rest.RestStatus.CREATED;
import static org.elasticsearch.rest.RestStatus.OK;

/**
 */
public class RestPutWatchAction extends WatcherRestHandler {

    @Inject
    public RestPutWatchAction(Settings settings, RestController controller) {
        super(settings);

        // @deprecated Remove deprecations in 6.0
        controller.registerWithDeprecatedHandler(POST, URI_BASE + "/watch/{id}", this,
                                                 POST, "/_watcher/watch/{id}", deprecationLogger);
        controller.registerWithDeprecatedHandler(PUT, URI_BASE + "/watch/{id}", this,
                                                 PUT, "/_watcher/watch/{id}", deprecationLogger);
    }

    @Override
    protected void handleRequest(final RestRequest request, RestChannel channel, WatcherClient client) throws Exception {
        PutWatchRequest putWatchRequest = new PutWatchRequest(request.param("id"), request.content());
        putWatchRequest.masterNodeTimeout(request.paramAsTime("master_timeout", putWatchRequest.masterNodeTimeout()));
        putWatchRequest.setActive(request.paramAsBoolean("active", putWatchRequest.isActive()));
        client.putWatch(putWatchRequest, new RestBuilderListener<PutWatchResponse>(channel) {
            @Override
            public RestResponse buildResponse(PutWatchResponse response, XContentBuilder builder) throws Exception {
                builder.startObject()
                        .field("_id", response.getId())
                        .field("_version", response.getVersion())
                        .field("created", response.isCreated())
                        .endObject();
                RestStatus status = response.isCreated() ? CREATED : OK;
                return new BytesRestResponse(status, builder);
            }
        });
    }
}
