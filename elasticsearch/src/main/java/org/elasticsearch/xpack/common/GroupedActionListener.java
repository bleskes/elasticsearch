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
package org.elasticsearch.xpack.common;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.common.util.concurrent.AtomicArray;
import org.elasticsearch.common.util.concurrent.CountDown;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * An action listener that delegates it's results to another listener once
 * it has received one or more failures or N results. This allows synchronous
 * tasks to be forked off in a loop with the same listener and respond to a higher level listener once all tasks responded.
 */
public final class GroupedActionListener<T> implements ActionListener<T> {
    private final CountDown countDown;
    private final AtomicInteger pos = new AtomicInteger();
    private final AtomicArray<T> roles;
    private final ActionListener<Collection<T>> delegate;
    private final Collection<T> defaults;
    private final AtomicReference<Exception> failure = new AtomicReference<>();

    /**
     * Creates a new listener
     * @param delegate the delegate listener
     * @param groupSize the group size
     */
    public GroupedActionListener(ActionListener<Collection<T>> delegate, int groupSize, Collection<T> defaults) {
        roles = new AtomicArray<>(groupSize);
        countDown = new CountDown(groupSize);
        this.delegate = delegate;
        this.defaults = defaults;
    }

    @Override
    public void onResponse(T element) {
        roles.set(pos.incrementAndGet() - 1, element);
        if (countDown.countDown()) {
            if (failure.get() != null) {
                delegate.onFailure(failure.get());
            } else {
                List<T> collect = this.roles.asList().stream().map((e)
                        -> e.value).filter(Objects::nonNull).collect(Collectors.toList());
                collect.addAll(defaults);
                delegate.onResponse(Collections.unmodifiableList(collect));
            }
        }
    }

    @Override
    public void onFailure(Exception e) {
        if (failure.compareAndSet(null, e) == false) {
            failure.get().addSuppressed(e);
        }
        if (countDown.countDown()) {
            delegate.onFailure(failure.get());
        }
    }
}