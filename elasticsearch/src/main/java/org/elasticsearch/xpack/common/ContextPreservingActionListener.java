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
import org.elasticsearch.common.util.concurrent.ThreadContext;

/**
 * Restores the given {@link org.elasticsearch.common.util.concurrent.ThreadContext.StoredContext}
 * once the listener is invoked
 */
public final class ContextPreservingActionListener<R> implements ActionListener<R> {

    private final ActionListener<R> delegate;
    private final ThreadContext.StoredContext context;
    private final ThreadContext threadContext;

    public ContextPreservingActionListener(ThreadContext threadContext, ThreadContext.StoredContext context, ActionListener<R> delegate) {
        this.delegate = delegate;
        this.context = context;
        this.threadContext = threadContext;
    }

    @Override
    public void onResponse(R r) {
        try (ThreadContext.StoredContext ignore = threadContext.newStoredContext()) {
            context.restore();
            delegate.onResponse(r);
        }
    }

    @Override
    public void onFailure(Exception e) {
        try (ThreadContext.StoredContext ignore = threadContext.newStoredContext()) {
            context.restore();
            delegate.onFailure(e);
        }
    }
}
