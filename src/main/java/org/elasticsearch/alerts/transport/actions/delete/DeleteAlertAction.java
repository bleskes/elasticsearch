package org.elasticsearch.alerts.transport.actions.delete;

import org.elasticsearch.action.ClientAction;
import org.elasticsearch.alerts.client.AlertsClient;
import org.elasticsearch.alerts.client.AlertsClientAction;
import org.elasticsearch.alerts.client.AlertsClientInterface;
import org.elasticsearch.client.Client;

/**
 */
public class DeleteAlertAction extends AlertsClientAction<DeleteAlertRequest, DeleteAlertResponse, DeleteAlertRequestBuilder> {

    public static final DeleteAlertAction INSTANCE = new DeleteAlertAction();
    public static final String NAME = "indices:data/write/alert/delete";

    private DeleteAlertAction() {
        super(NAME);
    }

    @Override
    public DeleteAlertResponse newResponse() {
        return new DeleteAlertResponse();
    }

    @Override
    public DeleteAlertRequestBuilder newRequestBuilder(AlertsClientInterface client) {
        return new DeleteAlertRequestBuilder(client);
    }
}
