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
package org.elasticsearch.xpack.prelert.rest.job;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.RestStatusToXContentListener;
import org.elasticsearch.xpack.prelert.PrelertPlugin;
import org.elasticsearch.xpack.prelert.action.GetJobAction;
import org.elasticsearch.xpack.prelert.job.Job;

import java.io.IOException;
import java.util.Set;

public class RestGetJobAction extends BaseRestHandler {

    private final GetJobAction.TransportAction transportGetJobAction;

    @Inject
    public RestGetJobAction(Settings settings, RestController controller, GetJobAction.TransportAction transportGetJobAction) {
        super(settings);
        this.transportGetJobAction = transportGetJobAction;
        controller.registerHandler(RestRequest.Method.GET, PrelertPlugin.BASE_PATH + "jobs/{" + Job.ID.getPreferredName() + "}", this);
        controller.registerHandler(RestRequest.Method.GET,
                PrelertPlugin.BASE_PATH + "jobs/{" + Job.ID.getPreferredName() + "}/{metric}", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        GetJobAction.Request getJobRequest = new GetJobAction.Request(restRequest.param(Job.ID.getPreferredName()));
        Set<String> stats = Strings.splitStringByCommaToSet(restRequest.param("metric", "config"));
        if (stats.contains("_all")) {
            getJobRequest.all();
        }
        else {
            getJobRequest.config(stats.contains("config"));
            getJobRequest.dataCounts(stats.contains("data_counts"));
            getJobRequest.modelSizeStats(stats.contains("model_size_stats"));
        }

        return channel -> transportGetJobAction.execute(getJobRequest, new RestStatusToXContentListener<>(channel));
    }
}
