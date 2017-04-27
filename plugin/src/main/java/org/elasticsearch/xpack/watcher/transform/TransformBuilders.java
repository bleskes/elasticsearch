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

package org.elasticsearch.xpack.watcher.transform;

import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.xpack.watcher.support.search.WatcherSearchTemplateRequest;
import org.elasticsearch.xpack.watcher.transform.chain.ChainTransform;
import org.elasticsearch.xpack.watcher.transform.script.ScriptTransform;
import org.elasticsearch.xpack.watcher.transform.search.SearchTransform;

import static java.util.Collections.emptyMap;

public final class TransformBuilders {

    private TransformBuilders() {
    }

    public static SearchTransform.Builder searchTransform(WatcherSearchTemplateRequest request) {
        return SearchTransform.builder(request);
    }

    public static ScriptTransform.Builder scriptTransform(String script) {
        return scriptTransform(new Script(ScriptType.INLINE, Script.DEFAULT_SCRIPT_LANG, script, emptyMap()));
    }

    public static ScriptTransform.Builder scriptTransform(Script script) {
        return ScriptTransform.builder(script);
    }

    public static ChainTransform.Builder chainTransform(Transform.Builder... transforms) {
        return ChainTransform.builder().add(transforms);
    }

    public static ChainTransform.Builder chainTransform(Transform... transforms) {
        return ChainTransform.builder(transforms);
    }

}
