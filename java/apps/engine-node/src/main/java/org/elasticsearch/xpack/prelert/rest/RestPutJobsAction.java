package org.elasticsearch.xpack.prelert.rest;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.xpack.prelert.action.PutJobRequest;
import org.elasticsearch.xpack.prelert.action.PutJobResponse;
import org.elasticsearch.xpack.prelert.action.TransportPutJobAction;
import org.elasticsearch.client.Client;
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
        controller.registerHandler(RestRequest.Method.POST, "/v2/engine/jobs", this);
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
