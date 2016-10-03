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

import org.elasticsearch.action.Action;
import org.elasticsearch.client.ElasticsearchClient;

/**
 * This Action gets the stats for the watcher plugin
 */
public class WatcherStatsAction extends Action<WatcherStatsRequest, WatcherStatsResponse, WatcherStatsRequestBuilder> {

    public static final WatcherStatsAction INSTANCE = new WatcherStatsAction();
    public static final String NAME = "cluster:monitor/xpack/watcher/stats";

    private WatcherStatsAction() {
        super(NAME);
    }

    @Override
    public WatcherStatsResponse newResponse() {
        return new WatcherStatsResponse();
    }

    @Override
    public WatcherStatsRequestBuilder newRequestBuilder(ElasticsearchClient client) {
        return new WatcherStatsRequestBuilder(client);
    }

}
