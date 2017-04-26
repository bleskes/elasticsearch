/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2017] Elasticsearch Incorporated. All Rights Reserved.
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

package org.elasticsearch.xpack.security.action.token;

import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.ToXContentObject;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.Objects;

/**
 * Response containing the token string that was generated from a token creation request. This
 * object also contains the scope and expiration date. If the scope was not provided or if the
 * provided scope matches the scope of the token, then the scope value is <code>null</code>
 */
public final class CreateTokenResponse extends ActionResponse implements ToXContentObject {

    private String tokenString;
    private TimeValue expiresIn;
    private String scope;

    CreateTokenResponse() {}

    public CreateTokenResponse(String tokenString, TimeValue expiresIn, String scope) {
        this.tokenString = Objects.requireNonNull(tokenString);
        this.expiresIn = Objects.requireNonNull(expiresIn);
        this.scope = scope;
    }

    public String getTokenString() {
        return tokenString;
    }

    public String getScope() {
        return scope;
    }

    public TimeValue getExpiresIn() {
        return expiresIn;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(tokenString);
        expiresIn.writeTo(out);
        out.writeOptionalString(scope);
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        tokenString = in.readString();
        expiresIn = new TimeValue(in);
        scope = in.readOptionalString();
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject()
            .field("access_token", tokenString)
            .field("type", "Bearer")
            .field("expires_in", expiresIn.seconds());
        // only show the scope if it is not null
        if (scope != null) {
            builder.field("scope", scope);
        }
        return builder.endObject();
    }
}
