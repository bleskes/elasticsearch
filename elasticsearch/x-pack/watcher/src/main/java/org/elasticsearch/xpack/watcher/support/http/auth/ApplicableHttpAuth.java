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

package org.elasticsearch.xpack.watcher.support.http.auth;

import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.net.HttpURLConnection;

public abstract class ApplicableHttpAuth<Auth extends HttpAuth> implements ToXContent {

    private final Auth auth;

    public ApplicableHttpAuth(Auth auth) {
        this.auth = auth;
    }

    public final String type() {
        return auth.type();
    }

    public abstract void apply(HttpURLConnection connection);

    @Override
    public final XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return auth.toXContent(builder, params);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ApplicableHttpAuth<?> that = (ApplicableHttpAuth<?>) o;

        return auth.equals(that.auth);
    }

    @Override
    public int hashCode() {
        return auth.hashCode();
    }
}
