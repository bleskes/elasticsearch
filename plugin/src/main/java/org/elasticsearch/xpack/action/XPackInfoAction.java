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
import org.elasticsearch.license.XPackInfoResponse;

/**
 *
 */
public class XPackInfoAction extends Action<XPackInfoRequest, XPackInfoResponse, XPackInfoRequestBuilder> {

    public static final String NAME = "cluster:monitor/xpack/info";
    public static final XPackInfoAction INSTANCE = new XPackInfoAction();

    public XPackInfoAction() {
        super(NAME);
    }

    @Override
    public XPackInfoRequestBuilder newRequestBuilder(ElasticsearchClient client) {
        return new XPackInfoRequestBuilder(client);
    }

    @Override
    public XPackInfoResponse newResponse() {
        return new XPackInfoResponse();
    }
}
