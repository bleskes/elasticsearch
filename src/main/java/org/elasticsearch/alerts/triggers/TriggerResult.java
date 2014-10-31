package org.elasticsearch.alerts.triggers;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;

/**
 */
public class TriggerResult {

    private final boolean triggered;
    private final SearchRequest request;
    private final SearchResponse response;

    public TriggerResult(boolean triggered, SearchRequest request, SearchResponse response) {
        this.triggered = triggered;
        this.request = request;
        this.response = response;
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
}
