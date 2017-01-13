/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.ml.rest.validate;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.AcknowledgedRestListener;
import org.elasticsearch.xpack.ml.MlPlugin;
import org.elasticsearch.xpack.ml.action.ValidateTransformsAction;

import java.io.IOException;

public class RestValidateTransformsAction extends BaseRestHandler {

    private ValidateTransformsAction.TransportAction transportValidateAction;

    @Inject
    public RestValidateTransformsAction(Settings settings, RestController controller,
            ValidateTransformsAction.TransportAction transportValidateAction) {
        super(settings);
        this.transportValidateAction = transportValidateAction;
        controller.registerHandler(RestRequest.Method.POST, MlPlugin.BASE_PATH + "_validate/transforms", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        XContentParser parser = restRequest.contentOrSourceParamParser();
        ValidateTransformsAction.Request validateDetectorRequest = ValidateTransformsAction.Request.PARSER.apply(parser, null);
        return channel -> transportValidateAction.execute(validateDetectorRequest, new AcknowledgedRestListener<>(channel));
    }

}
