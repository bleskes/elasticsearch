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
package org.elasticsearch.xpack.prelert.action.job;

import org.elasticsearch.action.Action;
import org.elasticsearch.client.ElasticsearchClient;

public class GetJobsAction extends Action<GetJobsRequest, GetJobsResponse, GetJobsRequestBuilder> {

    public static final GetJobsAction INSTANCE = new GetJobsAction();
    public static final String NAME = "cluster:admin/prelert/jobs/get";

    public GetJobsAction() {
        super(NAME);
    }

    @Override
    public GetJobsRequestBuilder newRequestBuilder(ElasticsearchClient client) {
        return new GetJobsRequestBuilder(client, this);
    }

    @Override
    public GetJobsResponse newResponse() {
        return new GetJobsResponse();
    }
}
