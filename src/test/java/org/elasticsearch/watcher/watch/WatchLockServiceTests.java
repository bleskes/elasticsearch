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

package org.elasticsearch.watcher.watch;

import org.elasticsearch.ElasticsearchIllegalStateException;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 */
public class WatchLockServiceTests extends ElasticsearchTestCase {

    @Test
    public void testLocking_notStarted() {
        WatchLockService lockService = new WatchLockService();
        try {
            lockService.acquire("_name");
            fail("exception expected");
        } catch (Exception e) {
            assertThat(e.getMessage(), equalTo("not started"));
        }
    }

    @Test
    public void testLocking() {
        WatchLockService lockService = new WatchLockService();
        lockService.start();
        WatchLockService.Lock lock = lockService.acquire("_name");
        assertThat(lockService.getWatchLocks().hasLockedKeys(), is(true));
        lock.release();
        assertThat(lockService.getWatchLocks().hasLockedKeys(), is(false));
        lockService.stop();
    }

    @Test
    public void testLocking_alreadyHeld() {
        WatchLockService lockService = new WatchLockService();
        lockService.start();
        WatchLockService.Lock lock1 = lockService.acquire("_name");
        try {
            lockService.acquire("_name");
            fail("exception expected");
        } catch (ElasticsearchIllegalStateException e) {
            assertThat(e.getMessage(), containsString("Lock already acquired"));
        }
        lock1.release();
        lockService.stop();
    }

}
