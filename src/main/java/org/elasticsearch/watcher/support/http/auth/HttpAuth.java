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

package org.elasticsearch.watcher.support.http.auth;

import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;
import java.net.HttpURLConnection;

public abstract class HttpAuth implements ToXContent {

    public abstract String type();

    public abstract void update(HttpURLConnection connection);

    @Override
    public final XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(type());
        builder = innerToXContent(builder, params);
        return builder.endObject();
    }

    public abstract XContentBuilder innerToXContent(XContentBuilder builder, Params params) throws IOException;

    public static interface Parser<Auth extends HttpAuth> {

        String type();

        Auth parse(XContentParser parser) throws IOException;

    }

}
