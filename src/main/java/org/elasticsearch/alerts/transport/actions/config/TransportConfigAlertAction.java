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

package org.elasticsearch.alerts.transport.actions.config;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.master.TransportMasterNodeOperationAction;
import org.elasticsearch.alerts.AlertsStore;
import org.elasticsearch.alerts.ConfigurationService;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

/**
 * Performs the config operation.
 */
public class TransportConfigAlertAction extends TransportMasterNodeOperationAction<ConfigAlertRequest, ConfigAlertResponse> {

    private final ConfigurationService configManager;

    @Inject
    public TransportConfigAlertAction(Settings settings, TransportService transportService, ClusterService clusterService,
                                      ThreadPool threadPool, ActionFilters actionFilters, ConfigurationService configManager) {
        super(settings, ConfigAlertAction.NAME, transportService, clusterService, threadPool, actionFilters);
        this.configManager = configManager;
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.MANAGEMENT;
    }

    @Override
    protected ConfigAlertRequest newRequest() {
        return new ConfigAlertRequest();
    }

    @Override
    protected ConfigAlertResponse newResponse() {
        return new ConfigAlertResponse();
    }

    @Override
    protected void masterOperation(ConfigAlertRequest request, ClusterState state, ActionListener<ConfigAlertResponse> listener) throws ElasticsearchException {
        try {
            IndexResponse indexResponse = configManager.updateConfig(request.getConfigSource());
            listener.onResponse(new ConfigAlertResponse(indexResponse));
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }

    @Override
    protected ClusterBlockException checkBlock(ConfigAlertRequest request, ClusterState state) {
        request.beforeLocalFork(); // This is the best place to make the config source safe
        return state.blocks().indexBlockedException(ClusterBlockLevel.WRITE, AlertsStore.ALERT_INDEX);
    }


}
