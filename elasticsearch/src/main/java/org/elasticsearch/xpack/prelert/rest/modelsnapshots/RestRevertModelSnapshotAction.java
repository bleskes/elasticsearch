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
import org.elasticsearch.xpack.prelert.action.RevertModelSnapshotAction;
import org.elasticsearch.xpack.prelert.job.Job;

import java.io.IOException;

public class RestRevertModelSnapshotAction extends BaseRestHandler {

    private final RevertModelSnapshotAction.TransportAction transportAction;

    private final String TIME_DEFAULT = null;
    private final String SNAPSHOT_ID_DEFAULT = null;
    private final String DESCRIPTION_DEFAULT = null;
    private final boolean DELETE_INTERVENING_DEFAULT = false;

    @Inject
    public RestRevertModelSnapshotAction(Settings settings, RestController controller,
            RevertModelSnapshotAction.TransportAction transportAction) {
        super(settings);
        this.transportAction = transportAction;
        controller.registerHandler(RestRequest.Method.POST,
                PrelertPlugin.BASE_PATH + "anomaly_detectors/{" + Job.ID.getPreferredName() + "}/modelsnapshots/_revert",
                this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        String jobId = restRequest.param(Job.ID.getPreferredName());
        RevertModelSnapshotAction.Request request;
        if (restRequest.hasContentOrSourceParam()) {
            BytesReference bodyBytes = restRequest.contentOrSourceParam();
            XContentParser parser = XContentFactory.xContent(bodyBytes).createParser(bodyBytes);
            request = RevertModelSnapshotAction.Request.parseRequest(jobId, parser, () -> parseFieldMatcher);
        } else {
            request = new RevertModelSnapshotAction.Request(jobId);
            request.setTime(restRequest.param(RevertModelSnapshotAction.Request.TIME.getPreferredName(), TIME_DEFAULT));
            request.setSnapshotId(restRequest.param(RevertModelSnapshotAction.Request.SNAPSHOT_ID.getPreferredName(), SNAPSHOT_ID_DEFAULT));
            request.setDescription(
                    restRequest.param(RevertModelSnapshotAction.Request.DESCRIPTION.getPreferredName(), DESCRIPTION_DEFAULT));
            request.setDeleteInterveningResults(restRequest
                    .paramAsBoolean(RevertModelSnapshotAction.Request.DELETE_INTERVENING.getPreferredName(), DELETE_INTERVENING_DEFAULT));
        }
        return channel -> transportAction.execute(request, new RestStatusToXContentListener<>(channel));
    }
}
