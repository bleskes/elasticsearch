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

package org.elasticsearch.watcher.transport.actions.ack;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.master.MasterNodeOperationRequestBuilder;
import org.elasticsearch.watcher.client.WatcherClient;
import org.elasticsearch.client.Client;

/**
 * A ack watch action request builder.
 */
public class AckWatchRequestBuilder extends MasterNodeOperationRequestBuilder<AckWatchRequest, AckWatchResponse, AckWatchRequestBuilder, Client> {

    public AckWatchRequestBuilder(Client client) {
        super(client, new AckWatchRequest());
    }

    public AckWatchRequestBuilder(Client client, String id) {
        super(client, new AckWatchRequest(id));
    }

    public AckWatchRequestBuilder setActionIds(String... actionIds) {
        request.setActionIds(actionIds);
        return this;
    }

    @Override
    protected void doExecute(final ActionListener<AckWatchResponse> listener) {
        new WatcherClient(client).ackWatch(request, listener);
    }

}
