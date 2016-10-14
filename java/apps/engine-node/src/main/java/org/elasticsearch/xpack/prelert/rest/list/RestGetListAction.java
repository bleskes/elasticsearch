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

package org.elasticsearch.xpack.prelert.rest.list;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.RestStatusToXContentListener;
import org.elasticsearch.xpack.prelert.action.GetListAction;

import java.io.IOException;

public class RestGetListAction extends BaseRestHandler {

    private final GetListAction.TransportAction transportGetListAction;

    @Inject
    public RestGetListAction(Settings settings, RestController controller, GetListAction.TransportAction transportGetListAction) {
        super(settings);
        this.transportGetListAction = transportGetListAction;
        controller.registerHandler(RestRequest.Method.GET, "/engine/v2/lists/{listId}", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        GetListAction.Request getListRequest = new GetListAction.Request();
        getListRequest.setListId(restRequest.param("listId"));
        return channel -> transportGetListAction.execute(getListRequest, new RestStatusToXContentListener<>(channel));
    }

}
