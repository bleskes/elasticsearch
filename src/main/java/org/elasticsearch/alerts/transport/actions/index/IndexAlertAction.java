package org.elasticsearch.alerts.transport.actions.index;

import org.elasticsearch.alerts.client.AlertsClientAction;
import org.elasticsearch.alerts.client.AlertsClientInterface;

/**
 */
public class IndexAlertAction extends AlertsClientAction<IndexAlertRequest, IndexAlertResponse, IndexAlertRequestBuilder> {

    public static final IndexAlertAction INSTANCE = new IndexAlertAction();
    public static final String NAME = "indices:data/write/alert/create";

    private IndexAlertAction() {
        super(NAME);
    }


    @Override
    public IndexAlertRequestBuilder newRequestBuilder(AlertsClientInterface client) {
        return new IndexAlertRequestBuilder(client);
    }

    @Override
    public IndexAlertResponse newResponse() {
        return new IndexAlertResponse();
    }
}
