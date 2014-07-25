package org.elasticsearch.shield.support;

import org.apache.lucene.util.automaton.Automaton;
import org.apache.lucene.util.automaton.CharacterRunAutomaton;
import org.elasticsearch.common.base.Predicate;

/**
*
*/
public class AutomatonPredicate implements Predicate<String> {

    private final CharacterRunAutomaton automaton;

    public AutomatonPredicate(Automaton automaton) {
        this.automaton = new CharacterRunAutomaton(automaton);
    }

    public AutomatonPredicate(CharacterRunAutomaton automaton) {
        this.automaton = automaton;
    }

    @Override
    public boolean apply(String input) {
        return automaton.run(input);
    }
}
