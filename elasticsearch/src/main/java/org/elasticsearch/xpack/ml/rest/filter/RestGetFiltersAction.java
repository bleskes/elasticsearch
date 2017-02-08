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
package org.elasticsearch.xpack.ml.rest.filter;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.RestStatusToXContentListener;
import org.elasticsearch.xpack.ml.MlPlugin;
import org.elasticsearch.xpack.ml.action.GetFiltersAction;
import org.elasticsearch.xpack.ml.action.util.PageParams;
import org.elasticsearch.xpack.ml.job.config.MlFilter;

import java.io.IOException;

public class RestGetFiltersAction extends BaseRestHandler {

    public RestGetFiltersAction(Settings settings, RestController controller) {
        super(settings);
        controller.registerHandler(RestRequest.Method.GET, MlPlugin.BASE_PATH + "filters/{" + MlFilter.ID.getPreferredName() + "}",
                this);
        controller.registerHandler(RestRequest.Method.GET, MlPlugin.BASE_PATH + "filters/", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        GetFiltersAction.Request getListRequest = new GetFiltersAction.Request();
        String filterId = restRequest.param(MlFilter.ID.getPreferredName());
        if (!Strings.isNullOrEmpty(filterId)) {
            getListRequest.setFilterId(filterId);
        }
        if (restRequest.hasParam(PageParams.FROM.getPreferredName())
                || restRequest.hasParam(PageParams.SIZE.getPreferredName())
                || Strings.isNullOrEmpty(filterId)) {
            getListRequest.setPageParams(new PageParams(restRequest.paramAsInt(PageParams.FROM.getPreferredName(), PageParams.DEFAULT_FROM),
                    restRequest.paramAsInt(PageParams.SIZE.getPreferredName(), PageParams.DEFAULT_SIZE)));
        }
        return channel -> client.execute(GetFiltersAction.INSTANCE, getListRequest, new RestStatusToXContentListener<>(channel));
    }

}
