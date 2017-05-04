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

package org.elasticsearch.xpack.security.authc.support.mapper.expressiondsl;

import java.io.IOException;
import java.util.Map;

import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.XContentBuilder;

/**
 * A negating expression. That is, this expression evaluates to <code>true</code> if-and-only-if
 * its delegate expression evaluate to <code>false</code>.
 * Syntactically, <em>except</em> expressions are intended to be children of <em>all</em>
 * expressions ({@link AllExpression}).
 */
public final class ExceptExpression implements RoleMapperExpression {

    static final String NAME = "except";

    private final RoleMapperExpression expression;

    ExceptExpression(RoleMapperExpression expression) {
        assert expression != null;
        this.expression = expression;
    }

    ExceptExpression(StreamInput in) throws IOException {
        this(ExpressionParser.readExpression(in));
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        ExpressionParser.writeExpression(expression, out);
    }

    @Override
    public String getWriteableName() {
        return NAME;
    }

    @Override
    public boolean match(Map<String, Object> object) {
        return !expression.match(object);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ExceptExpression that = (ExceptExpression) o;
        return this.expression.equals(that.expression);
    }

    @Override
    public int hashCode() {
        return expression.hashCode();
    }

    RoleMapperExpression getInnerExpression() {
        return expression;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(ExpressionParser.Fields.EXCEPT.getPreferredName());
        expression.toXContent(builder, params);
        return builder.endObject();
    }
}
