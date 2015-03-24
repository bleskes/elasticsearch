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

package org.elasticsearch.watcher.actions;

import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 *
 */
public class Actions implements Iterable<Action>, ToXContent {

    private final List<Action> actions;

    public Actions(List<Action> actions) {
        this.actions = actions;
    }

    public int count() {
        return actions.size();
    }

    @Override
    public Iterator<Action> iterator() {
        return actions.iterator();
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startArray();
        for (Action action : actions){
            builder.startObject().field(action.type(), action).endObject();
        }
        builder.endArray();
        return builder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Actions actions1 = (Actions) o;

        if (!actions.equals(actions1.actions)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return actions.hashCode();
    }
}
