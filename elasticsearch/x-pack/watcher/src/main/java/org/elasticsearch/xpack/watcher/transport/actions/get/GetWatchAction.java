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

package org.elasticsearch.xpack.watcher.transport.actions.get;

import org.elasticsearch.client.ElasticsearchClient;

/**
 * This action gets an watch by name
 */
public class GetWatchAction extends org.elasticsearch.action.Action<GetWatchRequest, GetWatchResponse, GetWatchRequestBuilder> {

    public static final GetWatchAction INSTANCE = new GetWatchAction();
    public static final String NAME = "cluster:monitor/xpack/watcher/watch/get";

    private GetWatchAction() {
        super(NAME);
    }

    @Override
    public GetWatchResponse newResponse() {
        return new GetWatchResponse();
    }

    @Override
    public GetWatchRequestBuilder newRequestBuilder(ElasticsearchClient client) {
        return new GetWatchRequestBuilder(client);
    }
}
