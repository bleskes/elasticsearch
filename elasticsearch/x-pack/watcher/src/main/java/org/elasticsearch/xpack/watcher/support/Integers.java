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

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

public class Integers {
    private Integers() {
    }

    public static Iterable<Integer> asIterable(int[] values) {
        Objects.requireNonNull(values);
        return () -> new Iterator<Integer>() {
            private int position = 0;
            @Override
            public boolean hasNext() {
                return position < values.length;
            }

            @Override
            public Integer next() {
                if (position < values.length) {
                    return values[position++];
                } else {
                    throw new NoSuchElementException("position: " + position + ", length: " + values.length);
                }
            }
        };
    }

    public static boolean contains(int[] values, final int value) {
        return Arrays.stream(values).anyMatch(v -> v == value);
    }
}
