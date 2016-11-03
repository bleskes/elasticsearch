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
package org.elasticsearch.xpack.prelert.rest.modelsnapshots;

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
import org.elasticsearch.rest.action.RestActions;
import org.elasticsearch.rest.action.RestStatusToXContentListener;
import org.elasticsearch.xpack.prelert.action.PutModelSnapshotDescriptionAction;
import java.io.IOException;

public class RestPutModelSnapshotDescriptionAction extends BaseRestHandler {

    private static final ParseField JOB_ID = new ParseField("jobId");
    private static final ParseField SNAPSHOT_ID = new ParseField("snapshotId");

    private final PutModelSnapshotDescriptionAction.TransportAction transportAction;

    @Inject
    public RestPutModelSnapshotDescriptionAction(Settings settings, RestController controller,
            PutModelSnapshotDescriptionAction.TransportAction transportAction) {
        super(settings);
        this.transportAction = transportAction;

        // NORELEASE: should be a POST action
        controller.registerHandler(RestRequest.Method.PUT, "/engine/v2/modelsnapshots/{jobId}/{snapshotId}/description", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        BytesReference bodyBytes = RestActions.getRestContent(restRequest);
        XContentParser parser = XContentFactory.xContent(bodyBytes).createParser(bodyBytes);
        PutModelSnapshotDescriptionAction.Request getModelSnapshots = PutModelSnapshotDescriptionAction.Request.parseRequest(
                restRequest.param(JOB_ID.getPreferredName()),
                restRequest.param(SNAPSHOT_ID.getPreferredName()),
                parser, () -> parseFieldMatcher
                );

        return channel -> transportAction.execute(getModelSnapshots, new RestStatusToXContentListener<>(channel));
    }
}
