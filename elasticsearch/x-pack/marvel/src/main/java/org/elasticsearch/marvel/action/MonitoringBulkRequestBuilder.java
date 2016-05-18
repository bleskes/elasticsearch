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

package org.elasticsearch.marvel.action;

import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.client.ElasticsearchClient;
import org.elasticsearch.common.bytes.BytesReference;

public class MonitoringBulkRequestBuilder
        extends ActionRequestBuilder<MonitoringBulkRequest, MonitoringBulkResponse, MonitoringBulkRequestBuilder> {

    public MonitoringBulkRequestBuilder(ElasticsearchClient client) {
        super(client, MonitoringBulkAction.INSTANCE, new MonitoringBulkRequest());
    }

    public MonitoringBulkRequestBuilder add(MonitoringBulkDoc doc) {
        request.add(doc);
        return this;
    }

    public MonitoringBulkRequestBuilder add(BytesReference content, String defaultId, String defaultVersion, String defaultType)
            throws Exception {
        request.add(content, defaultId, defaultVersion, defaultType);
        return this;
    }
}
