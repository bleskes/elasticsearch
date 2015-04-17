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

package org.elasticsearch.watcher.condition;

import org.elasticsearch.watcher.condition.always.AlwaysCondition;
import org.elasticsearch.watcher.condition.never.NeverCondition;
import org.elasticsearch.watcher.condition.script.ExecutableScriptCondition;
import org.elasticsearch.watcher.condition.never.ExecutableNeverCondition;
import org.elasticsearch.watcher.condition.always.ExecutableAlwaysCondition;
import org.elasticsearch.watcher.condition.script.ScriptCondition;

/**
 *
 */
public final class ConditionBuilders {

    private ConditionBuilders() {
    }

    public static AlwaysCondition.Builder alwaysCondition() {
        return AlwaysCondition.Builder.INSTANCE;
    }

    public static NeverCondition.Builder neverCondition() {
        return NeverCondition.Builder.INSTANCE;
    }

    public static ScriptCondition.Builder scriptCondition(String script) {
        return ScriptCondition.builder(script);
    }
}
