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

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.*;
import org.elasticsearch.rest.action.RestStatusToXContentListener;
import org.elasticsearch.xpack.prelert.action.RevertModelSnapshotAction;

import java.io.IOException;

public class RestRevertModelSnapshotAction extends BaseRestHandler {

    private final RevertModelSnapshotAction.TransportAction transportAction;

    private final ParseField JOB_ID = new ParseField("jobId");
    private final ParseField TIME = new ParseField("time");
    private final ParseField SNAPSHOT_ID = new ParseField("snapshotId");
    private final ParseField DESCRIPTION = new ParseField("description");
    private final ParseField DELETE_INTERVENING = new ParseField("deleteInterveningResults");

    private final String TIME_DEFAULT = null;
    private final String SNAPSHOT_ID_DEFAULT = null;
    private final String DESCRIPTION_DEFAULT = null;
    private final boolean DELETE_INTERVENING_DEFAULT = false;

    @Inject
    public RestRevertModelSnapshotAction(Settings settings, RestController controller,
                                         RevertModelSnapshotAction.TransportAction transportAction) {
        super(settings);
        this.transportAction = transportAction;
        controller.registerHandler(RestRequest.Method.POST, "/engine/v2/modelsnapshots/{jobId}/revert", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        RevertModelSnapshotAction.Request request =
                new RevertModelSnapshotAction.Request(restRequest.param(JOB_ID.getPreferredName()));
        request.setTime(restRequest.param(TIME.getPreferredName(), TIME_DEFAULT));
        request.setSnapshotId(restRequest.param(SNAPSHOT_ID.getPreferredName(), SNAPSHOT_ID_DEFAULT));
        request.setDescription(restRequest.param(DESCRIPTION.getPreferredName(), DESCRIPTION_DEFAULT));
        request.setDeleteInterveningResults(restRequest.paramAsBoolean
                (DELETE_INTERVENING.getPreferredName(), DELETE_INTERVENING_DEFAULT));

        return channel -> transportAction.execute(request, new RestStatusToXContentListener<>(channel));
    }
}
