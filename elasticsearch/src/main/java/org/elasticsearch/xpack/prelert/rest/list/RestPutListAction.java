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
import org.elasticsearch.xpack.prelert.action.PutListAction;

import java.io.IOException;

public class RestPutListAction extends BaseRestHandler {

    private final PutListAction.TransportAction transportCreateListAction;

    @Inject
    public RestPutListAction(Settings settings, RestController controller, PutListAction.TransportAction transportCreateListAction) {
        super(settings);
        this.transportCreateListAction = transportCreateListAction;
        controller.registerHandler(RestRequest.Method.PUT, PrelertPlugin.BASE_PATH + "lists", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        BytesReference bodyBytes = restRequest.contentOrSourceParam();
        XContentParser parser = XContentFactory.xContent(bodyBytes).createParser(bodyBytes);
        PutListAction.Request putListRequest = PutListAction.Request.parseRequest(parser, () -> parseFieldMatcher);
        return channel -> transportCreateListAction.execute(putListRequest, new AcknowledgedRestListener<>(channel));
    }

}
