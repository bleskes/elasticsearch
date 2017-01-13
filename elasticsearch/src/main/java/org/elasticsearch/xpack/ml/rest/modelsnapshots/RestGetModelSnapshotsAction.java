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
import org.elasticsearch.rest.action.RestToXContentListener;
import org.elasticsearch.xpack.ml.MlPlugin;
import org.elasticsearch.xpack.ml.action.GetModelSnapshotsAction;
import org.elasticsearch.xpack.ml.action.GetModelSnapshotsAction.Request;
import org.elasticsearch.xpack.ml.job.Job;
import org.elasticsearch.xpack.ml.job.results.PageParams;

import java.io.IOException;

public class RestGetModelSnapshotsAction extends BaseRestHandler {

    // Even though these are null, setting up the defaults in case
    // we want to change them later
    private final String DEFAULT_SORT = null;
    private final String DEFAULT_START = null;
    private final String DEFAULT_END = null;
    private final String DEFAULT_DESCRIPTION = null;
    private final boolean DEFAULT_DESC_ORDER = true;

    private final GetModelSnapshotsAction.TransportAction transportGetModelSnapshotsAction;

    @Inject
    public RestGetModelSnapshotsAction(Settings settings, RestController controller,
            GetModelSnapshotsAction.TransportAction transportGetModelSnapshotsAction) {
        super(settings);
        this.transportGetModelSnapshotsAction = transportGetModelSnapshotsAction;
        controller.registerHandler(RestRequest.Method.GET, MlPlugin.BASE_PATH + "anomaly_detectors/{"
                + Job.ID.getPreferredName() + "}/model_snapshots/", this);
        // endpoints that support body parameters must also accept POST
        controller.registerHandler(RestRequest.Method.POST, MlPlugin.BASE_PATH + "anomaly_detectors/{"
                + Job.ID.getPreferredName() + "}/model_snapshots/", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        String jobId = restRequest.param(Job.ID.getPreferredName());
        Request getModelSnapshots;
        if (restRequest.hasContentOrSourceParam()) {
            XContentParser parser = restRequest.contentOrSourceParamParser();
            getModelSnapshots = Request.parseRequest(jobId, parser);
        } else {
            getModelSnapshots = new Request(jobId);
            getModelSnapshots.setSort(restRequest.param(Request.SORT.getPreferredName(), DEFAULT_SORT));
            if (restRequest.hasParam(Request.START.getPreferredName())) {
                getModelSnapshots.setStart(restRequest.param(Request.START.getPreferredName(), DEFAULT_START));
            }
            if (restRequest.hasParam(Request.END.getPreferredName())) {
                getModelSnapshots.setEnd(restRequest.param(Request.END.getPreferredName(), DEFAULT_END));
            }
            if (restRequest.hasParam(Request.DESCRIPTION.getPreferredName())) {
                getModelSnapshots.setDescriptionString(restRequest.param(Request.DESCRIPTION.getPreferredName(), DEFAULT_DESCRIPTION));
            }
            getModelSnapshots.setDescOrder(restRequest.paramAsBoolean(Request.DESC.getPreferredName(), DEFAULT_DESC_ORDER));
            getModelSnapshots.setPageParams(new PageParams(
                    restRequest.paramAsInt(PageParams.FROM.getPreferredName(), PageParams.DEFAULT_FROM),
                    restRequest.paramAsInt(PageParams.SIZE.getPreferredName(), PageParams.DEFAULT_SIZE)));
        }

        return channel -> transportGetModelSnapshotsAction.execute(getModelSnapshots, new RestToXContentListener<>(channel));
    }
}
