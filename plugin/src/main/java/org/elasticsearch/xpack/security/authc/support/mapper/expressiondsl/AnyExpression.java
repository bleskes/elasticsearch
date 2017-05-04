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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.XContentBuilder;

/**
 * An expression that evaluates to <code>true</code> if at least one of its children
 * evaluate to <code>true</code>.
 * An <em>any</em> expression with no children is never <code>true</code>.
 */
public final class AnyExpression implements RoleMapperExpression {

    static final String NAME = "any";

    private final List<RoleMapperExpression> elements;

    AnyExpression(List<RoleMapperExpression> elements) {
        assert elements != null;
        this.elements = elements;
    }

    AnyExpression(StreamInput in) throws IOException {
        this(ExpressionParser.readExpressionList(in));
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        ExpressionParser.writeExpressionList(elements, out);
    }

    @Override
    public String getWriteableName() {
        return NAME;
    }

    @Override
    public boolean match(Map<String, Object> object) {
        return elements.stream().anyMatch(RoleMapperExpression.predicate(object));
    }

    public List<RoleMapperExpression> getElements() {
        return Collections.unmodifiableList(elements);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final AnyExpression that = (AnyExpression) o;
        return this.elements.equals(that.elements);
    }

    @Override
    public int hashCode() {
        return elements.hashCode();
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.startArray(ExpressionParser.Fields.ANY.getPreferredName());
        for (RoleMapperExpression e : elements) {
            e.toXContent(builder, params);
        }
        builder.endArray();
        return builder.endObject();
    }

}
