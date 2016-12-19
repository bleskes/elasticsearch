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

public class RestDeleteSchedulerAction extends BaseRestHandler {

    private final DeleteSchedulerAction.TransportAction transportDeleteSchedulerAction;

    @Inject
    public RestDeleteSchedulerAction(Settings settings, RestController controller,
                                     DeleteSchedulerAction.TransportAction transportDeleteSchedulerAction) {
        super(settings);
        this.transportDeleteSchedulerAction = transportDeleteSchedulerAction;
        controller.registerHandler(RestRequest.Method.DELETE, PrelertPlugin.BASE_PATH + "schedulers/{"
                + SchedulerConfig.ID.getPreferredName() + "}", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        String schedulerId = restRequest.param(SchedulerConfig.ID.getPreferredName());
        DeleteSchedulerAction.Request deleteSchedulerRequest = new DeleteSchedulerAction.Request(schedulerId);
        return channel -> transportDeleteSchedulerAction.execute(deleteSchedulerRequest, new AcknowledgedRestListener<>(channel));
    }

}
