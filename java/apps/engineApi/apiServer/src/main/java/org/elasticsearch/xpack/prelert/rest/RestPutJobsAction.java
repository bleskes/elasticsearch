package org.elasticsearch.xpack.prelert.rest;

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
import org.elasticsearch.rest.action.support.AcknowledgedRestListener;
import org.elasticsearch.rest.action.support.RestActions;

import static org.elasticsearch.rest.RestStatus.OK;

public class RestPutJobsAction extends BaseRestHandler {

    private final TransportPutJobAction transportPutJobAction;

    @Inject
    public RestPutJobsAction(Settings settings, RestController controller, Client client, TransportPutJobAction transportPutJobAction) {
        super(settings, controller, client);
        this.transportPutJobAction = transportPutJobAction;
        controller.registerHandler(RestRequest.Method.POST, "/engine/v2/jobs", this);
    }

    @Override
    protected void handleRequest(RestRequest request, RestChannel channel, Client client) throws Exception {
        PutJobRequest putJobRequest = new PutJobRequest();
        putJobRequest.setJobConfiguration(RestActions.getRestContent(request));
        boolean overwrite = request.paramAsBoolean("overwrite", false);
        putJobRequest.setOverwrite(overwrite);
        transportPutJobAction.execute(putJobRequest, new AcknowledgedRestListener<PutJobResponse>(channel) {

            @Override
            public RestResponse buildResponse(PutJobResponse response, XContentBuilder builder) throws Exception {
                return new BytesRestResponse(OK, XContentType.JSON.restContentType(), response.getResponse());
            }
        });
    }
}
