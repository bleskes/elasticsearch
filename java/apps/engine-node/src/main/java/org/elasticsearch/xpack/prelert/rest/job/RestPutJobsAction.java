package org.elasticsearch.xpack.prelert.rest.job;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.*;
import org.elasticsearch.rest.action.AcknowledgedRestListener;
import org.elasticsearch.rest.action.RestActions;
import org.elasticsearch.xpack.prelert.action.PutJobAction;

import java.io.IOException;

import static org.elasticsearch.rest.RestStatus.OK;

public class RestPutJobsAction extends BaseRestHandler {

    private final PutJobAction.TransportAction transportPutJobAction;

    @Inject
    public RestPutJobsAction(Settings settings, RestController controller, PutJobAction.TransportAction transportPutJobAction) {
        super(settings);
        this.transportPutJobAction = transportPutJobAction;
        controller.registerHandler(RestRequest.Method.POST, "/engine/v2/jobs", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        PutJobAction.Request putJobRequest = new PutJobAction.Request();
        putJobRequest.setJobConfiguration(RestActions.getRestContent(restRequest));
        boolean overwrite = restRequest.paramAsBoolean("overwrite", false);
        putJobRequest.setOverwrite(overwrite);
        return channel -> transportPutJobAction.execute(putJobRequest, new AcknowledgedRestListener<PutJobAction.Response>(channel) {

            @Override
            public RestResponse buildResponse(PutJobAction.Response response, XContentBuilder builder) throws Exception {
                return new BytesRestResponse(OK, XContentType.JSON.mediaType(), response.getResponse());
            }
        });
    }

}
