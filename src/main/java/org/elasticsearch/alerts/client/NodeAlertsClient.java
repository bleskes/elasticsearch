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

package org.elasticsearch.alerts.client;

import org.elasticsearch.action.*;
import org.elasticsearch.action.support.TransportAction;
import org.elasticsearch.alerts.transport.actions.ack.*;
import org.elasticsearch.alerts.transport.actions.delete.*;
import org.elasticsearch.alerts.transport.actions.get.*;
import org.elasticsearch.alerts.transport.actions.put.*;
import org.elasticsearch.alerts.transport.actions.stats.*;
import org.elasticsearch.client.support.Headers;
import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.threadpool.ThreadPool;

public class NodeAlertsClient implements AlertsClient {

    private final Headers headers;
    private final ThreadPool threadPool;
    private final ImmutableMap<GenericAction, TransportAction> internalActions;

    @Inject
    public NodeAlertsClient(ThreadPool threadPool, Headers headers, TransportPutAlertAction transportPutAlertAction,
                            TransportGetAlertAction transportGetAlertAction, TransportDeleteAlertAction transportDeleteAlertAction,
                            TransportAlertStatsAction transportAlertStatsAction, TransportAckAlertAction transportAckAlertAction) {
        this.headers = headers;
        this.threadPool = threadPool;
        internalActions = ImmutableMap.<GenericAction, TransportAction>builder()
                .put(PutAlertAction.INSTANCE, transportPutAlertAction)
                .put(GetAlertAction.INSTANCE, transportGetAlertAction)
                .put(DeleteAlertAction.INSTANCE, transportDeleteAlertAction)
                .put(AlertsStatsAction.INSTANCE, transportAlertStatsAction)
                .put(AckAlertAction.INSTANCE, transportAckAlertAction)
                .build();
    }

    @Override
    public GetAlertRequestBuilder prepareGetAlert(String alertName) {
        return new GetAlertRequestBuilder(this, alertName);
    }

    @Override
    public GetAlertRequestBuilder prepareGetAlert() {
        return new GetAlertRequestBuilder(this);
    }

    public void getAlert(GetAlertRequest request, ActionListener<GetAlertResponse> response){
        execute(GetAlertAction.INSTANCE, request, response);
    }

    @Override
    public ActionFuture<GetAlertResponse> getAlert(GetAlertRequest request) {
        return execute(GetAlertAction.INSTANCE, request);
    }

    @Override
    public DeleteAlertRequestBuilder prepareDeleteAlert(String alertName) {
        return new DeleteAlertRequestBuilder(this, alertName);
    }

    @Override
    public DeleteAlertRequestBuilder prepareDeleteAlert() {
        return new DeleteAlertRequestBuilder(this);
    }

    @Override
    public void deleteAlert(DeleteAlertRequest request, ActionListener<DeleteAlertResponse> response) {
        execute(DeleteAlertAction.INSTANCE, request, response);
    }

    @Override
    public ActionFuture<DeleteAlertResponse> deleteAlert(DeleteAlertRequest request) {
        return execute(DeleteAlertAction.INSTANCE, request);
    }

    @Override
    public PutAlertRequestBuilder prepareIndexAlert(String alertName) {
        return new PutAlertRequestBuilder(this, alertName);
    }

    @Override
    public PutAlertRequestBuilder prepareIndexAlert() {
        return new PutAlertRequestBuilder(this, null);
    }

    @Override
    public void indexAlert(PutAlertRequest request, ActionListener<PutAlertResponse> response) {
        execute(PutAlertAction.INSTANCE, request, response);
    }

    @Override
    public ActionFuture<PutAlertResponse> indexAlert(PutAlertRequest request) {
        return execute(PutAlertAction.INSTANCE, request);
    }

    @Override
    public ActionFuture<AlertsStatsResponse> alertsStats(AlertsStatsRequest request) {
        return execute(AlertsStatsAction.INSTANCE, request);
    }

    @Override
    public AlertsStatsRequestBuilder prepareAlertsStats() {
        return new AlertsStatsRequestBuilder(this);
    }

    @Override
    public void alertsStats(AlertsStatsRequest request, ActionListener<AlertsStatsResponse> listener) {
        execute(AlertsStatsAction.INSTANCE, request, listener);
    }

    @Override
    public AckAlertRequestBuilder prepareAckAlert(String alertName) {
        return new AckAlertRequestBuilder(this, alertName);
    }

    @Override
    public AckAlertRequestBuilder prepareAckAlert() {
        return new AckAlertRequestBuilder(this);
    }

    @Override
    public void ackAlert(AckAlertRequest request, ActionListener<AckAlertResponse> listener) {
        execute(AckAlertAction.INSTANCE, request, listener);
    }

    @Override
    public ActionFuture<AckAlertResponse> ackAlert(AckAlertRequest request) {
        return execute(AckAlertAction.INSTANCE, request);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <Request extends ActionRequest, Response extends ActionResponse, RequestBuilder extends ActionRequestBuilder<Request, Response, RequestBuilder, AlertsClient>> ActionFuture<Response> execute(Action<Request, Response, RequestBuilder, AlertsClient> action, Request request) {
        headers.applyTo(request);
        TransportAction<Request, Response> transportAction = internalActions.get((AlertsClientAction)action);
        return transportAction.execute(request);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <Request extends ActionRequest, Response extends ActionResponse, RequestBuilder extends ActionRequestBuilder<Request, Response, RequestBuilder, AlertsClient>> void execute(Action<Request, Response, RequestBuilder, AlertsClient> action, Request request, ActionListener<Response> listener) {
        headers.applyTo(request);
        TransportAction<Request, Response> transportAction = internalActions.get((AlertsClientAction)action);
        transportAction.execute(request, listener);
    }

    @Override
    public <Request extends ActionRequest, Response extends ActionResponse, RequestBuilder extends ActionRequestBuilder<Request, Response, RequestBuilder, AlertsClient>> RequestBuilder prepareExecute(Action<Request, Response, RequestBuilder, AlertsClient> action) {
        return action.newRequestBuilder(this);
    }

    @Override
    public ThreadPool threadPool() {
        return threadPool;
    }
}
