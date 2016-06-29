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

package org.elasticsearch.xpack.monitoring.client;

import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.xpack.monitoring.action.MonitoringBulkAction;
import org.elasticsearch.xpack.monitoring.action.MonitoringBulkRequest;
import org.elasticsearch.xpack.monitoring.action.MonitoringBulkRequestBuilder;
import org.elasticsearch.xpack.monitoring.action.MonitoringBulkResponse;

import java.util.Map;

public class MonitoringClient {

    private final Client client;

    @Inject
    public MonitoringClient(Client client) {
        this.client = client;
    }


    /**
     * Creates a request builder that bulk index monitoring documents.
     *
     * @return The request builder
     */
    public MonitoringBulkRequestBuilder prepareMonitoringBulk() {
        return new MonitoringBulkRequestBuilder(client);
    }

    /**
     * Executes a bulk of index operations that concern monitoring documents.
     *
     * @param request  The monitoring bulk request
     * @param listener A listener to be notified with a result
     */
    public void bulk(MonitoringBulkRequest request, ActionListener<MonitoringBulkResponse> listener) {
        client.execute(MonitoringBulkAction.INSTANCE, request, listener);
    }

    /**
     * Executes a bulk of index operations that concern monitoring documents.
     *
     * @param request  The monitoring bulk request
     */
    public ActionFuture<MonitoringBulkResponse> bulk(MonitoringBulkRequest request) {
        return client.execute(MonitoringBulkAction.INSTANCE, request);
    }

    public MonitoringClient filterWithHeader(Map<String, String> headers) {
        return new MonitoringClient(client.filterWithHeader(headers));
    }
}
