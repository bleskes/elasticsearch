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

package org.elasticsearch.xpack.watcher.transport.actions.ack;

import org.elasticsearch.action.Action;
import org.elasticsearch.client.ElasticsearchClient;

/**
 * This action acks a watch in memory, and the index
 */
public class AckWatchAction extends Action<AckWatchRequest, AckWatchResponse, AckWatchRequestBuilder> {

    public static final AckWatchAction INSTANCE = new AckWatchAction();
    public static final String NAME = "cluster:admin/xpack/watcher/watch/ack";

    private AckWatchAction() {
        super(NAME);
    }

    @Override
    public AckWatchResponse newResponse() {
        return new AckWatchResponse();
    }

    @Override
    public AckWatchRequestBuilder newRequestBuilder(ElasticsearchClient client) {
        return new AckWatchRequestBuilder(client);
    }
}
