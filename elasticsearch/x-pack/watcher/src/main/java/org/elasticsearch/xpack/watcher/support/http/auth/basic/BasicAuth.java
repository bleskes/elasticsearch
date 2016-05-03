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

package org.elasticsearch.xpack.watcher.support.http.auth.basic;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.watcher.support.http.auth.HttpAuth;
import org.elasticsearch.xpack.watcher.support.secret.Secret;
import org.elasticsearch.xpack.watcher.support.xcontent.WatcherParams;
import org.elasticsearch.xpack.watcher.support.xcontent.WatcherXContentParser;

import java.io.IOException;

/**
 *
 */
public class BasicAuth implements HttpAuth {

    public static final String TYPE = "basic";

    final String username;
    final Secret password;

    public BasicAuth(String username, char[] password) {
        this(username, new Secret(password));
    }

    public BasicAuth(String username, Secret password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public String type() {
        return TYPE;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BasicAuth basicAuth = (BasicAuth) o;

        if (!username.equals(basicAuth.username)) return false;
        return password.equals(basicAuth.password);
    }

    @Override
    public int hashCode() {
        int result = username.hashCode();
        result = 31 * result + password.hashCode();
        return result;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(Field.USERNAME.getPreferredName(), username);
        if (!WatcherParams.hideSecrets(params)) {
            builder.field(Field.PASSWORD.getPreferredName(), password, params);
        }
        return builder.endObject();
    }

    public static BasicAuth parse(XContentParser parser) throws IOException {
        String username = null;
        Secret password = null;

        String fieldName = null;
        XContentParser.Token token;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                fieldName = parser.currentName();
            } else if (token == XContentParser.Token.VALUE_STRING) {
                if (Field.USERNAME.getPreferredName().equals(fieldName)) {
                    username = parser.text();
                } else if (Field.PASSWORD.getPreferredName().equals(fieldName)) {
                    password = WatcherXContentParser.secret(parser);
                } else {
                    throw new ElasticsearchParseException("unsupported field [" + fieldName + "]");
                }
            } else {
                throw new ElasticsearchParseException("unsupported token [" + token + "]");
            }
        }

        if (username == null) {
            throw new ElasticsearchParseException("username is a required option");
        }
        if (password == null) {
            throw new ElasticsearchParseException("password is a required option");
        }

        return new BasicAuth(username, password);
    }

    interface Field {
        ParseField USERNAME = new ParseField("username");
        ParseField PASSWORD = new ParseField("password");
    }
}
