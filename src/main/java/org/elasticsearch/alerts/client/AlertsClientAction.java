package org.elasticsearch.alerts.client;

import org.elasticsearch.action.*;
import org.elasticsearch.alerts.Alert;

/**
 * Created by brian on 10/29/14.
 */

public abstract class AlertsClientAction<Request extends ActionRequest, Response extends ActionResponse, RequestBuilder extends ActionRequestBuilder<Request, Response, RequestBuilder, AlertsClientInterface>>
        extends Action<Request, Response, RequestBuilder, AlertsClientInterface> {

    protected AlertsClientAction(String name) {
        super(name);
    }

}