/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated. All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elasticsearch Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Elasticsearch Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elasticsearch Incorporated.
 */
package org.elasticsearch.xpack.prelert.rest.modelsnapshots;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.Nullable;
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
import org.elasticsearch.xpack.prelert.job.ModelSnapshot;
import org.elasticsearch.xpack.prelert.job.process.exceptions.MalformedJsonException;
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

    // NORELEASE: this will be removed with ToXContent changes
    /**
     * Given a string representing description update JSON, get the description
     * out of it.  Irrelevant junk in the JSON document is tolerated.
     */
    @Nullable
    private String parseDescriptionFromJson(@Nullable String updateJson) throws MalformedJsonException {
        if (updateJson != null && !updateJson.isEmpty()) {
            try {
                ObjectNode objNode = new ObjectMapper().readValue(updateJson, ObjectNode.class);
                JsonNode descNode = objNode.get(ModelSnapshot.DESCRIPTION.getPreferredName());
                if (descNode != null) {
                    return descNode.asText();
                }
            } catch (IOException e) {
                throw new MalformedJsonException(e);
            }
        }
        return null;    // this null ok, will catch in Request ctor
    }
}
