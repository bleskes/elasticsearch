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

package org.elasticsearch.xpack.prelert.job.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ListDocument {
    public static final String TYPE = "list";
    public static final String ID = "id";
    public static final String ITEMS = "items";

    private final String id;
    private final List<String> items;

    @JsonCreator
    public ListDocument(@JsonProperty("id") String id, @JsonProperty("items") List<String> items) {
        this.id = Objects.requireNonNull(id);
        this.items = items;
    }

    public String getId() {
        return id;
    }

    public List<String> getItems() {
        return new ArrayList<>(items);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof ListDocument)) {
            return false;
        }

        ListDocument other = (ListDocument) obj;
        return id.equals(other.id) && items.equals(other.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, items);
    }
}