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
import org.elasticsearch.xpack.prelert.action.PutJobAction;

import java.io.IOException;

public class RestPutJobAction extends BaseRestHandler {

    private final PutJobAction.TransportAction transportPutJobAction;

    @Inject
    public RestPutJobAction(Settings settings, RestController controller, PutJobAction.TransportAction transportPutJobAction) {
        super(settings);
        this.transportPutJobAction = transportPutJobAction;
        controller.registerHandler(RestRequest.Method.PUT, PrelertPlugin.BASE_PATH + "anomaly_detectors", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        XContentParser parser = XContentFactory.xContent(restRequest.content()).createParser(restRequest.content());
        PutJobAction.Request putJobRequest = PutJobAction.Request.parseRequest(parser, () -> parseFieldMatcher);
        boolean overwrite = restRequest.paramAsBoolean("overwrite", false);
        putJobRequest.setOverwrite(overwrite);
        return channel -> transportPutJobAction.execute(putJobRequest, new RestToXContentListener<>(channel));
    }

}
