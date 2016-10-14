package org.elasticsearch.xpack.prelert.rest.data;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.*;
import org.elasticsearch.rest.action.AcknowledgedRestListener;
import org.elasticsearch.xpack.prelert.action.PostDataAction;

import java.io.IOException;

import static org.elasticsearch.rest.RestStatus.OK;

public class RestPostDataAction extends BaseRestHandler {

    private final PostDataAction.TransportAction transportPostDataAction;

    @Inject
    public RestPostDataAction(Settings settings, RestController controller, PostDataAction.TransportAction transportPostDataAction) {
        super(settings);
        this.transportPostDataAction = transportPostDataAction;
        controller.registerHandler(RestRequest.Method.POST, "/engine/v2/data/{jobId}", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        PostDataAction.Request postDataRequest = new PostDataAction.Request();
        return channel -> transportPostDataAction.execute(postDataRequest, new AcknowledgedRestListener<PostDataAction.Response>(channel) {

            @Override
            public RestResponse buildResponse(PostDataAction.Response response, XContentBuilder builder) throws Exception {
                return new BytesRestResponse(OK, XContentType.JSON.mediaType(), response.getResponse());
            }
        });
    }
}
