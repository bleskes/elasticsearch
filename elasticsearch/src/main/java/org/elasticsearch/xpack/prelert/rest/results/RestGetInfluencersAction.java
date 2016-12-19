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
import org.elasticsearch.xpack.prelert.action.GetInfluencersAction;
import org.elasticsearch.xpack.prelert.job.Job;
import org.elasticsearch.xpack.prelert.job.results.Influencer;
import org.elasticsearch.xpack.prelert.job.results.PageParams;

import java.io.IOException;

public class RestGetInfluencersAction extends BaseRestHandler {

    private final GetInfluencersAction.TransportAction transportAction;

    @Inject
    public RestGetInfluencersAction(Settings settings, RestController controller, GetInfluencersAction.TransportAction transportAction) {
        super(settings);
        this.transportAction = transportAction;
        controller.registerHandler(RestRequest.Method.GET,
                PrelertPlugin.BASE_PATH + "anomaly_detectors/{" + Job.ID.getPreferredName() + "}/results/influencers", this);
        // endpoints that support body parameters must also accept POST
        controller.registerHandler(RestRequest.Method.POST,
                PrelertPlugin.BASE_PATH + "anomaly_detectors/{" + Job.ID.getPreferredName() + "}/results/influencers", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        String jobId = restRequest.param(Job.ID.getPreferredName());
        String start = restRequest.param(GetInfluencersAction.Request.START.getPreferredName());
        String end = restRequest.param(GetInfluencersAction.Request.END.getPreferredName());
        BytesReference bodyBytes = restRequest.content();
        final GetInfluencersAction.Request request;
        if (bodyBytes != null && bodyBytes.length() > 0) {
            XContentParser parser = XContentFactory.xContent(bodyBytes).createParser(bodyBytes);
            request = GetInfluencersAction.Request.parseRequest(jobId, parser, () -> parseFieldMatcher);
        } else {
            request = new GetInfluencersAction.Request(jobId);
            request.setStart(start);
            request.setEnd(end);
            request.setIncludeInterim(restRequest.paramAsBoolean(GetInfluencersAction.Request.INCLUDE_INTERIM.getPreferredName(), false));
            request.setPageParams(new PageParams(restRequest.paramAsInt(PageParams.FROM.getPreferredName(), PageParams.DEFAULT_FROM),
                    restRequest.paramAsInt(PageParams.SIZE.getPreferredName(), PageParams.DEFAULT_SIZE)));
            request.setAnomalyScore(
                    Double.parseDouble(restRequest.param(GetInfluencersAction.Request.ANOMALY_SCORE.getPreferredName(), "0.0")));
            request.setSort(restRequest.param(GetInfluencersAction.Request.SORT_FIELD.getPreferredName(),
                    Influencer.ANOMALY_SCORE.getPreferredName()));
            request.setDecending(restRequest.paramAsBoolean(GetInfluencersAction.Request.DESCENDING_SORT.getPreferredName(), true));
        }

        return channel -> transportAction.execute(request, new RestToXContentListener<GetInfluencersAction.Response>(channel));
    }
}
