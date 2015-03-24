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

import org.elasticsearch.watcher.watch.WatchStore;
import org.elasticsearch.watcher.client.WatcherClient;
import org.elasticsearch.watcher.transport.actions.service.WatcherServiceRequest;
import org.elasticsearch.watcher.transport.actions.service.WatcherServiceResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.support.AcknowledgedRestListener;

/**
 */
public class RestWatchServiceAction extends BaseRestHandler {

    @Inject
    protected RestWatchServiceAction(Settings settings, RestController controller, Client client) {
        super(settings, controller, client);
        controller.registerHandler(RestRequest.Method.PUT, WatchStore.INDEX + "/_restart", this);
        controller.registerHandler(RestRequest.Method.PUT, WatchStore.INDEX + "/_start", new StartRestHandler(settings, controller, client));
        controller.registerHandler(RestRequest.Method.PUT, WatchStore.INDEX + "/_stop", new StopRestHandler(settings, controller, client));
    }

    @Override
    protected void handleRequest(RestRequest request, RestChannel channel, Client client) throws Exception {
        new WatcherClient(client).watcherService(new WatcherServiceRequest().restart(), new AcknowledgedRestListener<WatcherServiceResponse>(channel));
    }

    static class StartRestHandler extends BaseRestHandler {

        public StartRestHandler(Settings settings, RestController controller, Client client) {
            super(settings, controller, client);
        }

        @Override
        public void handleRequest(RestRequest request, RestChannel channel, Client client) throws Exception {
            new WatcherClient(client).watcherService(new WatcherServiceRequest().start(), new AcknowledgedRestListener<WatcherServiceResponse>(channel));
        }
    }

    static class StopRestHandler extends BaseRestHandler {

        public StopRestHandler(Settings settings, RestController controller, Client client) {
            super(settings, controller, client);
        }

        @Override
        public void handleRequest(RestRequest request, RestChannel channel, Client client) throws Exception {
            new WatcherClient(client).watcherService(new WatcherServiceRequest().stop(), new AcknowledgedRestListener<WatcherServiceResponse>(channel));
        }
    }
}
