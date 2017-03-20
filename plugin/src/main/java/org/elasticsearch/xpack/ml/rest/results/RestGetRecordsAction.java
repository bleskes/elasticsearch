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
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.RestToXContentListener;
import org.elasticsearch.xpack.ml.MachineLearning;
import org.elasticsearch.xpack.ml.action.GetRecordsAction;
import org.elasticsearch.xpack.ml.job.config.Job;
import org.elasticsearch.xpack.ml.job.results.AnomalyRecord;
import org.elasticsearch.xpack.ml.action.util.PageParams;

import java.io.IOException;

public class RestGetRecordsAction extends BaseRestHandler {

    public RestGetRecordsAction(Settings settings, RestController controller) {
        super(settings);
        controller.registerHandler(RestRequest.Method.GET, MachineLearning.BASE_PATH + "anomaly_detectors/{"
                + Job.ID.getPreferredName() + "}/results/records", this);
        controller.registerHandler(RestRequest.Method.POST, MachineLearning.BASE_PATH + "anomaly_detectors/{"
                + Job.ID.getPreferredName() + "}/results/records", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        String jobId = restRequest.param(Job.ID.getPreferredName());
        final GetRecordsAction.Request request;
        if (restRequest.hasContent()) {
            XContentParser parser = restRequest.contentParser();
            request = GetRecordsAction.Request.parseRequest(jobId, parser);
        }
        else {
            request = new GetRecordsAction.Request(jobId);
            request.setStart(restRequest.param(GetRecordsAction.Request.START.getPreferredName()));
            request.setEnd(restRequest.param(GetRecordsAction.Request.END.getPreferredName()));
            request.setIncludeInterim(restRequest.paramAsBoolean(GetRecordsAction.Request.INCLUDE_INTERIM.getPreferredName(), false));
            request.setPageParams(new PageParams(restRequest.paramAsInt(PageParams.FROM.getPreferredName(), PageParams.DEFAULT_FROM),
                    restRequest.paramAsInt(PageParams.SIZE.getPreferredName(), PageParams.DEFAULT_SIZE)));
            request.setRecordScore(
                    Double.parseDouble(restRequest.param(GetRecordsAction.Request.RECORD_SCORE_FILTER.getPreferredName(), "0.0")));
            request.setSort(restRequest.param(GetRecordsAction.Request.SORT.getPreferredName(),
                    AnomalyRecord.RECORD_SCORE.getPreferredName()));
            request.setDecending(restRequest.paramAsBoolean(GetRecordsAction.Request.DESCENDING.getPreferredName(), true));
            String partitionValue = restRequest.param(GetRecordsAction.Request.PARTITION_VALUE.getPreferredName());
            if (partitionValue != null) {
                request.setPartitionValue(partitionValue);
            }
        }

        return channel -> client.execute(GetRecordsAction.INSTANCE, request, new RestToXContentListener<>(channel));
    }
}
