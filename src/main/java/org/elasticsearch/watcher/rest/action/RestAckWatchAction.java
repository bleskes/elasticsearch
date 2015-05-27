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

package org.elasticsearch.watcher.rest.action;

import org.elasticsearch.watcher.rest.WatcherRestHandler;
import org.elasticsearch.watcher.support.xcontent.WatcherParams;
import org.elasticsearch.watcher.watch.Watch;
import org.elasticsearch.watcher.client.WatcherClient;
import org.elasticsearch.watcher.transport.actions.ack.AckWatchRequest;
import org.elasticsearch.watcher.transport.actions.ack.AckWatchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.*;
import org.elasticsearch.rest.action.support.RestBuilderListener;

/**
 * The rest action to ack a watch
 */
public class RestAckWatchAction extends WatcherRestHandler {

    @Inject
    protected RestAckWatchAction(Settings settings, RestController controller, Client client) {
        super(settings, controller, client);
        controller.registerHandler(RestRequest.Method.PUT, URI_BASE + "/watch/{id}/_ack", this);
        controller.registerHandler(RestRequest.Method.POST, URI_BASE + "/watch/{id}/_ack", this);
        controller.registerHandler(RestRequest.Method.PUT, URI_BASE + "/watch/{id}/{actions}/_ack", this);
        controller.registerHandler(RestRequest.Method.POST, URI_BASE + "/watch/{id}/{actions}/_ack", this);
    }

    @Override
    protected void handleRequest(RestRequest request, RestChannel restChannel, WatcherClient client) throws Exception {
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
