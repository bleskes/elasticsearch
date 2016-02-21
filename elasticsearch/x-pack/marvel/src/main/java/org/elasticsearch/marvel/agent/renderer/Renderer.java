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

import org.elasticsearch.common.xcontent.XContentType;

import java.io.IOException;
import java.io.OutputStream;

/**
 * {@code Renderer}s are used to render documents using a given OutputStream.
 * <p>
 * Each {@code Renderer} can be thought of as a generator of a unique document <em>type</em> within the resulting ES index. For example,
 * there will be details about shards, which requires a unique document type and there will also be details about indices, which requires
 * their own unique documents.
 *
 * @see AbstractRenderer
 */
public interface Renderer<T> {
    /**
     * Convert the given {@code document} type into something that can be sent to the monitoring cluster.
     *
     * @param document The arbitrary document (e.g., details about a shard)
     * @param xContentType The rendered content type (e.g., JSON)
     * @param os The buffer
     * @throws IOException if any unexpected error occurs
     */
    void render(T document, XContentType xContentType, OutputStream os) throws IOException;
}
