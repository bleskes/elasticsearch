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

package org.elasticsearch.xpack.watcher.transport.actions.delete;

import org.elasticsearch.action.support.master.MasterNodeOperationRequestBuilder;
import org.elasticsearch.client.ElasticsearchClient;

/**
 * A delete document action request builder.
 */
public class DeleteWatchRequestBuilder extends MasterNodeOperationRequestBuilder<DeleteWatchRequest, DeleteWatchResponse,
        DeleteWatchRequestBuilder> {

    public DeleteWatchRequestBuilder(ElasticsearchClient client) {
        super(client, DeleteWatchAction.INSTANCE, new DeleteWatchRequest());
    }

    public DeleteWatchRequestBuilder(ElasticsearchClient client, String id) {
        super(client, DeleteWatchAction.INSTANCE, new DeleteWatchRequest(id));
    }

    /**
     * Sets the id of the watch to be deleted
     */
    public DeleteWatchRequestBuilder setId(String id) {
        this.request().setId(id);
        return this;
    }

    /**
     * Sets wiether this request is forced (ie ignores locks)
     */
    public DeleteWatchRequestBuilder setForce(boolean force) {
        this.request().setForce(force);
        return this;
    }

}
