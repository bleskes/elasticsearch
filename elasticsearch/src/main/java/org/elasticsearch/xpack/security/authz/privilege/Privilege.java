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

import org.apache.lucene.util.automaton.Automaton;
import org.elasticsearch.xpack.security.support.Automatons;

import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;

import static org.elasticsearch.xpack.security.support.Automatons.patterns;

public class Privilege {

    public static final Privilege NONE = new Privilege(Collections.singleton("none"), Automatons.EMPTY);
    public static final Privilege ALL = new Privilege(Collections.singleton("all"), Automatons.MATCH_ALL);

    protected final Set<String> name;
    protected final Automaton automaton;
    protected final Predicate<String> predicate;

    public Privilege(String name, String... patterns) {
        this(Collections.singleton(name), patterns);
    }

    public Privilege(Set<String> name, String... patterns) {
        this(name, patterns(patterns));
    }

    public Privilege(Set<String> name, Automaton automaton) {
        this.name = name;
        this.automaton = automaton;
        this.predicate = Automatons.predicate(automaton);
    }

    public Set<String> name() {
        return name;
    }

    public Predicate<String> predicate() {
        return predicate;
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

    @Override
    public String toString() {
        return name.toString();
    }

    public Automaton getAutomaton() {
        return automaton;
    }
}
