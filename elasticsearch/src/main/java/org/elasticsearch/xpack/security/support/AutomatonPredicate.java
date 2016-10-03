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

package org.elasticsearch.xpack.security.support;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.RunAutomaton;

import java.util.function.Predicate;

/**
*
*/
public class AutomatonPredicate implements Predicate<String> {

    private final RunAutomaton automaton;

    public AutomatonPredicate(Automaton automaton) {
        this(new RunAutomaton(automaton, false));
    }

    public AutomatonPredicate(RunAutomaton automaton) {
        this.automaton = automaton;
    }

    @Override
    public boolean test(String input) {
        return automaton.run(input);
    }
}
