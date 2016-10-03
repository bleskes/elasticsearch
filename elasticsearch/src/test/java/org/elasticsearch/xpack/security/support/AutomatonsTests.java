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
import org.elasticsearch.test.ESTestCase;

import static org.elasticsearch.xpack.security.support.Automatons.pattern;
import static org.elasticsearch.xpack.security.support.Automatons.patterns;
import static org.elasticsearch.xpack.security.support.Automatons.wildcard;
import static org.hamcrest.Matchers.is;

/**
 *
 */
public class AutomatonsTests extends ESTestCase {
    public void testPatternsUnionOfMultiplePatterns() throws Exception {
        assertMatch(patterns("/fo.*/", "ba*"), "foo");
        assertMatch(patterns("/fo.*/", "ba*"), "bar");
        assertMismatch(patterns("/fo.*/", "ba*"), "zipfoo");
    }

    public void testPatternSingle() throws Exception {
        assertMatch(pattern("/.*st/"), "test");
        assertMatch(pattern("/t.*st/"), "test");
        assertMatch(pattern("/tes*./"), "test");
        assertMatch(pattern("/test/"), "test");
        assertMismatch(pattern("/.*st/"), "tet");
        assertMatch(pattern("*st"), "test");
        assertMatch(pattern("t*t"), "test");
        assertMatch(pattern("t?st"), "test");
        assertMismatch(pattern("t?t"), "test");
        assertMatch(pattern("tes*"), "test");
        assertMatch(pattern("test"), "test");
        assertMismatch(pattern("*st"), "tet");
        assertInvalidPattern("/test");
        assertInvalidPattern("/te*");
        assertInvalidPattern("/te.*");
        assertMismatch(pattern(".*st"), "test");
        assertMatch(pattern("*st\\"), "test\\");
        assertMatch(pattern("tes.*/"), "tes.t/");
        assertMatch(pattern("\\/test"), "/test");
    }

    public void testWildcard() throws Exception {
        assertMatch(wildcard("*st"), "test");
        assertMatch(wildcard("t*st"), "test");
        assertMatch(wildcard("tes*"), "test");
        assertMatch(wildcard("test"), "test");
        assertMismatch(wildcard("*st"), "tet");
        assertMismatch(wildcard("t\\*st"), "test");
        assertMatch(wildcard("t\\*st"), "t*st");
    }

    private void assertMatch(Automaton automaton, String text) {
        RunAutomaton runAutomaton = new RunAutomaton(automaton, false);
        assertThat(runAutomaton.run(text), is(true));
    }

    private void assertMismatch(Automaton automaton, String text) {
        RunAutomaton runAutomaton = new RunAutomaton(automaton, false);
        assertThat(runAutomaton.run(text), is(false));
    }

    private void assertInvalidPattern(String text) {
        try {
            pattern(text);
            fail("expected an error on invalid pattern [" + text + "]");
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }
}
