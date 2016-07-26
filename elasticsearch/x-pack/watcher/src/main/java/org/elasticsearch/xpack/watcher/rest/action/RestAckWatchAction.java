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
import org.elasticsearch.xpack.watcher.support.xcontent.WatcherParams;
import org.elasticsearch.xpack.watcher.transport.actions.ack.AckWatchRequest;
import org.elasticsearch.xpack.watcher.transport.actions.ack.AckWatchResponse;
import org.elasticsearch.xpack.watcher.watch.Watch;

import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.rest.RestRequest.Method.PUT;

/**
 * The rest action to ack a watch
 */
public class RestAckWatchAction extends WatcherRestHandler {

    @Inject
    public RestAckWatchAction(Settings settings, RestController controller) {
        super(settings);
        // @deprecated Remove deprecations in 6.0
        controller.registerWithDeprecatedHandler(POST, URI_BASE + "/watch/{id}/_ack", this,
                                                 POST, "/_watcher/watch/{id}/_ack", deprecationLogger);
        controller.registerWithDeprecatedHandler(PUT, URI_BASE + "/watch/{id}/_ack", this,
                                                 PUT, "/_watcher/watch/{id}/_ack", deprecationLogger);
        controller.registerWithDeprecatedHandler(POST, URI_BASE + "/watch/{id}/_ack/{actions}", this,
                                                 POST, "/_watcher/watch/{id}/_ack/{actions}", deprecationLogger);
        controller.registerWithDeprecatedHandler(PUT, URI_BASE + "/watch/{id}/_ack/{actions}", this,
                                                 PUT, "/_watcher/watch/{id}/_ack/{actions}", deprecationLogger);

        // @deprecated The following can be totally dropped in 6.0
        // Note: we deprecated "/{actions}/_ack" totally; so we don't replace it with a matching _xpack variant
        controller.registerAsDeprecatedHandler(POST, "/_watcher/watch/{id}/{actions}/_ack", this,
                                               "[POST /_watcher/watch/{id}/{actions}/_ack] is deprecated! Use " +
                                               "[POST /_xpack/watcher/watch/{id}/_ack/{actions}] instead.",
                                               deprecationLogger);
        controller.registerAsDeprecatedHandler(PUT, "/_watcher/watch/{id}/{actions}/_ack", this,
                                               "[PUT /_watcher/watch/{id}/{actions}/_ack] is deprecated! Use " +
                                               "[PUT /_xpack/watcher/watch/{id}/_ack/{actions}] instead.",
                                               deprecationLogger);
    }

    @Override
    public void handleRequest(RestRequest request, RestChannel restChannel, WatcherClient client) throws Exception {
        AckWatchRequest ackWatchRequest = new AckWatchRequest(request.param("id"));
        String[] actions = request.paramAsStringArray("actions", null);
        if (actions != null) {
            ackWatchRequest.setActionIds(actions);
        }
        ackWatchRequest.masterNodeTimeout(request.paramAsTime("master_timeout", ackWatchRequest.masterNodeTimeout()));
        client.ackWatch(ackWatchRequest, new RestBuilderListener<AckWatchResponse>(restChannel) {
            @Override
            public RestResponse buildResponse(AckWatchResponse response, XContentBuilder builder) throws Exception {
                return new BytesRestResponse(RestStatus.OK, builder.startObject()
                        .field(Watch.Field.STATUS.getPreferredName(), response.getStatus(), WatcherParams.HIDE_SECRETS)
                        .endObject());

            }
        });
    }
    
}
