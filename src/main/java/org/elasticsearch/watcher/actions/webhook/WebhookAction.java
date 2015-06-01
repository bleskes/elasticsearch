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

package org.elasticsearch.watcher.actions.webhook;

import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.watcher.actions.Action;
import org.elasticsearch.watcher.support.http.HttpRequest;
import org.elasticsearch.watcher.support.http.HttpRequestTemplate;
import org.elasticsearch.watcher.support.http.HttpResponse;

import java.io.IOException;

/**
 *
 */
public class WebhookAction implements Action {

    public static final String TYPE = "webhook";

    final HttpRequestTemplate requestTemplate;

    public WebhookAction(HttpRequestTemplate requestTemplate) {
        this.requestTemplate = requestTemplate;
    }

    @Override
    public String type() {
        return TYPE;
    }

    public HttpRequestTemplate getRequest() {
        return requestTemplate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WebhookAction action = (WebhookAction) o;

        return requestTemplate.equals(action.requestTemplate);
    }

    @Override
    public int hashCode() {
        return requestTemplate.hashCode();
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return requestTemplate.toXContent(builder, params);
    }

    public static WebhookAction parse(String watchId, String actionId, XContentParser parser, HttpRequestTemplate.Parser requestParser) throws IOException {
        try {
            HttpRequestTemplate request = requestParser.parse(parser);
            return new WebhookAction(request);
        } catch (HttpRequestTemplate.ParseException pe) {
            throw new WebhookActionException("could not parse [{}] action [{}/{}]. failed parsing http request template", pe, TYPE, watchId, actionId);
        }
    }

    public static Builder builder(HttpRequestTemplate requestTemplate) {
        return new Builder(requestTemplate);
    }

    public interface Result {

        class Success extends Action.Result implements Result {

            private final HttpRequest request;
            private final HttpResponse response;

            public Success(HttpRequest request, HttpResponse response) {
                super(TYPE, Status.SUCCESS);
                this.request = request;
                this.response = response;
            }

            public HttpResponse response() {
                return response;
            }

            public HttpRequest request() {
                return request;
            }

            @Override
            public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
                return builder.startObject(type)
                        .field(Field.REQUEST.getPreferredName(), request, params)
                        .field(Field.RESPONSE.getPreferredName(), response, params)
                        .endObject();
            }
        }

        class Failure extends Action.Result.Failure implements Result {

            private final HttpRequest request;
            private final HttpResponse response;

            public Failure(HttpRequest request, HttpResponse response) {
                this(request, response, "received [{}] status code", response.status());
            }

            private Failure(HttpRequest request, HttpResponse response, String reason, Object... args) {
                super(TYPE, reason, args);
                this.request = request;
                this.response = response;
            }

            public HttpResponse response() {
                return response;
            }

            public HttpRequest request() {
                return request;
            }

            @Override
            public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
                super.toXContent(builder, params);
                return builder.startObject(type)
                        .field(Field.REQUEST.getPreferredName(), request, params)
                        .field(Field.RESPONSE.getPreferredName(), response, params)
                        .endObject();
            }
        }

        class Simulated extends Action.Result implements Result {

            private final HttpRequest request;

            public Simulated(HttpRequest request) {
                super(TYPE, Status.SIMULATED);
                this.request = request;
            }

            public HttpRequest request() {
                return request;
            }

            @Override
            public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
                return builder.startObject(type)
                        .field(Field.REQUEST.getPreferredName(), request, params)
                        .endObject();
            }
        }

    }

    public static class Builder implements Action.Builder<WebhookAction> {

        final HttpRequestTemplate requestTemplate;

        private Builder(HttpRequestTemplate requestTemplate) {
            this.requestTemplate = requestTemplate;
        }

        @Override
        public WebhookAction build() {
            return new WebhookAction(requestTemplate);
        }
    }

    interface Field extends Action.Field {
        ParseField REQUEST = new ParseField("request");
        ParseField RESPONSE = new ParseField("response");
    }
}
