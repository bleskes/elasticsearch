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

import org.elasticsearch.client.Client;
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
import org.elasticsearch.watcher.client.WatcherClient;
import org.elasticsearch.watcher.rest.WatcherRestHandler;
import org.elasticsearch.watcher.support.xcontent.WatcherParams;
import org.elasticsearch.watcher.transport.actions.activate.ActivateWatchRequest;
import org.elasticsearch.watcher.transport.actions.activate.ActivateWatchResponse;
import org.elasticsearch.watcher.watch.Watch;

/**
 * The rest action to de/activate a watch
 */
public class RestActivateWatchAction extends WatcherRestHandler {

    @Inject
    public RestActivateWatchAction(Settings settings, RestController controller, Client client) {
        super(settings, controller, client);
        controller.registerHandler(RestRequest.Method.PUT, URI_BASE + "/watch/{id}/_activate", this);
        controller.registerHandler(RestRequest.Method.POST, URI_BASE + "/watch/{id}/_activate", this);
        DeactivateRestHandler deactivateRestHandler = new DeactivateRestHandler(settings, controller, client);
        controller.registerHandler(RestRequest.Method.PUT, URI_BASE + "/watch/{id}/_deactivate", deactivateRestHandler);
        controller.registerHandler(RestRequest.Method.POST, URI_BASE + "/watch/{id}/_deactivate", deactivateRestHandler);
    }

    @Override
    protected void handleRequest(RestRequest request, RestChannel channel, WatcherClient client) throws Exception {
        String watchId = request.param("id");
        client.activateWatch(new ActivateWatchRequest(watchId, true), new RestBuilderListener<ActivateWatchResponse>(channel) {
            @Override
            public RestResponse buildResponse(ActivateWatchResponse response, XContentBuilder builder) throws Exception {
                return new BytesRestResponse(RestStatus.OK, builder.startObject()
                        .field(Watch.Field.STATUS.getPreferredName(), response.getStatus(), WatcherParams.HIDE_SECRETS)
                        .endObject());
            }
        });
    }

    static class DeactivateRestHandler extends WatcherRestHandler {

        public DeactivateRestHandler(Settings settings, RestController controller, Client client) {
            super(settings, controller, client);
        }

        @Override
        protected void handleRequest(RestRequest request, RestChannel channel, WatcherClient client) throws Exception {
            String watchId = request.param("id");
            client.activateWatch(new ActivateWatchRequest(watchId, false), new RestBuilderListener<ActivateWatchResponse>(channel) {
                @Override
                public RestResponse buildResponse(ActivateWatchResponse response, XContentBuilder builder) throws Exception {
                    return new BytesRestResponse(RestStatus.OK, builder.startObject()
                            .field(Watch.Field.STATUS.getPreferredName(), response.getStatus(), WatcherParams.HIDE_SECRETS)
                            .endObject());
                }
            });
        }
    }

}
