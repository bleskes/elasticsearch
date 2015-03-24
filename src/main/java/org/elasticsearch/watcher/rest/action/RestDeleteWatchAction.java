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

import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.*;
import org.elasticsearch.rest.action.support.RestBuilderListener;
import org.elasticsearch.watcher.client.WatcherClient;
import org.elasticsearch.watcher.rest.WatcherRestHandler;
import org.elasticsearch.watcher.transport.actions.delete.DeleteWatchRequest;
import org.elasticsearch.watcher.transport.actions.delete.DeleteWatchResponse;

import static org.elasticsearch.rest.RestRequest.Method.DELETE;
import static org.elasticsearch.rest.RestStatus.NOT_FOUND;
import static org.elasticsearch.rest.RestStatus.OK;

/**
 */
public class RestDeleteWatchAction extends WatcherRestHandler {

    @Inject
    public RestDeleteWatchAction(Settings settings, RestController controller, Client client) {
        super(settings, controller, client);
        controller.registerHandler(DELETE, URI_BASE + "/watch/{name}", this);
    }

    @Override
    protected void handleRequest(RestRequest request, RestChannel channel, WatcherClient client) throws Exception {
        DeleteWatchRequest indexWatchRequest = new DeleteWatchRequest(request.param("name"));
        client.deleteWatch(indexWatchRequest, new RestBuilderListener<DeleteWatchResponse>(channel) {
            @Override
            public RestResponse buildResponse(DeleteWatchResponse result, XContentBuilder builder) throws Exception {
                DeleteResponse deleteResponse = result.deleteResponse();
                builder.startObject()
                        .field("found", deleteResponse.isFound())
                        .field("_id", deleteResponse.getId())
                        .field("_version", deleteResponse.getVersion())
                        .endObject();
                RestStatus status = deleteResponse.isFound() ? OK : NOT_FOUND;
                return new BytesRestResponse(status, builder);
            }
        });
    }
}
