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

import org.elasticsearch.action.support.master.MasterNodeOperationRequestBuilder;
import org.elasticsearch.client.ElasticsearchClient;

/**
 * A ack watch action request builder.
 */
public class AckWatchRequestBuilder extends MasterNodeOperationRequestBuilder<AckWatchRequest, AckWatchResponse, AckWatchRequestBuilder> {

    public AckWatchRequestBuilder(ElasticsearchClient client) {
        super(client, AckWatchAction.INSTANCE, new AckWatchRequest());
    }

    public AckWatchRequestBuilder(ElasticsearchClient client, String id) {
        super(client, AckWatchAction.INSTANCE, new AckWatchRequest(id));
    }

    public AckWatchRequestBuilder setActionIds(String... actionIds) {
        request.setActionIds(actionIds);
        return this;
    }


}
