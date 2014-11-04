package org.elasticsearch.alerts.transport.actions.update;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.support.master.MasterNodeOperationRequestBuilder;
import org.elasticsearch.alerts.Alert;
import org.elasticsearch.alerts.client.AlertsClient;
import org.elasticsearch.alerts.client.AlertsClientInterface;
import org.elasticsearch.client.Client;

/**
 */
public class UpdateAlertRequestBuilder extends MasterNodeOperationRequestBuilder
        <UpdateAlertRequest, UpdateAlertResponse, UpdateAlertRequestBuilder, AlertsClientInterface> {


    public UpdateAlertRequestBuilder(AlertsClientInterface client) {
        super(client, new UpdateAlertRequest());
    }

    public UpdateAlertRequestBuilder(AlertsClientInterface client, Alert alert) {
        super(client, new UpdateAlertRequest(alert));
    }

    @Override
    protected void doExecute(ActionListener<UpdateAlertResponse> listener) {
        client.updateAlert(request, listener);
    }
}
