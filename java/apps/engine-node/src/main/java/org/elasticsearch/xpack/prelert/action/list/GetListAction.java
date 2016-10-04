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

package org.elasticsearch.xpack.prelert.action.list;

import org.elasticsearch.action.Action;
import org.elasticsearch.client.ElasticsearchClient;


public class GetListAction extends Action<GetListRequest, GetListResponse, GetListRequestBuilder> {

    public static final GetListAction INSTANCE = new GetListAction();
    public static final String NAME = "cluster:admin/prelert/list/get";

    public GetListAction() {
        super(NAME);
    }

    @Override
    public GetListRequestBuilder newRequestBuilder(ElasticsearchClient client) {
        return new GetListRequestBuilder(client, this);
    }

    @Override
    public GetListResponse newResponse() {
        return new GetListResponse();
    }
}

