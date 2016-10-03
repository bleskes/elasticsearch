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

package org.elasticsearch.xpack.watcher.transport.actions.activate;

import org.elasticsearch.action.Action;
import org.elasticsearch.client.ElasticsearchClient;

/**
 * This action acks a watch in memory, and the index
 */
public class ActivateWatchAction extends Action<ActivateWatchRequest, ActivateWatchResponse, ActivateWatchRequestBuilder> {

    public static final ActivateWatchAction INSTANCE = new ActivateWatchAction();
    public static final String NAME = "cluster:admin/xpack/watcher/watch/activate";

    private ActivateWatchAction() {
        super(NAME);
    }

    @Override
    public ActivateWatchResponse newResponse() {
        return new ActivateWatchResponse();
    }

    @Override
    public ActivateWatchRequestBuilder newRequestBuilder(ElasticsearchClient client) {
        return new ActivateWatchRequestBuilder(client);
    }
}
