/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2016] Elasticsearch Incorporated. All Rights Reserved.
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
import org.elasticsearch.common.util.concurrent.ThreadContext;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * This action listener wraps another listener and provides a framework for iteration over a List while calling an asynchronous function
 * for each. The listener calls the {@link BiConsumer} with the current element in the list and a {@link ActionListener}. This function
 * is expected to call the listener in case of success or failure due to an exception. If there is a failure due to an exception the wrapped
 * listener's {@link ActionListener#onFailure(Exception)} method is called. If the consumer calls {@link #onResponse(Object)} with a
 * non-null object, iteration will cease and the wrapped listener will be called with the response. In the case of a null value being passed
 * to {@link #onResponse(Object)} then iteration will continue by applying the {@link BiConsumer} to the next item in the list; if the list
 * has no more elements then the wrapped listener will be called with a null value.
 *
 * After creation, iteration is started by calling {@link #run()}
 */
public final class IteratingActionListener<T, U> implements ActionListener<T>, Runnable {

    private final List<U> consumables;
    private final ActionListener<T> delegate;
    private final BiConsumer<U, ActionListener<T>> consumer;
    private final ThreadContext threadContext;

    private int position = 0;

    public IteratingActionListener(ActionListener<T> delegate, BiConsumer<U, ActionListener<T>> consumer, List<U> consumables,
                                   ThreadContext threadContext) {
        this.delegate = delegate;
        this.consumer = consumer;
        this.consumables = Collections.unmodifiableList(consumables);
        this.threadContext = threadContext;
    }

    @Override
    public void run() {
        if (consumables.isEmpty()) {
            onResponse(null);
        } else if (position < 0 || position >= consumables.size()) {
            onFailure(new IllegalStateException("invalid position [" + position + "]. List size [" + consumables.size() + "]"));
        } else {
            try (ThreadContext.StoredContext ignore = threadContext.newStoredContext(false)) {
                consumer.accept(consumables.get(position++), this);
            }
        }
    }

    @Override
    public void onResponse(T response) {
        // we need to store the context here as there is a chance that this method is called from a thread outside of the ThreadPool
        // like a LDAP connection reader thread and we can pollute the context in certain cases
        try (ThreadContext.StoredContext ignore = threadContext.newStoredContext(false)) {
            if (response == null) {
                if (position == consumables.size()) {
                    delegate.onResponse(null);
                } else {
                    consumer.accept(consumables.get(position++), this);
                }
            } else {
                delegate.onResponse(response);
            }
        }
    }

    @Override
    public void onFailure(Exception e) {
        // we need to store the context here as there is a chance that this method is called from a thread outside of the ThreadPool
        // like a LDAP connection reader thread and we can pollute the context in certain cases
        try (ThreadContext.StoredContext ignore = threadContext.newStoredContext(false)) {
            delegate.onFailure(e);
        }
    }
}
