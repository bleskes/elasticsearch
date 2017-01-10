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
package org.elasticsearch.xpack.ml.rest.modelsnapshots;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.RestStatusToXContentListener;
import org.elasticsearch.xpack.ml.MlPlugin;
import org.elasticsearch.xpack.ml.action.UpdateModelSnapshotAction;
import org.elasticsearch.xpack.ml.job.Job;
import org.elasticsearch.xpack.ml.job.ModelSnapshot;

import java.io.IOException;

public class RestUpdateModelSnapshotAction extends BaseRestHandler {

    private final UpdateModelSnapshotAction.TransportAction transportAction;

    @Inject
    public RestUpdateModelSnapshotAction(Settings settings, RestController controller,
                                         UpdateModelSnapshotAction.TransportAction transportAction) {
        super(settings);
        this.transportAction = transportAction;

        controller.registerHandler(RestRequest.Method.POST, MlPlugin.BASE_PATH + "anomaly_detectors/{"
                + Job.ID.getPreferredName() + "}/model_snapshots/{" + ModelSnapshot.SNAPSHOT_ID +"}/_update",
                this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        XContentParser parser = restRequest.contentParser();
        UpdateModelSnapshotAction.Request getModelSnapshots = UpdateModelSnapshotAction.Request.parseRequest(
                restRequest.param(Job.ID.getPreferredName()),
                restRequest.param(ModelSnapshot.SNAPSHOT_ID.getPreferredName()),
                parser, () -> parseFieldMatcher
                );

        return channel -> transportAction.execute(getModelSnapshots, new RestStatusToXContentListener<>(channel));
    }
}
