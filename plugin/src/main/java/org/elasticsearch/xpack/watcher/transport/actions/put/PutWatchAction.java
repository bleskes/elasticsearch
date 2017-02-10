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

package org.elasticsearch.xpack.watcher.transport.actions.put;

import org.elasticsearch.action.Action;
import org.elasticsearch.client.ElasticsearchClient;

/**
 * This action puts an watch into the watch index and adds it to the scheduler
 */
public class PutWatchAction extends Action<PutWatchRequest, PutWatchResponse, PutWatchRequestBuilder> {

    public static final PutWatchAction INSTANCE = new PutWatchAction();
    public static final String NAME = "cluster:admin/xpack/watcher/watch/put";

    private PutWatchAction() {
        super(NAME);
    }

    @Override
    public PutWatchResponse newResponse() {
        return new PutWatchResponse();
    }

    @Override
    public PutWatchRequestBuilder newRequestBuilder(ElasticsearchClient client) {
        return new PutWatchRequestBuilder(client);
    }
}
