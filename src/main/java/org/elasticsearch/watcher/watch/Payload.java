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

package org.elasticsearch.watcher.watch;

import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.watcher.WatcherException;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.elasticsearch.watcher.support.WatcherUtils.responseToData;

/**
 *
 */
public interface Payload extends ToXContent {

    Simple EMPTY = new Simple(Collections.<String, Object>emptyMap());

    Map<String, Object> data();

    class Simple implements Payload {

        private final Map<String, Object> data;

        public Simple() {
            this(new HashMap<String, Object>());
        }

        public Simple(String key, Object value) {
            this(new MapBuilder<String, Object>().put(key, value).map());
        }

        public Simple(Map<String, Object> data) {
            this.data = data;
        }

        @Override
        public Map<String, Object> data() {
            return data;
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            return builder.value(data);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Simple simple = (Simple) o;

            if (!data.equals(simple.data)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }

        @Override
        public String toString() {
            return "simple[" + Objects.toString(data) + "]";
        }
    }

    static class XContent extends Simple {

        public XContent(XContentParser parser) {
            super(mapOrdered(parser));
        }

        public XContent(ToXContent response) {
            super(responseToData(response));
        }

        private static Map<String, Object> mapOrdered(XContentParser parser) {
            try {
                return parser.mapOrdered();
            } catch (IOException ioe) {
                throw new WatcherException("could not build a payload out of xcontent", ioe);
            }
        }
    }
}
