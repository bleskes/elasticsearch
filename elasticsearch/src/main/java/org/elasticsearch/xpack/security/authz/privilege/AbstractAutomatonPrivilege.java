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

import dk.brics.automaton.Automaton;
import dk.brics.automaton.BasicOperations;
import org.elasticsearch.xpack.security.support.AutomatonPredicate;
import org.elasticsearch.xpack.security.support.Automatons;

import java.util.function.Predicate;

import static org.elasticsearch.xpack.security.support.Automatons.patterns;

/**
 *
 */
@SuppressWarnings("unchecked")
abstract class AbstractAutomatonPrivilege<P extends AbstractAutomatonPrivilege<P>> extends Privilege<P> {

    protected final Automaton automaton;

    AbstractAutomatonPrivilege(String name, String... patterns) {
        super(new Name(name));
        this.automaton = patterns(patterns);
    }

    AbstractAutomatonPrivilege(Name name, String... patterns) {
        super(name);
        this.automaton = patterns(patterns);
    }

    AbstractAutomatonPrivilege(Name name, Automaton automaton) {
        super(name);
        this.automaton = automaton;
    }

    @Override
    public Predicate<String> predicate() {
        return new AutomatonPredicate(automaton);
    }

    protected P plus(P other) {
        if (other.implies((P) this)) {
            return other;
        }
        if (this.implies(other)) {
            return (P) this;
        }
        return create(name.add(other.name), Automatons.unionAndDeterminize(automaton, other.automaton));
    }

    protected P minus(P other) {
        if (other.implies((P) this)) {
            return none();
        }
        if (other == none() || !this.implies(other)) {
            return (P) this;
        }
        return create(name.remove(other.name), Automatons.minusAndDeterminize(automaton, other.automaton));
    }

    @Override
    public boolean implies(P other) {
        return BasicOperations.subsetOf(other.automaton, automaton);
    }

    @Override
    public String toString() {
        return name.toString();
    }

    protected abstract P create(Name name, Automaton automaton);

    protected abstract P none();


}
