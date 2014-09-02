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

import org.apache.lucene.util.automaton.Automaton;
import org.apache.lucene.util.automaton.BasicAutomata;
import org.apache.lucene.util.automaton.MinimizationOperations;
import org.apache.lucene.util.automaton.RegExp;

import java.util.Collection;

/**
 *
 */
public final class Automatons {

    private Automatons() {
    }

    public static Automaton patterns(String... patterns) {
        if (patterns.length == 0) {
            return BasicAutomata.makeEmpty();
        }
        Automaton automaton = new RegExp(patterns[0]).toAutomaton();
        for (String pattern : patterns) {
            automaton = automaton.union(new RegExp(pattern).toAutomaton());
        }
        MinimizationOperations.minimize(automaton);
        return automaton;
    }

    public static Automaton patterns(Collection<String> patterns) {
        if (patterns.isEmpty()) {
            return BasicAutomata.makeEmpty();
        }
        Automaton automaton = null;
        for (String pattern : patterns) {
            if (automaton == null) {
                automaton = new RegExp(pattern).toAutomaton();
            } else {
                automaton = automaton.union(new RegExp(pattern).toAutomaton());
            }
        }
        MinimizationOperations.minimize(automaton);
        return automaton;
    }
}
