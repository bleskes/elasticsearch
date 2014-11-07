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
import org.elasticsearch.action.search.SearchResponse;

/**
 */
public class TriggerResult {

    private final boolean triggered;
    private final SearchRequest request;
    private final SearchResponse response;
    private final AlertTrigger trigger;

    public TriggerResult(boolean triggered, SearchRequest request, SearchResponse response, AlertTrigger trigger) {
        this.triggered = triggered;
        this.request = request;
        this.response = response;
        this.trigger = trigger;
    }

    public boolean isTriggered() {
        return triggered;
    }

    public SearchRequest getRequest() {
        return request;
    }

    public SearchResponse getResponse() {
        return response;
    }

    public AlertTrigger getTrigger() {
        return trigger;
    }

}
