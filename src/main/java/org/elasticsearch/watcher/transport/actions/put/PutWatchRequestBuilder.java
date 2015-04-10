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

package org.elasticsearch.watcher.transport.actions.put;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.master.MasterNodeOperationRequestBuilder;
import org.elasticsearch.watcher.client.WatchSourceBuilder;
import org.elasticsearch.watcher.client.WatcherClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.bytes.BytesReference;

public class PutWatchRequestBuilder extends MasterNodeOperationRequestBuilder<PutWatchRequest, PutWatchResponse, PutWatchRequestBuilder, Client> {

    public PutWatchRequestBuilder(Client client) {
        super(client, new PutWatchRequest());
    }

    public PutWatchRequestBuilder(Client client, String id) {
        super(client, new PutWatchRequest());
        request.setId(id);
    }

    /**
     * @param id The watch id to be created
     */
    public PutWatchRequestBuilder setId(String id){
        request.setId(id);
        return this;
    }

    /**
     * @param source the source of the watch to be created
     */
    public PutWatchRequestBuilder setSource(BytesReference source) {
        request.setSource(source);
        return this;
    }

    /**
     * @param source the source of the watch to be created
     */
    public PutWatchRequestBuilder setSource(WatchSourceBuilder source) {
        request.setSource(source);
        return this;
    }

    @Override
    protected void doExecute(ActionListener<PutWatchResponse> listener) {
        new WatcherClient(client).putWatch(request, listener);
    }
}
