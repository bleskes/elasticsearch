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

package org.elasticsearch.watcher.support.template.xmustache;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.reflect.ReflectionObjectHandler;
import com.google.common.collect.Iterables;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.watcher.support.ArrayObjectIterator;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.*;

/**
 * An extension to elasticsearch's {@code JsonEscapingMustacheFactory} that on top of applying json
 * escapes it also enables support for navigating arrays using `array.X` notation (where `X` is the index
 * of the element in the array).
 */
public class XMustacheFactory extends DefaultMustacheFactory {

    final XContentType contentType;

    public XMustacheFactory(XContentType contentType) {
        this.contentType = contentType;
        setObjectHandler(new ReflectionObjectHandler() {
            @Override
            public Object coerce(Object object) {
                if (object != null) {
                    if (object.getClass().isArray()) {
                        return new ArrayMap(object);
                    } else if (object instanceof Collection) {
                        return new CollectionMap((Collection) object);
                    }
                }
                return super.coerce(object);
            }
        });
    }

    @Override
    public void encode(String value, Writer writer) {
        try {
            if (contentType == XContentType.JSON) {
                writer.write(JsonStringEncoder.getInstance().quoteAsString(value));
            } else {
                writer.write(value);
            }
        } catch (IOException e) {
            throw new MustacheException("Failed to encode value: " + value);
        }
    }

    static class ArrayMap extends AbstractMap<Object, Object> implements Iterable<Object> {

        private final Object array;

        public ArrayMap(Object array) {
            this.array = array;
        }

        @Override
        public Object get(Object key) {
            if (key instanceof Number) {
                return Array.get(array, ((Number) key).intValue());
            }
            try {
                int index = Integer.parseInt(key.toString());
                return Array.get(array, index);
            } catch (NumberFormatException nfe) {
                // if it's not a number it is as if the key doesn't exist
                return null;
            }
        }

        @Override
        public boolean containsKey(Object key) {
            return get(key) != null;
        }

        @Override
        public Set<Entry<Object, Object>> entrySet() {
            int length = Array.getLength(array);
            Map<Object, Object> map = new HashMap<>(length);
            for (int i = 0; i < length; i++) {
                map.put(i, Array.get(array, i));
            }
            return map.entrySet();
        }

        /**
         * Returns an iterator over a set of elements of type T.
         *
         * @return an Iterator.
         */
        @Override
        public Iterator<Object> iterator() {
            return new ArrayObjectIterator(array);
        }


    }

    static class CollectionMap extends AbstractMap<Object, Object> implements Iterable<Object> {

        private final Collection col;

        public CollectionMap(Collection col) {
            this.col = col;
        }

        @Override
        public Object get(Object key) {
            if (key instanceof Number) {
                return Iterables.get(col, ((Number) key).intValue());
            }
            try {
                int index = Integer.parseInt(key.toString());
                return Iterables.get(col, index);
            } catch (NumberFormatException nfe) {
                // if it's not a number it is as if the key doesn't exist
                return null;
            }
        }

        @Override
        public boolean containsKey(Object key) {
            return get(key) != null;
        }

        @Override
        public Set<Entry<Object, Object>> entrySet() {
            Map<Object, Object> map = new HashMap<>(col.size());
            int i = 0;
            for (Object item : col) {
                map.put(i++, item);
            }
            return map.entrySet();
        }

        /**
         * Returns an iterator over a set of elements of type T.
         *
         * @return an Iterator.
         */
        @Override
        public Iterator<Object> iterator() {
            return col.iterator();
        }
    }


}
