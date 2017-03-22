/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2017] Elasticsearch Incorporated. All Rights Reserved.
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

package org.elasticsearch.xpack.security.authc.ldap;

import org.elasticsearch.ElasticsearchTimeoutException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.security.authc.ldap.LdapRealm.CancellableLdapRunnable;
import org.elasticsearch.xpack.security.user.User;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.sameInstance;

public class CancellableLdapRunnableTests extends ESTestCase {

    public void testTimingOutARunnable() {
        AtomicReference<Exception> exceptionAtomicReference = new AtomicReference<>();
        final CancellableLdapRunnable runnable =
                new CancellableLdapRunnable(ActionListener.wrap(user -> {
            throw new AssertionError("onResponse should not be called");
        }, exceptionAtomicReference::set), () -> {
            throw new AssertionError("runnable should not be executed");
        }, logger);

        runnable.maybeTimeout();
        runnable.run();
        assertNotNull(exceptionAtomicReference.get());
        assertThat(exceptionAtomicReference.get(), instanceOf(ElasticsearchTimeoutException.class));
        assertThat(exceptionAtomicReference.get().getMessage(),
                containsString("timed out waiting for execution"));
    }

    public void testCallTimeOutAfterRunning() {
        final AtomicBoolean ran = new AtomicBoolean(false);
        final AtomicBoolean listenerCalled = new AtomicBoolean(false);
        final CancellableLdapRunnable runnable =
                new CancellableLdapRunnable(ActionListener.wrap(user -> {
                    listenerCalled.set(true);
                    throw new AssertionError("onResponse should not be called");
        }, e -> {
                    listenerCalled.set(true);
                    throw new AssertionError("onFailure should not be called");
        }), () -> ran.set(ran.get() == false), logger);

        runnable.run();
        assertTrue(ran.get());
        runnable.maybeTimeout();
        assertTrue(ran.get());
        // the listener shouldn't have ever been called. If it was, then either something called
        // onResponse or onFailure was called as part of the timeout
        assertFalse(listenerCalled.get());
    }

    public void testRejectingExecution() {
        AtomicReference<Exception> exceptionAtomicReference = new AtomicReference<>();
        final CancellableLdapRunnable runnable =
                new CancellableLdapRunnable(ActionListener.wrap(user -> {
            throw new AssertionError("onResponse should not be called");
        }, exceptionAtomicReference::set), () -> {
            throw new AssertionError("runnable should not be executed");
        }, logger);

        final Exception e = new RuntimeException("foo");
        runnable.onRejection(e);

        assertNotNull(exceptionAtomicReference.get());
        assertThat(exceptionAtomicReference.get(), sameInstance(e));
    }

    public void testTimeoutDuringExecution() throws InterruptedException {
        final CountDownLatch listenerCalledLatch = new CountDownLatch(1);
        final CountDownLatch timeoutCalledLatch = new CountDownLatch(1);
        final CountDownLatch runningLatch = new CountDownLatch(1);
        final ActionListener<User> listener = ActionListener.wrap(user -> {
            listenerCalledLatch.countDown();
        }, e -> {
            throw new AssertionError("onFailure should not be executed");
        });
        final CancellableLdapRunnable runnable = new CancellableLdapRunnable(listener, () -> {
            runningLatch.countDown();
            try {
                timeoutCalledLatch.await();
                listener.onResponse(null);
            } catch (InterruptedException e) {
                throw new AssertionError("don't interrupt me", e);
            }
        }, logger);

        Thread t = new Thread(runnable);
        t.start();
        runningLatch.await();
        runnable.maybeTimeout();
        timeoutCalledLatch.countDown();
        listenerCalledLatch.await();
        t.join();
    }
}
