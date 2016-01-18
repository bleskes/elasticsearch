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

package org.elasticsearch.marvel.agent.renderer;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.marvel.agent.exporter.MarvelDoc;

import java.util.Collections;
import java.util.Map;

public class RendererRegistry {

    private final Map<Class<? extends MarvelDoc>, Renderer> renderers;

    @Inject
    public RendererRegistry(Map<Class<? extends MarvelDoc>, Renderer> renderers) {
        this.renderers = Collections.unmodifiableMap(renderers);
    }

    public Renderer getRenderer(MarvelDoc marvelDoc) {
        return renderers.get(marvelDoc.getClass());
    }
}
