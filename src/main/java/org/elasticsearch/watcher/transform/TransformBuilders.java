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

package org.elasticsearch.watcher.transform;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.watcher.support.Script;

/**
 *
 */
public final class TransformBuilders {

    private TransformBuilders() {
    }

    public static SearchTransform.SourceBuilder searchTransform(SearchRequest request) {
        return new SearchTransform.SourceBuilder(request);
    }

    public static ScriptTransform.SourceBuilder scriptTransform(String script) {
        return new ScriptTransform.SourceBuilder(script);
    }

    public static ScriptTransform.SourceBuilder scriptTransform(Script script) {
        return new ScriptTransform.SourceBuilder(script);
    }

    public static ChainTransform.SourceBuilder chainTransform(Transform.SourceBuilder... transforms) {
        return new ChainTransform.SourceBuilder(transforms);
    }

}
