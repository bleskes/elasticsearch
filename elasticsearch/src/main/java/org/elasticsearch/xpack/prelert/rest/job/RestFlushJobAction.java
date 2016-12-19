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

public class RestFlushJobAction extends BaseRestHandler {

    private final boolean DEFAULT_CALC_INTERIM = false;
    private final String DEFAULT_START = "";
    private final String DEFAULT_END = "";
    private final String DEFAULT_ADVANCE_TIME = "";

    private final FlushJobAction.TransportAction flushJobAction;

    @Inject
    public RestFlushJobAction(Settings settings, RestController controller,
                              FlushJobAction.TransportAction flushJobAction) {
        super(settings);
        this.flushJobAction = flushJobAction;
        controller.registerHandler(RestRequest.Method.POST, PrelertPlugin.BASE_PATH
                + "anomaly_detectors/{" + Job.ID.getPreferredName() + "}/_flush", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        String jobId = restRequest.param(Job.ID.getPreferredName());
        final FlushJobAction.Request request;
        if (restRequest.hasContentOrSourceParam()) {
            XContentParser parser = restRequest.contentOrSourceParamParser();
            request = FlushJobAction.Request.parseRequest(jobId, parser, () -> parseFieldMatcher);
        } else {
            request = new FlushJobAction.Request(restRequest.param(Job.ID.getPreferredName()));
            request.setCalcInterim(restRequest.paramAsBoolean(FlushJobAction.Request.CALC_INTERIM.getPreferredName(),
                    DEFAULT_CALC_INTERIM));
            request.setStart(restRequest.param(FlushJobAction.Request.START.getPreferredName(), DEFAULT_START));
            request.setEnd(restRequest.param(FlushJobAction.Request.END.getPreferredName(), DEFAULT_END));
            request.setAdvanceTime(restRequest.param(FlushJobAction.Request.ADVANCE_TIME.getPreferredName(), DEFAULT_ADVANCE_TIME));
        }

        return channel -> flushJobAction.execute(request, new AcknowledgedRestListener<>(channel));
    }
}
