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

package org.elasticsearch.shield.support;

import org.apache.lucene.util.automaton.Automata;
import org.apache.lucene.util.automaton.Automaton;
import org.apache.lucene.util.automaton.RegExp;

import java.util.Collection;

import static org.apache.lucene.util.automaton.MinimizationOperations.minimize;
import static org.apache.lucene.util.automaton.Operations.*;

/**
 *
 */
public final class Automatons {

    private Automatons() {
    }

    public static Automaton patterns(String... patterns) {
        if (patterns.length == 0) {
            return Automata.makeEmpty();
        }
        Automaton automaton = new RegExp(patterns[0]).toAutomaton();
        for (String pattern : patterns) {
            automaton = union(automaton, new RegExp(pattern).toAutomaton());
        }
        return determinize(minimize(automaton));
    }

    public static Automaton patterns(Collection<String> patterns) {
        if (patterns.isEmpty()) {
            return Automata.makeEmpty();
        }
        Automaton automaton = null;
        for (String pattern : patterns) {
            if (automaton == null) {
                automaton = new RegExp(pattern).toAutomaton();
            } else {
                automaton = union(automaton, new RegExp(pattern).toAutomaton());
            }
        }
        return determinize(minimize(automaton));
    }

    public static Automaton unionAndDeterminize(Automaton a1, Automaton a2) {
        return determinize(union(a1, a2));
    }

    public static Automaton minusAndDeterminize(Automaton a1, Automaton a2) {
        return determinize(minus(a1, a2));
    }
}
