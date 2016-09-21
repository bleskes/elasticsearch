package org.elasticsearch.xpack.prelert.rest.job;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.xpack.prelert.action.job.PutJobRequest;
import org.elasticsearch.xpack.prelert.action.job.PutJobResponse;
import org.elasticsearch.xpack.prelert.action.job.TransportPutJobAction;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.action.AcknowledgedRestListener;
import org.elasticsearch.rest.action.RestActions;

import static org.elasticsearch.rest.RestStatus.OK;

public class RestPutJobsAction extends BaseRestHandler {

    private final TransportPutJobAction transportPutJobAction;

    @Inject
    public RestPutJobsAction(Settings settings, RestController controller, TransportPutJobAction transportPutJobAction) {
        super(settings);
        this.transportPutJobAction = transportPutJobAction;
        controller.registerHandler(RestRequest.Method.POST, "/engine/v2/jobs", this);
    }

    @Override
    public void handleRequest(RestRequest request, RestChannel channel, NodeClient client) throws Exception {
        PutJobRequest putJobRequest = new PutJobRequest();
        putJobRequest.setJobConfiguration(RestActions.getRestContent(request));
        boolean overwrite = request.paramAsBoolean("overwrite", false);
        putJobRequest.setOverwrite(overwrite);
        transportPutJobAction.execute(putJobRequest, new AcknowledgedRestListener<PutJobResponse>(channel) {

            @Override
            public RestResponse buildResponse(PutJobResponse response, XContentBuilder builder) throws Exception {
                return new BytesRestResponse(OK, XContentType.JSON.mediaType(), response.getResponse());
            }
        });
    }

}
