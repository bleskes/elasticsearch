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
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.xpack.watcher.client.WatcherClient;
import org.elasticsearch.xpack.watcher.rest.WatcherRestHandler;
import org.elasticsearch.xpack.watcher.watch.WatchStore;

import static org.elasticsearch.rest.RestRequest.Method.DELETE;
import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.rest.RestRequest.Method.PUT;

/**
  */
public class RestHijackOperationAction extends WatcherRestHandler {

    private static final String ALLOW_DIRECT_ACCESS_TO_WATCH_INDEX_SETTING = "xpack.watcher.index.rest.direct_access";

    @Inject
    public RestHijackOperationAction(Settings settings, RestController controller) {
        super(settings);
        if (!settings.getAsBoolean(ALLOW_DIRECT_ACCESS_TO_WATCH_INDEX_SETTING, false)) {
            WatcherRestHandler unsupportedHandler = new UnsupportedHandler(settings);
            controller.registerHandler(POST, WatchStore.INDEX + "/watch", this);
            controller.registerHandler(POST, WatchStore.INDEX + "/watch/{id}", this);
            controller.registerHandler(PUT, WatchStore.INDEX + "/watch/{id}", this);
            controller.registerHandler(POST, WatchStore.INDEX + "/watch/{id}/_update", this);
            controller.registerHandler(DELETE, WatchStore.INDEX + "/watch/_query", this);
            controller.registerHandler(DELETE, WatchStore.INDEX + "/watch/{id}", this);
            controller.registerHandler(GET, WatchStore.INDEX + "/watch/{id}", this);
            controller.registerHandler(POST, WatchStore.INDEX + "/watch/_bulk", unsupportedHandler);
            controller.registerHandler(POST, WatchStore.INDEX + "/_bulk", unsupportedHandler);
            controller.registerHandler(PUT, WatchStore.INDEX + "/watch/_bulk", unsupportedHandler);
            controller.registerHandler(PUT, WatchStore.INDEX + "/_bulk", unsupportedHandler);
            controller.registerHandler(DELETE, WatchStore.INDEX, unsupportedHandler);
        }
    }

    @Override
    public void handleRequest(RestRequest request, RestChannel channel, WatcherClient client) throws Exception {
        XContentBuilder jsonBuilder = XContentFactory.jsonBuilder();
        jsonBuilder.startObject().field("error","This endpoint is not supported for " +
                request.method().name() + " on " + WatchStore.INDEX + " index. Please use " +
                request.method().name() + " " + URI_BASE + "/watch/<watch_id> instead");
        jsonBuilder.field("status", RestStatus.BAD_REQUEST.getStatus());
        jsonBuilder.endObject();
        channel.sendResponse(new BytesRestResponse(RestStatus.BAD_REQUEST, jsonBuilder));
    }

    public static class UnsupportedHandler extends WatcherRestHandler {

        public UnsupportedHandler(Settings settings) {
            super(settings);
        }

        @Override
        public void handleRequest(RestRequest request, RestChannel channel, WatcherClient client) throws Exception {
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
