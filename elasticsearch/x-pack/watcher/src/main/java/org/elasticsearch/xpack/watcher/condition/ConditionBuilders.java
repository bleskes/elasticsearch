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

package org.elasticsearch.xpack.watcher.condition;

import org.elasticsearch.xpack.watcher.condition.always.AlwaysCondition;
import org.elasticsearch.xpack.watcher.condition.compare.CompareCondition;
import org.elasticsearch.xpack.watcher.condition.compare.array.ArrayCompareCondition;
import org.elasticsearch.xpack.watcher.condition.never.NeverCondition;
import org.elasticsearch.xpack.watcher.condition.script.ScriptCondition;
import org.elasticsearch.xpack.watcher.support.Script;

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
        return scriptCondition(Script.inline(script));
    }

    public static ScriptCondition.Builder scriptCondition(Script.Builder script) {
        return scriptCondition(script.build());
    }

    public static ScriptCondition.Builder scriptCondition(Script script) {
        return ScriptCondition.builder(script);
    }

    public static CompareCondition.Builder compareCondition(String path, CompareCondition.Op op, Object value) {
        return CompareCondition.builder(path, op, value);
    }

    public static ArrayCompareCondition.Builder arrayCompareCondition(String arrayPath, String path, ArrayCompareCondition.Op op,
                                                                      Object value, ArrayCompareCondition .Quantifier quantifier) {
        return ArrayCompareCondition.builder(arrayPath, path, op, value, quantifier);
    }
}
