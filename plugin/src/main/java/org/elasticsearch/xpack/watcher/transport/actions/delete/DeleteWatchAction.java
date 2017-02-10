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

import org.elasticsearch.action.Action;
import org.elasticsearch.client.ElasticsearchClient;

/**
 * This action deletes an watch from in memory, the scheduler and the index
 */
public class DeleteWatchAction extends Action<DeleteWatchRequest, DeleteWatchResponse, DeleteWatchRequestBuilder> {

    public static final DeleteWatchAction INSTANCE = new DeleteWatchAction();
    public static final String NAME = "cluster:admin/xpack/watcher/watch/delete";

    private DeleteWatchAction() {
        super(NAME);
    }

    @Override
    public DeleteWatchResponse newResponse() {
        return new DeleteWatchResponse();
    }

    @Override
    public DeleteWatchRequestBuilder newRequestBuilder(ElasticsearchClient client) {
        return new DeleteWatchRequestBuilder(client);
    }
}
