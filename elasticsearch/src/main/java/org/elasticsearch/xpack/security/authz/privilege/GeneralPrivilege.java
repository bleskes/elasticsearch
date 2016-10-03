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
import dk.brics.automaton.BasicAutomata;

/**
 *
 */
public class GeneralPrivilege extends AbstractAutomatonPrivilege<GeneralPrivilege> {

    public static final GeneralPrivilege NONE = new GeneralPrivilege(Name.NONE, BasicAutomata.makeEmpty());
    public static final GeneralPrivilege ALL = new GeneralPrivilege(Name.ALL, "*");

    public GeneralPrivilege(String name, String... patterns) {
        super(name, patterns);
    }

    public GeneralPrivilege(Name name, String... patterns) {
        super(name, patterns);
    }

    public GeneralPrivilege(Name name, Automaton automaton) {
        super(name, automaton);
    }

    @Override
    protected GeneralPrivilege create(Name name, Automaton automaton) {
        return new GeneralPrivilege(name, automaton);
    }

    @Override
    protected GeneralPrivilege none() {
        return NONE;
    }
}
