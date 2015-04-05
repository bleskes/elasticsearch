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

package org.elasticsearch.watcher.client;

import org.elasticsearch.watcher.support.http.TemplatedHttpRequest;
import org.elasticsearch.watcher.support.template.ScriptTemplate;

/**
 *
 */
public final class WatchSourceBuilders {

    private WatchSourceBuilders() {
    }

    public static WatchSourceBuilder watchBuilder() {
        return new WatchSourceBuilder();
    }

    public static ScriptTemplate.SourceBuilder template(String text) {
        return new ScriptTemplate.SourceBuilder(text);
    }

    public static TemplatedHttpRequest.SourceBuilder templatedHttpRequest(String host, int port) {
        return new TemplatedHttpRequest.SourceBuilder(host, port);
    }
}
