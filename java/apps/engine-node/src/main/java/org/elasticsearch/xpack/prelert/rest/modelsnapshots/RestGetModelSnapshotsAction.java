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
package org.elasticsearch.xpack.prelert.rest.modelsnapshots;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.*;
import org.elasticsearch.rest.action.RestBuilderListener;
import org.elasticsearch.xpack.prelert.action.GetModelSnapshotsAction;
import org.elasticsearch.xpack.prelert.job.results.PageParams;

import java.io.IOException;

import static org.elasticsearch.rest.RestStatus.OK;

public class RestGetModelSnapshotsAction extends BaseRestHandler {

    private static final ParseField JOB_ID = new ParseField("jobId");
    private static final ParseField SORT = new ParseField("sort");
    private static final ParseField DESC_ORDER = new ParseField("desc");
    private static final ParseField TAKE = new ParseField("take");
    private static final ParseField SKIP = new ParseField("skip");
    private static final ParseField START = new ParseField("start");
    private static final ParseField END = new ParseField("end");
    private static final ParseField DESCRIPTION = new ParseField("description");

    // Even though these are null, setting up the defaults in case
    // we want to change them later
    private final String DEFAULT_SORT = null;
    private final String DEFAULT_START = null;
    private final String DEFAULT_END = null;
    private final String DEFAULT_DESCRIPTION = null;
    private final boolean DEFAULT_DESC_ORDER = true;
    private final int DEFAULT_SKIP = 0;
    private final int DEFAULT_TAKE = 100;

    private final GetModelSnapshotsAction.TransportAction transportGetModelSnapshotsAction;

    @Inject
    public RestGetModelSnapshotsAction(Settings settings, RestController controller,
            GetModelSnapshotsAction.TransportAction transportGetModelSnapshotsAction) {
        super(settings);
        this.transportGetModelSnapshotsAction = transportGetModelSnapshotsAction;
        controller.registerHandler(RestRequest.Method.GET, "/engine/v2/modelsnapshots/{jobId}", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        GetModelSnapshotsAction.Request getModelSnapshots = new GetModelSnapshotsAction.Request(restRequest.param(JOB_ID.getPreferredName()));
        getModelSnapshots.setSort(restRequest.param(SORT.getPreferredName(), DEFAULT_SORT));
        getModelSnapshots.setStart(restRequest.param(START.getPreferredName(), DEFAULT_START));
        getModelSnapshots.setEnd(restRequest.param(END.getPreferredName(), DEFAULT_END));
        getModelSnapshots.setDescriptionString(restRequest.param(DESCRIPTION.getPreferredName(), DEFAULT_DESCRIPTION));
        getModelSnapshots.setDescOrder(restRequest.paramAsBoolean(DESC_ORDER.getPreferredName(), DEFAULT_DESC_ORDER));
        getModelSnapshots.setPageParams(new PageParams(restRequest.paramAsInt(SKIP.getPreferredName(), DEFAULT_SKIP),
                restRequest.paramAsInt(TAKE.getPreferredName(), DEFAULT_TAKE)));

        return channel -> transportGetModelSnapshotsAction.execute(getModelSnapshots,
                new RestBuilderListener<GetModelSnapshotsAction.Response>(channel) {

            @Override
            public RestResponse buildResponse(GetModelSnapshotsAction.Response response, XContentBuilder builder) throws Exception {
                return new BytesRestResponse(OK, XContentType.JSON.mediaType(), response.getResponse());
            }
        });
    }
}
