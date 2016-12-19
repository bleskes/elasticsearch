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

import java.io.IOException;

public class RestDeleteJobAction extends BaseRestHandler {

    private final DeleteJobAction.TransportAction transportDeleteJobAction;

    @Inject
    public RestDeleteJobAction(Settings settings, RestController controller, DeleteJobAction.TransportAction transportDeleteJobAction) {
        super(settings);
        this.transportDeleteJobAction = transportDeleteJobAction;
        controller.registerHandler(RestRequest.Method.DELETE, PrelertPlugin.BASE_PATH
                + "anomaly_detectors/{" + Job.ID.getPreferredName() + "}", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        DeleteJobAction.Request deleteJobRequest = new DeleteJobAction.Request(restRequest.param(Job.ID.getPreferredName()));
        return channel -> transportDeleteJobAction.execute(deleteJobRequest, new AcknowledgedRestListener<>(channel));
    }
}
