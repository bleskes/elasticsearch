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
import org.elasticsearch.xpack.prelert.scheduler.SchedulerConfig;

import java.io.IOException;

public class RestStopSchedulerAction extends BaseRestHandler {

    private final StopSchedulerAction.TransportAction transportJobSchedulerAction;

    @Inject
    public RestStopSchedulerAction(Settings settings, RestController controller,
                                   StopSchedulerAction.TransportAction transportJobSchedulerAction) {
        super(settings);
        this.transportJobSchedulerAction = transportJobSchedulerAction;
        controller.registerHandler(RestRequest.Method.POST, PrelertPlugin.BASE_PATH + "schedulers/{"
                + SchedulerConfig.ID.getPreferredName() + "}/_stop", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        StopSchedulerAction.Request jobSchedulerRequest = new StopSchedulerAction.Request(
                restRequest.param(SchedulerConfig.ID.getPreferredName()));
        if (restRequest.hasParam("stop_timeout")) {
            jobSchedulerRequest.setStopTimeout(TimeValue.parseTimeValue(restRequest.param("stop_timeout"), "stop_timeout"));
        }
        return channel -> transportJobSchedulerAction.execute(jobSchedulerRequest, new AcknowledgedRestListener<>(channel));
    }
}
