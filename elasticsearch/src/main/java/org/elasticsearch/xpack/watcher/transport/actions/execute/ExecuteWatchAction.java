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

package org.elasticsearch.xpack.watcher.transport.actions.execute;

import org.elasticsearch.action.Action;
import org.elasticsearch.client.ElasticsearchClient;

/**
 * This action executes a watch, either ignoring the schedule and condition or just the schedule and can execute a subset of the actions,
 * optionally persisting the history entry
 */
public class ExecuteWatchAction extends Action<ExecuteWatchRequest, ExecuteWatchResponse, ExecuteWatchRequestBuilder> {

    public static final ExecuteWatchAction INSTANCE = new ExecuteWatchAction();
    public static final String NAME = "cluster:admin/xpack/watcher/watch/execute";

    private ExecuteWatchAction() {
        super(NAME);
    }

    @Override
    public ExecuteWatchResponse newResponse() {
        return new ExecuteWatchResponse();
    }

    @Override
    public ExecuteWatchRequestBuilder newRequestBuilder(ElasticsearchClient client) {
        return new ExecuteWatchRequestBuilder(client);
    }

}
