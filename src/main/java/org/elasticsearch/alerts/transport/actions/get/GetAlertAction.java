package org.elasticsearch.alerts.transport.actions.get;

import org.elasticsearch.action.ClientAction;
import org.elasticsearch.alerts.client.AlertsClient;
import org.elasticsearch.alerts.client.AlertsClientAction;
import org.elasticsearch.alerts.client.AlertsClientInterface;
import org.elasticsearch.client.Client;

/**
 */
public class GetAlertAction extends AlertsClientAction<GetAlertRequest, GetAlertResponse, GetAlertRequestBuilder> {

    public static final GetAlertAction INSTANCE = new GetAlertAction();
    public static final String NAME = "indices:data/read/alert/get";

    private GetAlertAction() {
        super(NAME);
    }

    @Override
    public GetAlertResponse newResponse() {
        return new GetAlertResponse();
    }

    @Override
    public GetAlertRequestBuilder newRequestBuilder(AlertsClientInterface client) {
        return new GetAlertRequestBuilder(client);
    }
}
