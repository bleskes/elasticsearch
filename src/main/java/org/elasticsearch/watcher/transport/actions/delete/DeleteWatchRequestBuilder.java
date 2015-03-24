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

package org.elasticsearch.watcher.transport.actions.delete;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.master.MasterNodeOperationRequestBuilder;
import org.elasticsearch.watcher.client.WatcherClient;
import org.elasticsearch.client.Client;

/**
 * A delete document action request builder.
 */
public class DeleteWatchRequestBuilder extends MasterNodeOperationRequestBuilder<DeleteWatchRequest, DeleteWatchResponse, DeleteWatchRequestBuilder, Client> {

    public DeleteWatchRequestBuilder(Client client) {
        super(client, new DeleteWatchRequest());
    }

    public DeleteWatchRequestBuilder(Client client, String watchName) {
        super(client, new DeleteWatchRequest(watchName));
    }

    /**
     * Sets the name of the watch to be deleted
     */
    public DeleteWatchRequestBuilder setWatchName(String watchName) {
        this.request().setWatchName(watchName);
        return this;
    }

    @Override
    protected void doExecute(final ActionListener<DeleteWatchResponse> listener) {
        new WatcherClient(client).deleteWatch(request, listener);
    }

}
