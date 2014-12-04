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

package org.elasticsearch.alerts.triggers;

import org.elasticsearch.action.search.SearchRequest;

import java.util.Map;

/**
 */
public class TriggerResult {

    private final boolean triggered;
    private boolean throttled;
    private final SearchRequest triggerRequest;
    private final Map<String, Object> triggerResponse;
    private final AlertTrigger trigger;

    private SearchRequest payloadRequest = null;
    private Map<String, Object> payloadResponse = null;

    public TriggerResult(boolean triggered, SearchRequest triggerRequest, Map<String, Object> triggerResponse, AlertTrigger trigger) {
        this.triggered = triggered;
        this.triggerRequest = triggerRequest;
        this.triggerResponse = triggerResponse;
        this.trigger = trigger;
    }

    public boolean isTriggered() {
        return triggered;
    }

    public boolean isThrottled() {
        return throttled;
    }

    public void setThrottled(boolean throttled) {
        this.throttled = throttled;
    }

    /**
     * Get's the request to trigger
     */
    public SearchRequest getTriggerRequest() {
        return triggerRequest;
    }

    /**
     * The response from the trigger request
     * @return
     */
    public Map<String, Object> getTriggerResponse() {
        return triggerResponse;
    }

    /**
     * The request to generate the payloads for the alert actions
     * @return
     */
    public SearchRequest getPayloadRequest() {
        return payloadRequest;
    }

    public void setPayloadRequest(SearchRequest payloadRequest) {
        this.payloadRequest = payloadRequest;
    }

    /**
     * The response from the payload request
     * @return
     */
    public Map<String,Object> getPayloadResponse() {
        return payloadResponse;
    }

    public void setPayloadResponse(Map<String, Object> payloadResponse) {
        this.payloadResponse = payloadResponse;
    }

    public AlertTrigger getTrigger() {
        return trigger;
    }

    /**
     * @return the response the actions should use
     */
    public Map<String, Object> getActionResponse() {
        return payloadResponse != null ? payloadResponse : triggerResponse;
    }

    /**
     * @return the request the actions should use
     */
    public SearchRequest getActionRequest() {
        return payloadRequest != null ? payloadRequest : triggerRequest;
    }

}
