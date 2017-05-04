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

import java.util.Map;
import java.util.function.Predicate;

import org.elasticsearch.common.io.stream.NamedWriteable;
import org.elasticsearch.common.xcontent.ToXContentObject;

/**
 * Implementations of this interface represent an expression over a simple object that resolves to
 * a boolean value. The "simple object" is implemented as a (flattened) {@link Map}.
 */
public interface RoleMapperExpression extends ToXContentObject, NamedWriteable {

    /**
     * Determines whether this expression matches against the provided object.
     */
    boolean match(Map<String, Object> object);

    /**
     * Adapt this expression to a standard {@link Predicate}
     */
    default Predicate<Map<String, Object>> asPredicate() {
        return this::match;
    }

    /**
     * Creates an <em>inverted</em> predicate that can test whether an expression matches
     * a fixed object. Its purpose is for cases where there is a {@link java.util.stream.Stream} of
     * expressions, that need to be filtered against a single map.
     */
    static Predicate<RoleMapperExpression> predicate(Map<String, Object> map) {
        return expr -> expr.match(map);
    }

}
