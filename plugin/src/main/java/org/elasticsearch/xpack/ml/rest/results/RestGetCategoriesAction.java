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
package org.elasticsearch.xpack.ml.rest.results;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.RestToXContentListener;
import org.elasticsearch.xpack.ml.MachineLearning;
import org.elasticsearch.xpack.ml.action.GetCategoriesAction;
import org.elasticsearch.xpack.ml.action.GetCategoriesAction.Request;
import org.elasticsearch.xpack.ml.action.util.PageParams;
import org.elasticsearch.xpack.ml.job.config.Job;

import java.io.IOException;

public class RestGetCategoriesAction extends BaseRestHandler {

    public RestGetCategoriesAction(Settings settings, RestController controller) {
        super(settings);
        controller.registerHandler(RestRequest.Method.GET,
                MachineLearning.BASE_PATH + "anomaly_detectors/{" + Job.ID.getPreferredName() + "}/results/categories/{"
                + Request.CATEGORY_ID.getPreferredName() + "}", this);
        controller.registerHandler(RestRequest.Method.GET,
                MachineLearning.BASE_PATH + "anomaly_detectors/{" + Job.ID.getPreferredName() + "}/results/categories", this);

        controller.registerHandler(RestRequest.Method.POST,
                MachineLearning.BASE_PATH + "anomaly_detectors/{" + Job.ID.getPreferredName() + "}/results/categories/{"
                + Request.CATEGORY_ID.getPreferredName() + "}", this);
        controller.registerHandler(RestRequest.Method.POST,
                MachineLearning.BASE_PATH + "anomaly_detectors/{" + Job.ID.getPreferredName() + "}/results/categories", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        Request request;
        String jobId = restRequest.param(Job.ID.getPreferredName());
        Long categoryId = restRequest.hasParam(Request.CATEGORY_ID.getPreferredName()) ? Long.parseLong(
                restRequest.param(Request.CATEGORY_ID.getPreferredName())) : null;
        BytesReference bodyBytes = restRequest.content();

        if (bodyBytes != null && bodyBytes.length() > 0) {
            XContentParser parser = restRequest.contentParser();
            request = GetCategoriesAction.Request.parseRequest(jobId, parser);
            if (categoryId != null) {
                request.setCategoryId(categoryId);
            }
        } else {
            request = new Request(jobId);
            if (categoryId != null) {
                request.setCategoryId(categoryId);
            }
            if (restRequest.hasParam(Request.FROM.getPreferredName())
                    || restRequest.hasParam(Request.SIZE.getPreferredName())
                    || categoryId == null){

                request.setPageParams(new PageParams(
                        restRequest.paramAsInt(Request.FROM.getPreferredName(), 0),
                        restRequest.paramAsInt(Request.SIZE.getPreferredName(), 100)
                ));
            }
        }

        return channel -> client.execute(GetCategoriesAction.INSTANCE, request, new RestToXContentListener<>(channel));
    }

}
