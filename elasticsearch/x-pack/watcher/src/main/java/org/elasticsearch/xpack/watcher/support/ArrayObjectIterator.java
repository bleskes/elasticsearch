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

package org.elasticsearch.xpack.watcher.support;

import java.lang.reflect.Array;
import java.util.Iterator;

/**
 *
 */
public class ArrayObjectIterator implements Iterator<Object> {

    private final Object array;
    private final int length;
    private int index;

    public ArrayObjectIterator(Object array) {
        this.array = array;
        this.length = Array.getLength(array);
        this.index = 0;
    }

    @Override
    public boolean hasNext() {
        return index < length;
    }

    @Override
    public Object next() {
        return Array.get(array, index++);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("array iterator does not support removing elements");
    }

    public static class Iterable implements java.lang.Iterable<Object> {

        private Object array;

        public Iterable(Object array) {
            this.array = array;
        }

        @Override
        public Iterator<Object> iterator() {
            return new ArrayObjectIterator(array);
        }
    }
}
