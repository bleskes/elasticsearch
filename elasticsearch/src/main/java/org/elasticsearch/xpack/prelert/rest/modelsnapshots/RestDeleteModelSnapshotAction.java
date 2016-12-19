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
import org.elasticsearch.xpack.prelert.job.ModelSnapshot;

import java.io.IOException;

public class RestDeleteModelSnapshotAction extends BaseRestHandler {

    private final DeleteModelSnapshotAction.TransportAction transportAction;

    @Inject
    public RestDeleteModelSnapshotAction(Settings settings, RestController controller,
            DeleteModelSnapshotAction.TransportAction transportAction) {
        super(settings);
        this.transportAction = transportAction;
        controller.registerHandler(RestRequest.Method.DELETE, PrelertPlugin.BASE_PATH + "anomaly_detectors/{"
                + Job.ID.getPreferredName() + "}/modelsnapshots/{" + ModelSnapshot.SNAPSHOT_ID.getPreferredName() + "}", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        DeleteModelSnapshotAction.Request deleteModelSnapshot = new DeleteModelSnapshotAction.Request(
                restRequest.param(Job.ID.getPreferredName()),
                restRequest.param(ModelSnapshot.SNAPSHOT_ID.getPreferredName()));

        return channel -> transportAction.execute(deleteModelSnapshot, new AcknowledgedRestListener<>(channel));
    }
}
