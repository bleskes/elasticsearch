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

package org.elasticsearch.marvel.action;

import org.elasticsearch.common.Strings;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;

import java.io.IOException;

/**
 * {@code MonitoringIndex} represents the receivable index from any request.
 * <p>
 * This allows external systems to provide details for an index without having to know its exact name.
 */
public enum MonitoringIndex implements Writeable {

    /**
     * Data that drives information about the "cluster" (e.g., a node or instance).
     */
    DATA {
        @Override
        public boolean matchesIndexName(String indexName) {
            return "_data".equals(indexName);
        }
    },

    /**
     * Timestamped data that drives the charts (e.g., memory statistics).
     */
    TIMESTAMPED {
        @Override
        public boolean matchesIndexName(String indexName) {
            return Strings.isEmpty(indexName);
        }
    };

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeByte((byte)ordinal());
    }

    public static MonitoringIndex readFrom(StreamInput in) throws IOException {
        return values()[in.readByte()];
    }

    /**
     * Determine if the {@code indexName} matches {@code this} monitoring index.
     *
     * @param indexName The name of the index.
     * @return {@code true} if {@code this} matches the {@code indexName}
     */
    public abstract boolean matchesIndexName(String indexName);

    /**
     * Find the {@link MonitoringIndex} to use for the request.
     *
     * @param indexName The name of the index.
     * @return Never {@code null}.
     * @throws IllegalArgumentException if {@code indexName} is unrecognized
     */
    public static MonitoringIndex from(String indexName) {
        for (MonitoringIndex index : values()) {
            if (index.matchesIndexName(indexName)) {
                return index;
            }
        }

        throw new IllegalArgumentException("unrecognized index name [" + indexName + "]");
    }

}
