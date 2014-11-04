/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.alerts.transport.actions.get;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.master.TransportMasterNodeOperationAction;
import org.elasticsearch.alerts.Alert;
import org.elasticsearch.alerts.AlertManager;
import org.elasticsearch.alerts.AlertsStore;
import org.elasticsearch.alerts.actions.AlertActionManager;
import org.elasticsearch.alerts.transport.actions.delete.DeleteAlertRequest;
import org.elasticsearch.alerts.transport.actions.delete.DeleteAlertResponse;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

/**
 * Performs the delete operation.
 */
public class TransportGetAlertAction extends TransportMasterNodeOperationAction<GetAlertRequest,  GetAlertResponse> {

    private final AlertManager alertManager;

    @Inject
    public TransportGetAlertAction(Settings settings, String actionName, TransportService transportService,
                                   ClusterService clusterService, ThreadPool threadPool, ActionFilters actionFilters,
                                   AlertManager alertManager) {
        super(settings, actionName, transportService, clusterService, threadPool, actionFilters);
        this.alertManager = alertManager;
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.MANAGEMENT;
    }

    @Override
    protected GetAlertRequest newRequest() {
        return new GetAlertRequest();
    }

    @Override
    protected GetAlertResponse newResponse() {
        return new GetAlertResponse();
    }

    @Override
    protected void masterOperation(GetAlertRequest request, ClusterState state, ActionListener<GetAlertResponse> listener) throws ElasticsearchException {
        try {
            Alert alert = alertManager.getAlert(request.alertName());
            GetAlertResponse response = new GetAlertResponse();
            response.found(alert != null);
            response.alert(alert);
            listener.onResponse(response);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }

    @Override
    protected ClusterBlockException checkBlock(GetAlertRequest request, ClusterState state) {
        if (!alertManager.isStarted()) {
            return new ClusterBlockException(null);
        }
        return state.blocks().indicesBlockedException(ClusterBlockLevel.WRITE, new String[]{AlertsStore.ALERT_INDEX, AlertActionManager.ALERT_HISTORY_INDEX});
    }


}
