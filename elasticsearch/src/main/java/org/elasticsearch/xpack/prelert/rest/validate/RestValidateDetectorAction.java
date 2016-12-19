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
import org.elasticsearch.xpack.prelert.PrelertPlugin;
import org.elasticsearch.xpack.prelert.action.ValidateDetectorAction;

import java.io.IOException;

public class RestValidateDetectorAction extends BaseRestHandler {

    private ValidateDetectorAction.TransportAction transportValidateAction;

    @Inject
    public RestValidateDetectorAction(Settings settings, RestController controller,
            ValidateDetectorAction.TransportAction transportValidateAction) {
        super(settings);
        this.transportValidateAction = transportValidateAction;
        controller.registerHandler(RestRequest.Method.POST, PrelertPlugin.BASE_PATH + "_validate/detector", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        BytesReference bodyBytes = restRequest.contentOrSourceParam();
        XContentParser parser = XContentFactory.xContent(bodyBytes).createParser(bodyBytes);
        ValidateDetectorAction.Request validateDetectorRequest = ValidateDetectorAction.Request.parseRequest(parser,
                () -> parseFieldMatcher);
        return channel -> transportValidateAction.execute(validateDetectorRequest,
                new AcknowledgedRestListener<ValidateDetectorAction.Response>(channel));
    }

}
