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

package org.elasticsearch.xpack.watcher.transport.actions.service;

import org.elasticsearch.action.support.master.MasterNodeOperationRequestBuilder;
import org.elasticsearch.client.ElasticsearchClient;

/**
 */
public class WatcherServiceRequestBuilder extends MasterNodeOperationRequestBuilder<WatcherServiceRequest, WatcherServiceResponse,
        WatcherServiceRequestBuilder> {

    public WatcherServiceRequestBuilder(ElasticsearchClient client) {
        super(client, WatcherServiceAction.INSTANCE, new WatcherServiceRequest());
    }

    /**
     * Starts the watcher if not already started.
     */
    public WatcherServiceRequestBuilder start() {
        request.start();
        return this;
    }

    /**
     * Stops the watcher if not already stopped.
     */
    public WatcherServiceRequestBuilder stop() {
        request.stop();
        return this;
    }

    /**
     * Starts and stops the watcher.
     */
    public WatcherServiceRequestBuilder restart() {
        request.restart();
        return this;
    }
}
