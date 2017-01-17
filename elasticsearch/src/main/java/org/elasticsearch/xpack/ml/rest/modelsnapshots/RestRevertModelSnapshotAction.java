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
import org.elasticsearch.xpack.ml.action.RevertModelSnapshotAction;
import org.elasticsearch.xpack.ml.job.Job;

import java.io.IOException;

public class RestRevertModelSnapshotAction extends BaseRestHandler {

    private final String TIME_DEFAULT = null;
    private final String SNAPSHOT_ID_DEFAULT = null;
    private final String DESCRIPTION_DEFAULT = null;
    private final boolean DELETE_INTERVENING_DEFAULT = false;

    @Inject
    public RestRevertModelSnapshotAction(Settings settings, RestController controller) {
        super(settings);
        controller.registerHandler(RestRequest.Method.POST,
                MlPlugin.BASE_PATH + "anomaly_detectors/{" + Job.ID.getPreferredName() + "}/model_snapshots/_revert",
                this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        String jobId = restRequest.param(Job.ID.getPreferredName());
        RevertModelSnapshotAction.Request request;
        if (restRequest.hasContentOrSourceParam()) {
            XContentParser parser = restRequest.contentOrSourceParamParser();
            request = RevertModelSnapshotAction.Request.parseRequest(jobId, parser);
        } else {
            request = new RevertModelSnapshotAction.Request(jobId);
            request.setTime(restRequest.param(RevertModelSnapshotAction.Request.TIME.getPreferredName(), TIME_DEFAULT));
            request.setSnapshotId(restRequest.param(RevertModelSnapshotAction.Request.SNAPSHOT_ID.getPreferredName(), SNAPSHOT_ID_DEFAULT));
            request.setDescription(
                    restRequest.param(RevertModelSnapshotAction.Request.DESCRIPTION.getPreferredName(), DESCRIPTION_DEFAULT));
            request.setDeleteInterveningResults(restRequest
                    .paramAsBoolean(RevertModelSnapshotAction.Request.DELETE_INTERVENING.getPreferredName(), DELETE_INTERVENING_DEFAULT));
        }
        return channel -> client.execute(RevertModelSnapshotAction.INSTANCE, request, new RestStatusToXContentListener<>(channel));
    }
}
