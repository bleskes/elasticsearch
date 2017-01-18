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
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.test.ESTestCase;

import java.io.IOException;

public class ContextPreservingActionListenerTests extends ESTestCase {

    public void testOriginalContextIsPreservedAfterOnResponse() throws IOException {
        try (ThreadContext threadContext = new ThreadContext(Settings.EMPTY)) {
            final boolean nonEmptyContext = randomBoolean();
            if (nonEmptyContext) {
                threadContext.putHeader("not empty", "value");
            }
            ContextPreservingActionListener<Void> actionListener;
            try (ThreadContext.StoredContext ignore = threadContext.stashContext()) {
                threadContext.putHeader("foo", "bar");
                actionListener = new ContextPreservingActionListener<>(threadContext.newRestorableContext(true),
                        new ActionListener<Void>() {
                    @Override
                    public void onResponse(Void aVoid) {
                        assertEquals("bar", threadContext.getHeader("foo"));
                        assertNull(threadContext.getHeader("not empty"));
                    }

                    @Override
                    public void onFailure(Exception e) {
                        throw new RuntimeException("onFailure shouldn't be called", e);
                    }
                });
            }

            assertNull(threadContext.getHeader("foo"));
            assertEquals(nonEmptyContext ? "value" : null, threadContext.getHeader("not empty"));

            actionListener.onResponse(null);

            assertNull(threadContext.getHeader("foo"));
            assertEquals(nonEmptyContext ? "value" : null, threadContext.getHeader("not empty"));
        }
    }

    public void testOriginalContextIsPreservedAfterOnFailure() throws Exception {
        try (ThreadContext threadContext = new ThreadContext(Settings.EMPTY)) {
            final boolean nonEmptyContext = randomBoolean();
            if (nonEmptyContext) {
                threadContext.putHeader("not empty", "value");
            }
            ContextPreservingActionListener<Void> actionListener;
            try (ThreadContext.StoredContext ignore = threadContext.stashContext()) {
                threadContext.putHeader("foo", "bar");
                actionListener = new ContextPreservingActionListener<>(threadContext.newRestorableContext(true),
                        new ActionListener<Void>() {
                            @Override
                            public void onResponse(Void aVoid) {
                                throw new RuntimeException("onResponse shouldn't be called");
                            }

                            @Override
                            public void onFailure(Exception e) {
                                assertEquals("bar", threadContext.getHeader("foo"));
                                assertNull(threadContext.getHeader("not empty"));
                            }
                        });
            }

            assertNull(threadContext.getHeader("foo"));
            assertEquals(nonEmptyContext ? "value" : null, threadContext.getHeader("not empty"));

            actionListener.onFailure(null);

            assertNull(threadContext.getHeader("foo"));
            assertEquals(nonEmptyContext ? "value" : null, threadContext.getHeader("not empty"));
        }
    }

    public void testOriginalContextIsWhenListenerThrows() throws Exception {
        try (ThreadContext threadContext = new ThreadContext(Settings.EMPTY)) {
            final boolean nonEmptyContext = randomBoolean();
            if (nonEmptyContext) {
                threadContext.putHeader("not empty", "value");
            }
            ContextPreservingActionListener<Void> actionListener;
            try (ThreadContext.StoredContext ignore = threadContext.stashContext()) {
                threadContext.putHeader("foo", "bar");
                actionListener = new ContextPreservingActionListener<>(threadContext.newRestorableContext(true),
                        new ActionListener<Void>() {
                            @Override
                            public void onResponse(Void aVoid) {
                                assertEquals("bar", threadContext.getHeader("foo"));
                                assertNull(threadContext.getHeader("not empty"));
                                throw new RuntimeException("onResponse called");
                            }

                            @Override
                            public void onFailure(Exception e) {
                                assertEquals("bar", threadContext.getHeader("foo"));
                                assertNull(threadContext.getHeader("not empty"));
                                throw new RuntimeException("onFailure called");
                            }
                        });
            }

            assertNull(threadContext.getHeader("foo"));
            assertEquals(nonEmptyContext ? "value" : null, threadContext.getHeader("not empty"));

            RuntimeException e = expectThrows(RuntimeException.class, () -> actionListener.onResponse(null));
            assertEquals("onResponse called", e.getMessage());

            assertNull(threadContext.getHeader("foo"));
            assertEquals(nonEmptyContext ? "value" : null, threadContext.getHeader("not empty"));

            e = expectThrows(RuntimeException.class, () -> actionListener.onFailure(null));
            assertEquals("onFailure called", e.getMessage());

            assertNull(threadContext.getHeader("foo"));
            assertEquals(nonEmptyContext ? "value" : null, threadContext.getHeader("not empty"));
        }
    }
}
