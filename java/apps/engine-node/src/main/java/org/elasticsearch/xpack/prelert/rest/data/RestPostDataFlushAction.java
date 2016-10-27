package org.elasticsearch.xpack.prelert.rest.data;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.AcknowledgedRestListener;
import org.elasticsearch.rest.action.RestActions;
import org.elasticsearch.xpack.prelert.action.PostDataFlushAction;
import org.elasticsearch.xpack.prelert.job.JobDetails;

import java.io.IOException;

public class RestPostDataFlushAction extends BaseRestHandler {

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
        controller.registerHandler(RestRequest.Method.POST, "/engine/v2/data/{" + JobDetails.ID.getPreferredName() + "}/flush", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        String jobId = restRequest.param(JobDetails.ID.getPreferredName());
        BytesReference bodyBytes = RestActions.getRestContent(restRequest);
        final PostDataFlushAction.Request request;
        if (RestActions.hasBodyContent(restRequest)) {
            XContentParser parser = XContentFactory.xContent(bodyBytes).createParser(bodyBytes);
            request = PostDataFlushAction.Request.parseRequest(jobId, parser, () -> parseFieldMatcher);
        } else {
            request = new PostDataFlushAction.Request(restRequest.param(JobDetails.ID.getPreferredName()));
            request.setCalcInterim(restRequest.paramAsBoolean(PostDataFlushAction.Request.CALC_INTERIM.getPreferredName(),
                    DEFAULT_CALC_INTERIM));
            request.setStart(restRequest.param(PostDataFlushAction.Request.START.getPreferredName(), DEFAULT_START));
            request.setEnd(restRequest.param(PostDataFlushAction.Request.END.getPreferredName(), DEFAULT_END));
            request.setAdvanceTime(restRequest.param(PostDataFlushAction.Request.ADVANCE_TIME.getPreferredName(), DEFAULT_ADVANCE_TIME));
        }

        return channel -> transportPostDataFlushAction.execute(request, new AcknowledgedRestListener<>(channel));
    }
}
