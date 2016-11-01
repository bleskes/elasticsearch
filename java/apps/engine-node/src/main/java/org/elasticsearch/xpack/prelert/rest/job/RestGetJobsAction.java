/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
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
package org.elasticsearch.xpack.prelert.rest.job;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.RestActions;
import org.elasticsearch.rest.action.RestToXContentListener;
import org.elasticsearch.xpack.prelert.action.GetJobsAction;
import org.elasticsearch.xpack.prelert.action.GetJobsAction.Response;
import org.elasticsearch.xpack.prelert.job.results.PageParams;

import java.io.IOException;

public class RestGetJobsAction extends BaseRestHandler {
    private static final int DEFAULT_SKIP = 0;
    private static final int DEFAULT_TAKE = 100;

    private final GetJobsAction.TransportAction transportGetJobsAction;

    @Inject
    public RestGetJobsAction(Settings settings, RestController controller, GetJobsAction.TransportAction transportGetJobsAction) {
        super(settings);
        this.transportGetJobsAction = transportGetJobsAction;
        controller.registerHandler(RestRequest.Method.GET, "/engine/v2/jobs", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        final GetJobsAction.Request request;
        if (RestActions.hasBodyContent(restRequest)) {
            BytesReference bodyBytes = RestActions.getRestContent(restRequest);
            XContentParser parser = XContentFactory.xContent(bodyBytes).createParser(bodyBytes);
            request = GetJobsAction.Request.PARSER.apply(parser, () -> parseFieldMatcher);
        } else {
            request = new GetJobsAction.Request();
            request.setPageParams(new PageParams(restRequest.paramAsInt(PageParams.SKIP.getPreferredName(), DEFAULT_SKIP),
                    restRequest.paramAsInt(PageParams.TAKE.getPreferredName(), DEFAULT_TAKE)));
        }
        return channel -> transportGetJobsAction.execute(request, new RestToXContentListener<Response>(channel));
    }
}
