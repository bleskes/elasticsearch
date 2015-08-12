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

package org.elasticsearch.marvel.agent.exporter;

public abstract class MarvelDoc<T> {

    private final String index;
    private final String type;
    private final String id;

    private final String clusterName;
    private final long timestamp;

    public MarvelDoc(String index, String type, String id, String clusterName, long timestamp) {
        this.index = index;
        this.type = type;
        this.id = id;
        this.clusterName = clusterName;
        this.timestamp = timestamp;
    }

    public MarvelDoc(String clusterName, String type, long timestamp) {
        this(null, type, null, clusterName, timestamp);
    }

    public String clusterName() {
        return clusterName;
    }

    public String index() {
        return index;
    }

    public String type() {
        return type;
    }

    public String id() {
        return id;
    }

    public long timestamp() {
        return timestamp;
    }

    public abstract T payload();
}