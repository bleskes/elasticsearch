package org.elasticsearch.xpack.prelert.rest.data;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.AcknowledgedRestListener;
import org.elasticsearch.xpack.prelert.action.PostDataFlushAction;

import java.io.IOException;

public class RestPostDataFlushAction extends BaseRestHandler {

    private static final ParseField JOB_ID = new ParseField("jobId");
    private static final ParseField CALC_INTERIM = new ParseField("calcInterim");
    private static final ParseField START = new ParseField("start");
    private static final ParseField END = new ParseField("end");
    private static final ParseField ADVANCE_TIME = new ParseField("advanceTime");

    private final boolean DEFAULT_CALC_INTERIM = false;
    private final String DEFAULT_START = "";
    private final String DEFAULT_END = "";
    private final String DEFAULT_ADVANCE_TIME = "";


    private final PostDataFlushAction.TransportAction transportPostDataFlushAction;

    @Inject
    public RestPostDataFlushAction(Settings settings, RestController controller,
                                   PostDataFlushAction.TransportAction transportPostDataFlushAction) {
        super(settings);
        this.transportPostDataFlushAction = transportPostDataFlushAction;
        controller.registerHandler(RestRequest.Method.POST, "/engine/v2/data/{jobId}/flush", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        PostDataFlushAction.Request postDataFlushRequest = new PostDataFlushAction.Request(restRequest.param(JOB_ID.getPreferredName()));
        postDataFlushRequest.setCalcInterim(restRequest.paramAsBoolean(CALC_INTERIM.getPreferredName(), DEFAULT_CALC_INTERIM));
        postDataFlushRequest.setStart(restRequest.param(START.getPreferredName(), DEFAULT_START));
        postDataFlushRequest.setEnd(restRequest.param(END.getPreferredName(), DEFAULT_END));
        postDataFlushRequest.setAdvanceTime(restRequest.param(ADVANCE_TIME.getPreferredName(), DEFAULT_ADVANCE_TIME));

        return channel -> transportPostDataFlushAction.execute(postDataFlushRequest, new AcknowledgedRestListener<>(channel));
    }
}
