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

package org.elasticsearch.xpack.prelert.rest.validate;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.AcknowledgedRestListener;
import org.elasticsearch.xpack.prelert.action.ValidateDetectorAction;

public class RestValidateDetectorAction extends BaseRestHandler {

    private ValidateDetectorAction.TransportAction transportValidateAction;

    @Inject
    public RestValidateDetectorAction(Settings settings, RestController controller,
            ValidateDetectorAction.TransportAction transportValidateAction) {
        super(settings);
        this.transportValidateAction = transportValidateAction;
        controller.registerHandler(RestRequest.Method.GET, "/engine/v2/validate/detector", this);
        controller.registerHandler(RestRequest.Method.POST, "/engine/v2/validate/detector", this);
    }

    @Override
    public void handleRequest(RestRequest request, RestChannel channel, NodeClient client) throws Exception {
        ValidateDetectorAction.Request validateDetectorRequest = new ValidateDetectorAction.Request(request.content());
        transportValidateAction.execute(validateDetectorRequest, new AcknowledgedRestListener<ValidateDetectorAction.Response>(channel));
    }

}
