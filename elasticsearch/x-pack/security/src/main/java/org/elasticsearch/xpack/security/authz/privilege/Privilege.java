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

package org.elasticsearch.xpack.security.authz.privilege;

import org.elasticsearch.common.Strings;
import org.elasticsearch.common.util.set.Sets;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import static java.util.Collections.singleton;
import static java.util.Collections.unmodifiableSet;
import static org.elasticsearch.common.util.set.Sets.newHashSet;

/**
 *
 */
public abstract class Privilege<P extends Privilege<P>> {

    protected final Name name;

    Privilege(Name name) {
        this.name = name;
    }

    public Name name() {
        return name;
    }

    public abstract Predicate<String> predicate();

    public abstract boolean implies(P other);

    @SuppressWarnings("unchecked")
    public boolean isAlias(P other) {
        return this.implies(other) && other.implies((P) this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Privilege privilege = (Privilege) o;

        if (name != null ? !name.equals(privilege.name) : privilege.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    static String actionToPattern(String text) {
        return text + "*";
    }

    public static class Name {

        public static final Name NONE = new Name("none");
        public static final Name ALL = new Name("all");

        final Set<String> parts;

        public Name(String name) {
            assert name != null && !name.contains(",");
            parts = singleton(name);
        }

        public Name(Set<String> parts) {
            assert !parts.isEmpty();
            this.parts = unmodifiableSet(new HashSet<>(parts));
        }

        public Name(String... parts) {
            this(unmodifiableSet(newHashSet(parts)));
        }

        @Override
        public String toString() {
            return Strings.collectionToCommaDelimitedString(parts);
        }

        public Name add(Name other) {
            return new Name(Sets.union(parts, other.parts));
        }

        public Name remove(Name other) {
            Set<String> parts = Sets.difference(this.parts, other.parts);
            return parts.isEmpty() ? NONE : new Name(parts);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Name name = (Name) o;

            return parts.equals(name.parts);
        }

        @Override
        public int hashCode() {
            return parts.hashCode();
        }
    }
}
