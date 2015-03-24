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

package org.elasticsearch.watcher.transport.actions.stats;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.master.MasterNodeOperationRequestBuilder;
import org.elasticsearch.watcher.client.WatcherClient;
import org.elasticsearch.client.Client;

/**
 * Watcher stats request builder.
 */
public class WatcherStatsRequestBuilder extends MasterNodeOperationRequestBuilder<WatcherStatsRequest, WatcherStatsResponse, WatcherStatsRequestBuilder, Client> {

    public WatcherStatsRequestBuilder(Client client) {
        super(client, new WatcherStatsRequest());
    }


    @Override
    protected void doExecute(final ActionListener<WatcherStatsResponse> listener) {
        new WatcherClient(client).watcherStats(request, listener);
    }

}
