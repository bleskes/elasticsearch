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
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.rest.*;
import org.elasticsearch.watcher.client.WatcherClient;
import org.elasticsearch.watcher.rest.WatcherRestHandler;
import org.elasticsearch.watcher.watch.WatchStore;

/**
  */
public class RestHijackOperationAction extends WatcherRestHandler {
    private static String ALLOW_DIRECT_ACCESS_TO_WATCH_INDEX_SETTING = "watcher.index.rest.direct_access";


    @Inject
    public RestHijackOperationAction(Settings settings, RestController controller, Client client) {
        super(settings, controller, client);
        if (!settings.getAsBoolean(ALLOW_DIRECT_ACCESS_TO_WATCH_INDEX_SETTING, false)) {
            WatcherRestHandler unsupportedHandler = new UnsupportedHandler(settings, controller, client);
            controller.registerHandler(RestRequest.Method.POST, WatchStore.INDEX + "/watch", this);
            controller.registerHandler(RestRequest.Method.POST, WatchStore.INDEX + "/watch/{id}", this);
            controller.registerHandler(RestRequest.Method.PUT, WatchStore.INDEX + "/watch/{id}", this);
            controller.registerHandler(RestRequest.Method.POST, WatchStore.INDEX + "/watch/{id}/_update", this);
            controller.registerHandler(RestRequest.Method.DELETE, WatchStore.INDEX + "/watch/_query", this);
            controller.registerHandler(RestRequest.Method.DELETE, WatchStore.INDEX + "/watch/{id}", this);
            controller.registerHandler(RestRequest.Method.GET, WatchStore.INDEX + "/watch/{id}", this);
            controller.registerHandler(RestRequest.Method.POST, WatchStore.INDEX + "/watch/_bulk", unsupportedHandler);
            controller.registerHandler(RestRequest.Method.POST, WatchStore.INDEX + "/_bulk", unsupportedHandler);
            controller.registerHandler(RestRequest.Method.PUT, WatchStore.INDEX + "/watch/_bulk", unsupportedHandler);
            controller.registerHandler(RestRequest.Method.PUT, WatchStore.INDEX + "/_bulk", unsupportedHandler);
        }
    }

    @Override
    protected void handleRequest(RestRequest request, RestChannel channel, WatcherClient client) throws Exception {
        XContentBuilder jsonBuilder = XContentFactory.jsonBuilder();
        jsonBuilder.startObject().field("error","This endpoint is not supported for " +
                request.method().name() + " on " + WatchStore.INDEX + " index. Please use " +
                request.method().name() + " " + URI_BASE + "/watch/<watch_id> instead");
        jsonBuilder.field("status", RestStatus.BAD_REQUEST.getStatus());
        jsonBuilder.endObject();
        channel.sendResponse(new BytesRestResponse(RestStatus.BAD_REQUEST, jsonBuilder));
    }

    public static class UnsupportedHandler extends WatcherRestHandler{

        public UnsupportedHandler(Settings settings, RestController controller, Client client) {
            super(settings, controller, client);
        }

        @Override
        protected void handleRequest(RestRequest request, RestChannel channel, WatcherClient client) throws Exception {
            request.path();
            XContentBuilder jsonBuilder = XContentFactory.jsonBuilder();
            jsonBuilder.startObject().field("error","This endpoint is not supported for " +
                    request.method().name() + " on " + WatchStore.INDEX + " index.");
            jsonBuilder.field("status", RestStatus.BAD_REQUEST.getStatus());
            jsonBuilder.endObject();
            channel.sendResponse(new BytesRestResponse(RestStatus.BAD_REQUEST, jsonBuilder));
        }
    }
}
