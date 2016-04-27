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

package org.elasticsearch.xpack.watcher.transport.actions.stats;

import org.elasticsearch.action.support.master.MasterNodeReadOperationRequestBuilder;
import org.elasticsearch.client.ElasticsearchClient;

/**
 * Watcher stats request builder.
 */
public class WatcherStatsRequestBuilder extends MasterNodeReadOperationRequestBuilder<WatcherStatsRequest, WatcherStatsResponse,
        WatcherStatsRequestBuilder> {

    public WatcherStatsRequestBuilder(ElasticsearchClient client) {
        super(client, WatcherStatsAction.INSTANCE, new WatcherStatsRequest());
    }

    public WatcherStatsRequestBuilder setIncludeCurrentWatches(boolean includeCurrentWatches) {
        request().includeCurrentWatches(includeCurrentWatches);
        return this;
    }

    public WatcherStatsRequestBuilder setIncludeQueuedWatches(boolean includeQueuedWatches) {
        request().includeQueuedWatches(includeQueuedWatches);
        return this;
    }
}
