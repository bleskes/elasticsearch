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

package org.elasticsearch.watcher.transport.actions.execute;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.master.MasterNodeOperationRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.watcher.client.WatcherClient;
import org.elasticsearch.watcher.trigger.TriggerEvent;

import java.io.IOException;
import java.util.Map;

/**
 * A execute watch action request builder.
 */
public class ExecuteWatchRequestBuilder extends MasterNodeOperationRequestBuilder<ExecuteWatchRequest, ExecuteWatchResponse, ExecuteWatchRequestBuilder, Client> {

    public ExecuteWatchRequestBuilder(Client client) {
        super(client, new ExecuteWatchRequest());
    }

    public ExecuteWatchRequestBuilder(Client client, String watchName) {
        super(client, new ExecuteWatchRequest(watchName));
    }

    /**
     * Sets the id of the watch to be executed
     */
    public ExecuteWatchRequestBuilder setId(String id) {
        this.request().setId(id);
        return this;
    }
    /**
    * @param ignoreCondition set if the condition for this execution be ignored
    */
    public ExecuteWatchRequestBuilder setIgnoreCondition(boolean ignoreCondition) {
        request.setIgnoreCondition(ignoreCondition);
        return this;
    }

    /**
     * @param ignoreThrottle Sets if the throttle should be ignored for this execution
     */
    public ExecuteWatchRequestBuilder setIgnoreThrottle(boolean ignoreThrottle) {
        request.setIgnoreThrottle(ignoreThrottle);
        return this;
    }

    /**
     * @param recordExecution Sets if this execution be recorded in the history index and reflected in the watch
     */
    public ExecuteWatchRequestBuilder setRecordExecution(boolean recordExecution) {
        request.setRecordExecution(recordExecution);
        return this;
    }

    /**
     * @param alternativeInput Set's the alernative input
     */
    public ExecuteWatchRequestBuilder setAlternativeInput(Map<String, Object> alternativeInput) {
        request.setAlternativeInput(alternativeInput);
        return this;
    }

    /**
     * @param triggerType the trigger type to use
     * @param triggerSource the trigger source to use
     */
    public ExecuteWatchRequestBuilder setTriggerEvent(String triggerType, BytesReference triggerSource) {
        request.setTriggerEvent(triggerType, triggerSource);
        return this;
    }

    /**
     * @param triggerEvent the trigger event to use
     */
    public ExecuteWatchRequestBuilder setTriggerEvent(TriggerEvent triggerEvent) throws IOException {
        request.setTriggerEvent(triggerEvent);
        return this;
    }

    /**
     * @param simulatedActionIds a list of action ids to run in simulations for this execution
     */
    public ExecuteWatchRequestBuilder addSimulatedActions(String ... simulatedActionIds) {
        request.addSimulatedActions(simulatedActionIds);
        return this;
    }

    @Override
    protected void doExecute(final ActionListener<ExecuteWatchResponse> listener) {
        new WatcherClient(client).executeWatch(request, listener);
    }

}
