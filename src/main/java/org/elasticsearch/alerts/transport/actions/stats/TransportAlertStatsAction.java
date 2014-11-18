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

package org.elasticsearch.alerts.transport.actions.stats;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.master.TransportMasterNodeOperationAction;
import org.elasticsearch.alerts.AlertManager;
import org.elasticsearch.alerts.actions.AlertActionManager;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

/**
 * Performs the stats operation.
 */
public class TransportAlertStatsAction extends TransportMasterNodeOperationAction<AlertsStatsRequest, AlertsStatsResponse> {

    private final AlertManager alertManager;
    private final AlertActionManager alertActionManager;

    @Inject
    public TransportAlertStatsAction(Settings settings, TransportService transportService, ClusterService clusterService,
                                     ThreadPool threadPool, ActionFilters actionFilters, AlertManager alertManager,
                                     AlertActionManager alertActionManager) {
        super(settings, AlertsStatsAction.NAME, transportService, clusterService, threadPool, actionFilters);
        this.alertManager = alertManager;
        this.alertActionManager = alertActionManager;
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.MANAGEMENT;
    }

    @Override
    protected AlertsStatsRequest newRequest() {
        return new AlertsStatsRequest();
    }

    @Override
    protected AlertsStatsResponse newResponse() {
        return new AlertsStatsResponse();
    }

    @Override
    protected void masterOperation(AlertsStatsRequest request, ClusterState state, ActionListener<AlertsStatsResponse> listener) throws ElasticsearchException {
        AlertsStatsResponse statsResponse = new AlertsStatsResponse();
        statsResponse.setAlertManagerStarted(alertManager.isStarted());
        statsResponse.setAlertActionManagerStarted(alertActionManager.started());
        statsResponse.setAlertActionManagerQueueSize(alertActionManager.getQueueSize());
        statsResponse.setNumberOfRegisteredAlerts(alertManager.getNumberOfAlerts());
        statsResponse.setAlertActionManagerLargestQueueSize(alertActionManager.getLargestQueueSize());
        listener.onResponse(statsResponse);
    }

    @Override
    protected ClusterBlockException checkBlock(AlertsStatsRequest request, ClusterState state) {
        return null;
    }


}
