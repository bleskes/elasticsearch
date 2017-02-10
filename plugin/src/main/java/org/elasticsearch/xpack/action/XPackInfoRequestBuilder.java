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

import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.client.ElasticsearchClient;
import org.elasticsearch.license.XPackInfoResponse;

import java.util.EnumSet;

/**
 */
public class XPackInfoRequestBuilder extends ActionRequestBuilder<XPackInfoRequest, XPackInfoResponse, XPackInfoRequestBuilder> {

    public XPackInfoRequestBuilder(ElasticsearchClient client) {
        this(client, XPackInfoAction.INSTANCE);
    }

    public XPackInfoRequestBuilder(ElasticsearchClient client, XPackInfoAction action) {
        super(client, action, new XPackInfoRequest());
    }

    public XPackInfoRequestBuilder setVerbose(boolean verbose) {
        request.setVerbose(verbose);
        return this;
    }


    public XPackInfoRequestBuilder setCategories(EnumSet<XPackInfoRequest.Category> categories) {
        request.setCategories(categories);
        return this;
    }

}
