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

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.Base64;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.base.Charsets;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;
import java.net.HttpURLConnection;

/**
 */
public class BasicAuth extends HttpAuth {

    public static final String TYPE = "basic";

    private final String username;
    private final String password;

    private final String basicAuth;

    public BasicAuth(String username, String password) {
        this.username = username;
        this.password = password;
        basicAuth = "Basic " + Base64.encodeBytes((username + ":" + password).getBytes(Charsets.UTF_8));
    }

    public String type() {
        return TYPE;
    }

    @Override
    public XContentBuilder innerToXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(Parser.USERNAME_FIELD.getPreferredName(), username);
        builder.field(Parser.PASSWORD_FIELD.getPreferredName(), password);
        return builder.endObject();
    }

    public void update(HttpURLConnection connection) {
        connection.setRequestProperty("Authorization", basicAuth);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BasicAuth basicAuth = (BasicAuth) o;

        if (!password.equals(basicAuth.password)) return false;
        if (!username.equals(basicAuth.username)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = username.hashCode();
        result = 31 * result + password.hashCode();
        return result;
    }

    public static class Parser implements HttpAuth.Parser<BasicAuth> {

        static final ParseField USERNAME_FIELD = new ParseField("username");
        static final ParseField PASSWORD_FIELD = new ParseField("password");

        public String type() {
            return TYPE;
        }

        public BasicAuth parse(XContentParser parser) throws IOException {
            String username = null;
            String password = null;

            String fieldName = null;
            XContentParser.Token token;
            while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                if (token == XContentParser.Token.FIELD_NAME) {
                    fieldName = parser.currentName();
                } else if (token == XContentParser.Token.VALUE_STRING) {
                    if (USERNAME_FIELD.getPreferredName().equals(fieldName)) {
                        username = parser.text();
                    } else if (PASSWORD_FIELD.getPreferredName().equals(fieldName)) {
                        password = parser.text();
                    } else {
                        throw new ElasticsearchParseException("unsupported field [" + fieldName + "]");
                    }
                } else {
                    throw new ElasticsearchParseException("unsupported token [" + token + "]");
                }
            }

            if (username == null) {
                throw new HttpAuthException("username is a required option");
            }
            if (password == null) {
                throw new HttpAuthException("password is a required option");
            }

            return new BasicAuth(username, password);
        }
    }
}
