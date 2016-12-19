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
import org.elasticsearch.xpack.prelert.job.Job;

import java.io.IOException;

public class RestCloseJobAction extends BaseRestHandler {

    private final CloseJobAction.TransportAction closeJobAction;

    @Inject
    public RestCloseJobAction(Settings settings, RestController controller, CloseJobAction.TransportAction closeJobAction) {
        super(settings);
        this.closeJobAction = closeJobAction;
        controller.registerHandler(RestRequest.Method.POST, PrelertPlugin.BASE_PATH
                + "anomaly_detectors/{" + Job.ID.getPreferredName() + "}/_close", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        CloseJobAction.Request request = new CloseJobAction.Request(restRequest.param(Job.ID.getPreferredName()));
        if (restRequest.hasParam("close_timeout")) {
            request.setCloseTimeout(TimeValue.parseTimeValue(restRequest.param("close_timeout"), "close_timeout"));
        }
        return channel -> closeJobAction.execute(request, new AcknowledgedRestListener<>(channel));
    }
}
