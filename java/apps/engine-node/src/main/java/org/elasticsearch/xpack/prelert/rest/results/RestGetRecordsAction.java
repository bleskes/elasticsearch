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
package org.elasticsearch.xpack.prelert.rest.results;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.RestToXContentListener;
import org.elasticsearch.xpack.prelert.action.GetRecordsAction;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.results.AnomalyRecord;
import org.elasticsearch.xpack.prelert.job.results.PageParams;

import java.io.IOException;

public class RestGetRecordsAction extends BaseRestHandler {

    private final GetRecordsAction.TransportAction transportAction;

    @Inject
    public RestGetRecordsAction(Settings settings, RestController controller, GetRecordsAction.TransportAction transportAction) {
        super(settings);
        this.transportAction = transportAction;
        controller.registerHandler(RestRequest.Method.GET, "/engine/v2/results/{" + JobDetails.ID.getPreferredName() + "}/records", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        GetRecordsAction.Request request = new GetRecordsAction.Request(restRequest.param(JobDetails.ID.getPreferredName()),
                restRequest.param(GetRecordsAction.Request.START.getPreferredName()),
                restRequest.param(GetRecordsAction.Request.END.getPreferredName()));
        request.setIncludeInterim(restRequest.paramAsBoolean(GetRecordsAction.Request.INCLUDE_INTERIM.getPreferredName(), false));
        request.setPageParams(new PageParams(restRequest.paramAsInt(PageParams.SKIP.getPreferredName(), 0),
                restRequest.paramAsInt(PageParams.TAKE.getPreferredName(), 100)));
        request.setAnomalyScore(
                Double.parseDouble(restRequest.param(GetRecordsAction.Request.ANOMALY_SCORE_FILTER.getPreferredName(), "0.0")));
        request.setSort(restRequest.param(GetRecordsAction.Request.SORT.getPreferredName(),
                AnomalyRecord.NORMALIZED_PROBABILITY.getPreferredName()));
        request.setDecending(restRequest.paramAsBoolean(GetRecordsAction.Request.DESCENDING.getPreferredName(), false));
        request.setMaxNormalizedProbability(
                Double.parseDouble(restRequest.param(GetRecordsAction.Request.MAX_NORMALIZED_PROBABILITY.getPreferredName(), "0.0")));
        String partitionValue = restRequest.param(GetRecordsAction.Request.PARTITION_VALUE.getPreferredName());
        if (partitionValue != null) {
            request.setPartitionValue(partitionValue);
        }

        return channel -> transportAction.execute(request, new RestToXContentListener<>(channel));
    }
}
