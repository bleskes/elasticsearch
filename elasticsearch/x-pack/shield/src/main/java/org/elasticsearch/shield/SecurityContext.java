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

package org.elasticsearch.shield;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.shield.authc.AuthenticationService;
import org.elasticsearch.shield.user.User;
import org.elasticsearch.threadpool.ThreadPool;

import java.io.IOException;
import java.util.concurrent.Callable;

/**
 *
 */
public interface SecurityContext {

    void executeAs(User user, Runnable runnable);

    <V> V executeAs(User user, Callable<V> callable);

    User getUser();

    class Insecure implements SecurityContext {

        public static final Insecure INSTANCE = new Insecure();

        private Insecure() {
        }

        @Override
        public void executeAs(User user, Runnable runnable) {
            runnable.run();
        }

        @Override
        public <V> V executeAs(User user, Callable<V> callable) {
            try {
                return callable.call();
            } catch (Exception e) {
                throw new ElasticsearchException(e);
            }
        }

        @Override
        public User getUser() {
            return null;
        }
    }

    class Secure implements SecurityContext {

        private final ThreadContext threadContext;
        private final AuthenticationService authcService;

        @Inject
        public Secure(ThreadPool threadPool, AuthenticationService authcService) {
            this.threadContext = threadPool.getThreadContext();
            this.authcService = authcService;
        }

        public void executeAs(User user, Runnable runnable) {
            try (ThreadContext.StoredContext ctx = threadContext.stashContext()) {
                setUser(user);
                runnable.run();
            }
        }

        public <V> V executeAs(User user, Callable<V> callable) {
            try (ThreadContext.StoredContext ctx = threadContext.stashContext()) {
                setUser(user);
                return callable.call();
            } catch (Exception e) {
                throw new ElasticsearchException(e);
            }
        }

        @Override
        public User getUser() {
            return authcService.getCurrentUser();
        }

        private void setUser(User user) {
            try {
                authcService.attachUserHeaderIfMissing(user);
            } catch (IOException e) {
                throw new ElasticsearchException("failed to attach watcher user to request", e);
            }
        }
    }
}
