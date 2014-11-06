package org.elasticsearch.alerts.transport.actions.index;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.master.MasterNodeOperationRequestBuilder;
import org.elasticsearch.alerts.client.AlertsClientInterface;
import org.elasticsearch.common.bytes.BytesReference;

/**
 */
public class IndexAlertRequestBuilder
        extends MasterNodeOperationRequestBuilder<IndexAlertRequest, IndexAlertResponse,
        IndexAlertRequestBuilder, AlertsClientInterface> {


    public IndexAlertRequestBuilder(AlertsClientInterface client) {
        super(client, new IndexAlertRequest());
    }


    public IndexAlertRequestBuilder(AlertsClientInterface client, String alertName) {
        super(client, new IndexAlertRequest());
        request.alertName(alertName);
    }

    public IndexAlertRequestBuilder setAlertName(String alertName){
        request.alertName(alertName);
        return this;
    }

    public IndexAlertRequestBuilder setAlertSource(BytesReference alertSource) {
        request.alertSource(alertSource);
        return this;
    }


    @Override
    protected void doExecute(ActionListener<IndexAlertResponse> listener) {
        client.createAlert(request, listener);
    }
}
