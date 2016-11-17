/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.prelert.utils;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An interface for iterators that can have resources that will be automatically cleaned up
 * if iterator is created in a try-with-resources block.
 */
public interface CloseableIterator<T> extends Iterator<T>, Closeable {

    /**
     * Returns a closable iterator that has no elements.
     */
    @SuppressWarnings("unchecked")
    static <T> CloseableIterator<T> empty() {
        return (CloseableIterator<T>) EMPTY;
    }

    CloseableIterator<Object> EMPTY = new CloseableIterator<Object>() {
        @Override
        public void close() throws IOException {}

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Object next() {
            throw new NoSuchElementException();
        }
    };

}
