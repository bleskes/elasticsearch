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

import org.apache.lucene.util.automaton.Automata;
import org.apache.lucene.util.automaton.Automaton;
import org.apache.lucene.util.automaton.RegExp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.apache.lucene.util.automaton.MinimizationOperations.minimize;
import static org.apache.lucene.util.automaton.Operations.DEFAULT_MAX_DETERMINIZED_STATES;
import static org.apache.lucene.util.automaton.Operations.concatenate;
import static org.apache.lucene.util.automaton.Operations.determinize;
import static org.apache.lucene.util.automaton.Operations.minus;
import static org.apache.lucene.util.automaton.Operations.union;

/**
 *
 */
public final class Automatons {

    public static final Automaton EMPTY = Automata.makeEmpty();
    public static final Automaton MATCH_ALL = Automata.makeAnyString();

    static final char WILDCARD_STRING = '*';     // String equality with support for wildcards
    static final char WILDCARD_CHAR = '?';       // Char equality with support for wildcards
    static final char WILDCARD_ESCAPE = '\\';    // Escape character

    private Automatons() {
    }

    /**
     * Builds and returns an automaton that will represent the union of all the given patterns.
     */
    public static Automaton patterns(String... patterns) {
        return patterns(Arrays.asList(patterns));
    }

    /**
     * Builds and returns an automaton that will represent the union of all the given patterns.
     */
    public static Automaton patterns(Collection<String> patterns) {
        if (patterns.isEmpty()) {
            return EMPTY;
        }
        Automaton automaton = null;
        for (String pattern : patterns) {
            if (automaton == null) {
                automaton = pattern(pattern);
            } else {
                automaton = union(automaton, pattern(pattern));
            }
        }
        return minimize(automaton, DEFAULT_MAX_DETERMINIZED_STATES); // minimal is also deterministic
    }

    /**
     * Builds and returns an automaton that represents the given pattern.
     */
    static Automaton pattern(String pattern) {
        if (pattern.startsWith("/")) { // it's a lucene regexp
            if (pattern.length() == 1 || !pattern.endsWith("/")) {
                throw new IllegalArgumentException("invalid pattern [" + pattern + "]. patterns starting with '/' " +
                        "indicate regular expression pattern and therefore must also end with '/'." +
                        " other patterns (those that do not start with '/') will be treated as simple wildcard patterns");
            }
            String regex = pattern.substring(1, pattern.length() - 1);
            return new RegExp(regex).toAutomaton();
        }
        return wildcard(pattern);
    }

    /**
     * Builds and returns an automaton that represents the given pattern.
     */
    @SuppressWarnings("fallthrough") // explicit fallthrough at end of switch
    static Automaton wildcard(String text) {
        List<Automaton> automata = new ArrayList<>();
        for (int i = 0; i < text.length();) {
            final char c = text.charAt(i);
            int length = 1;
            switch(c) {
                case WILDCARD_STRING:
                    automata.add(Automata.makeAnyString());
                    break;
                case WILDCARD_CHAR:
                    automata.add(Automata.makeAnyChar());
                    break;
                case WILDCARD_ESCAPE:
                    // add the next codepoint instead, if it exists
                    if (i + length < text.length()) {
                        final char nextChar = text.charAt(i + length);
                        length += 1;
                        automata.add(Automata.makeChar(nextChar));
                        break;
                    } // else fallthru, lenient parsing with a trailing \
                default:
                    automata.add(Automata.makeChar(c));
            }
            i += length;
        }
        return concatenate(automata);
    }

    public static Automaton unionAndDeterminize(Automaton a1, Automaton a2) {
        Automaton res = union(a1, a2);
        return determinize(res, DEFAULT_MAX_DETERMINIZED_STATES);
    }

    public static Automaton minusAndDeterminize(Automaton a1, Automaton a2) {
        Automaton res = minus(a1, a2, DEFAULT_MAX_DETERMINIZED_STATES);
        return determinize(res, DEFAULT_MAX_DETERMINIZED_STATES);
    }
}
