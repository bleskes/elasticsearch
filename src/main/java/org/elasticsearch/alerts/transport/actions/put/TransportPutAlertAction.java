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

package org.elasticsearch.alerts.transport.actions.put;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.action.support.master.TransportMasterNodeOperationAction;
import org.elasticsearch.alerts.AlertManager;
import org.elasticsearch.alerts.AlertsStore;
import org.elasticsearch.alerts.actions.AlertActionManager;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 */
public class TransportPutAlertAction extends TransportMasterNodeOperationAction<PutAlertRequest, PutAlertResponse> {

    private final AlertManager alertManager;

    @Inject
    public TransportPutAlertAction(Settings settings, TransportService transportService, ClusterService clusterService,
                                   ThreadPool threadPool, ActionFilters actionFilters, AlertManager alertManager) {
        super(settings, PutAlertAction.NAME, transportService, clusterService, threadPool, actionFilters);
        this.alertManager = alertManager;
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.MANAGEMENT;
    }

    @Override
    protected PutAlertRequest newRequest() {
        return new PutAlertRequest();
    }

    @Override
    protected PutAlertResponse newResponse() {
        return new PutAlertResponse();
    }

    @Override
    protected void masterOperation(PutAlertRequest request, ClusterState state, ActionListener<PutAlertResponse> listener) throws ElasticsearchException {
        try {
            IndexResponse indexResponse = alertManager.putAlert(request.getAlertName(), request.getAlertSource());
            listener.onResponse(new PutAlertResponse(indexResponse));
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }

    @Override
    protected ClusterBlockException checkBlock(PutAlertRequest request, ClusterState state) {
        request.beforeLocalFork(); // This is the best place to make the alert source safe
        String[] indices = state.metaData().concreteIndices(IndicesOptions.lenientExpandOpen(), AlertActionManager.ALERT_HISTORY_INDEX_PREFIX + "*");
        List<String> indicesToCheck = new ArrayList<>(Arrays.asList(indices));
        indicesToCheck.add(AlertsStore.ALERT_INDEX);
        return state.blocks().indicesBlockedException(ClusterBlockLevel.WRITE, indicesToCheck.toArray(new String[indicesToCheck.size()]));

    }

}
