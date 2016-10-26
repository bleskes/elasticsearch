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
package org.elasticsearch.xpack.prelert.rest.schedulers;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.mapper.DateFieldMapper;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.AcknowledgedRestListener;
import org.elasticsearch.xpack.prelert.action.StartJobSchedulerAction;
import org.elasticsearch.xpack.prelert.job.JobSchedulerStatus;
import org.elasticsearch.xpack.prelert.job.SchedulerState;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;

import java.io.IOException;

public class RestStartJobSchedulerAction extends BaseRestHandler {

    private static final ParseField JOB_ID = new ParseField("jobId");
    private static final ParseField START = new ParseField("start");
    private static final ParseField END = new ParseField("end");

    private static final String DEFAULT_START = "0";

    private final StartJobSchedulerAction.TransportAction transportJobSchedulerAction;

    @Inject
    public RestStartJobSchedulerAction(Settings settings, RestController controller,
                                       StartJobSchedulerAction.TransportAction transportJobSchedulerAction) {
        super(settings);
        this.transportJobSchedulerAction = transportJobSchedulerAction;
        controller.registerHandler(RestRequest.Method.POST, "/engine/v2/schedulers/{jobId}/start", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        String jobId = restRequest.param(JOB_ID.getPreferredName());
        long startTimeMillis = parseDateOrThrow(restRequest.param(START.getPreferredName(), DEFAULT_START), START.getPreferredName());
        Long endTimeMillis = null;
        if (restRequest.hasParam(END.getPreferredName())) {
            endTimeMillis = parseDateOrThrow(restRequest.param(END.getPreferredName()), END.getPreferredName());
        }
        SchedulerState schedulerState = new SchedulerState(JobSchedulerStatus.STARTED, startTimeMillis, endTimeMillis);
        StartJobSchedulerAction.Request jobSchedulerRequest = new StartJobSchedulerAction.Request(jobId, schedulerState);
        return channel -> transportJobSchedulerAction.execute(jobSchedulerRequest, new AcknowledgedRestListener<>(channel));
    }

    private long parseDateOrThrow(String date, String paramName) {
        try {
            return DateFieldMapper.DEFAULT_DATE_TIME_FORMATTER.parser().parseMillis(date);
        } catch (IllegalArgumentException e) {
            String msg = Messages.getMessage(Messages.REST_INVALID_DATETIME_PARAMS, paramName, date);
            throw ExceptionsHelper.invalidRequestException(msg, ErrorCodes.UNPARSEABLE_DATE_ARGUMENT);
        }
    }
}
