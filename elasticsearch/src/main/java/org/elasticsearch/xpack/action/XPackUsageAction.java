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

package org.elasticsearch.xpack.action;

import org.elasticsearch.action.Action;
import org.elasticsearch.client.ElasticsearchClient;

/**
 *
 */
public class XPackUsageAction extends Action<XPackUsageRequest, XPackUsageResponse, XPackUsageRequestBuilder> {

    public static final String NAME = "cluster:monitor/xpack/usage";
    public static final XPackUsageAction INSTANCE = new XPackUsageAction();

    public XPackUsageAction() {
        super(NAME);
    }

    @Override
    public XPackUsageRequestBuilder newRequestBuilder(ElasticsearchClient client) {
        return new XPackUsageRequestBuilder(client);
    }

    @Override
    public XPackUsageResponse newResponse() {
        return new XPackUsageResponse();
    }
}
