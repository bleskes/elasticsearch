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

package org.elasticsearch.alerts.client;

import org.elasticsearch.action.*;
import org.elasticsearch.alerts.transport.actions.index.IndexAlertRequest;
import org.elasticsearch.alerts.transport.actions.index.IndexAlertRequestBuilder;
import org.elasticsearch.alerts.transport.actions.index.IndexAlertResponse;
import org.elasticsearch.alerts.transport.actions.delete.DeleteAlertRequest;
import org.elasticsearch.alerts.transport.actions.delete.DeleteAlertRequestBuilder;
import org.elasticsearch.alerts.transport.actions.delete.DeleteAlertResponse;
import org.elasticsearch.alerts.transport.actions.get.GetAlertRequest;
import org.elasticsearch.alerts.transport.actions.get.GetAlertRequestBuilder;
import org.elasticsearch.alerts.transport.actions.get.GetAlertResponse;
import org.elasticsearch.client.ElasticsearchClient;

/**
 */
public interface AlertsClientInterface extends ElasticsearchClient<AlertsClientInterface> {

    /**
     * Creates a request builder that gets an alert by name (id)
     *
     * @param alertName the name (id) of the alert
     * @return The request builder
     */
    GetAlertRequestBuilder prepareGetAlert(String alertName);

    /**
     * Creates a request builder that gets an alert
     *
     * @return the request builder
     */
    GetAlertRequestBuilder prepareGetAlert();

    /**
     * Gets an alert from the alert index
     *
     * @param request The get alert request
     * @param listener The listener for the get alert response containing the GetResponse for this alert
     */
    public void getAlert(GetAlertRequest request, ActionListener<GetAlertResponse> listener);

    /**
     * Gets an alert from the alert index
     *
     * @param request The get alert request with the alert name (id)
     * @return The response containing the GetResponse for this alert
     */
    ActionFuture<GetAlertResponse> getAlert(GetAlertRequest request);

    /**
     * Creates a request builder to delete an alert by name (id)
     *
     * @param alertName the name (id) of the alert
     * @return The request builder
     */
    DeleteAlertRequestBuilder prepareDeleteAlert(String alertName);

    /**
     * Creates a request builder that deletes an alert
     *
     * @return The request builder
     */
    DeleteAlertRequestBuilder prepareDeleteAlert();

    /**
     * Deletes an alert
     *
     * @param request The delete request with the alert name (id) to be deleted
     * @param listener The listener for the delete alert response containing the DeleteResponse for this action
     */
    public void deleteAlert(DeleteAlertRequest request, ActionListener<DeleteAlertResponse> listener);

    /**
     * Deletes an alert
     *
     * @param request The delete request with the alert name (id) to be deleted
     * @return The response containing the DeleteResponse for this action
     */
    ActionFuture<DeleteAlertResponse> deleteAlert(DeleteAlertRequest request);

    /**
     * Creates a request builder to build a request to index an alert
     *
     * @param alertName The name of the alert to index
     * @return The builder to create the alert
     */
    IndexAlertRequestBuilder prepareIndexAlert(String alertName);

    /**
     * Creates a request builder to build a request to index an alert
     *
     * @return The builder
     */
    IndexAlertRequestBuilder prepareIndexAlert();

    /**
     * Indexes an alert and registers it with the scheduler
     *
     * @param request The request containing the alert to index and register
     * @param listener The listener for the response containing the IndexResponse for this alert
     */
    public void indexAlert(IndexAlertRequest request, ActionListener<IndexAlertResponse> listener);

    /**
     * Indexes an alert and registers it with the scheduler
     *
     * @param request The request containing the alert to index and register
     * @return The response containing the IndexResponse for this alert
     */
    ActionFuture<IndexAlertResponse> indexAlert(IndexAlertRequest request);

}
