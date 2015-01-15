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


package org.elasticsearch.alerts.actions;

import org.elasticsearch.common.netty.handler.codec.http.HttpMethod;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;

/**
 */
public class WebhookAlertAction implements AlertAction {

    private final String url;
    private final HttpMethod method;
    private final String parameterString;

    public WebhookAlertAction(String url, HttpMethod method, String parameterString) {
        this.url = url;
        this.method = method;
        this.parameterString = parameterString;
    }

    public String getUrl() {
        return url;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public String getParameterString() {
        return parameterString;
    }

    /**
     */
    @Override
    public String getActionName() {
        return "webhook";
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field("url", url);
        builder.field("method", method.getName());
        builder.field("parameter_string", parameterString);
        builder.endObject();
        return builder;
    }

    @Override
    public String toString() {
        return "WebhookAlertAction{" +
                "url='" + url + '\'' +
                ", method=" + method +
                ", parameterString='" + parameterString + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WebhookAlertAction that = (WebhookAlertAction) o;

        if (method != null ? !method.equals(that.method) : that.method != null) return false;
        if (parameterString != null ? !parameterString.equals(that.parameterString) : that.parameterString != null)
            return false;
        if (url != null ? !url.equals(that.url) : that.url != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = url != null ? url.hashCode() : 0;
        result = 31 * result + (method != null ? method.hashCode() : 0);
        result = 31 * result + (parameterString != null ? parameterString.hashCode() : 0);
        return result;
    }
}
