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

package org.elasticsearch.alerts.transport.actions.service;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchIllegalArgumentException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.master.TransportMasterNodeOperationAction;
import org.elasticsearch.alerts.AlertManager;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

/**
 */
public class TransportAlertsServiceAction extends TransportMasterNodeOperationAction<AlertsServiceRequest, AlertsServiceResponse> {

    private final AlertManager alertManager;

    @Inject
    public TransportAlertsServiceAction(Settings settings, String actionName, TransportService transportService, ClusterService clusterService, ThreadPool threadPool, ActionFilters actionFilters, AlertManager alertManager) {
        super(settings, actionName, transportService, clusterService, threadPool, actionFilters);
        this.alertManager = alertManager;
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.MANAGEMENT;
    }

    @Override
    protected AlertsServiceRequest newRequest() {
        return new AlertsServiceRequest();
    }

    @Override
    protected AlertsServiceResponse newResponse() {
        return new AlertsServiceResponse();
    }

    @Override
    protected void masterOperation(AlertsServiceRequest request, ClusterState state, ActionListener<AlertsServiceResponse> listener) throws ElasticsearchException {
        switch (request.getCommand()) {
            case "start":
                alertManager.start();
                break;
            case "stop":
                alertManager.stop();
                break;
            case "restart":
                alertManager.start();
                alertManager.stop();
                break;
            default:
                listener.onFailure(new ElasticsearchIllegalArgumentException("Command [" + request.getCommand() + "] is undefined"));
                return;
        }
        listener.onResponse(new AlertsServiceResponse(true));
    }

    @Override
    protected ClusterBlockException checkBlock(AlertsServiceRequest request, ClusterState state) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA);
    }
}
