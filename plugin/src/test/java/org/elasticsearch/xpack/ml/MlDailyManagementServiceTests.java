/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2017 Elasticsearch BV. All Rights Reserved.
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
package org.elasticsearch.xpack.ml;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.threadpool.TestThreadPool;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.ml.action.DeleteExpiredDataAction;
import org.junit.After;
import org.junit.Before;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.elasticsearch.mock.orig.Mockito.verify;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;

public class MlDailyManagementServiceTests extends ESTestCase {

    private ThreadPool threadPool;
    private Client client;

    @Before
    public void setUpTests() {
        threadPool = new TestThreadPool("MlDailyManagementServiceTests");
        client = mock(Client.class);
    }

    @After
    public void stop() throws InterruptedException {
        terminate(threadPool);
    }

    public void testScheduledTriggering() throws InterruptedException {
        int triggerCount = randomIntBetween(2, 4);
        CountDownLatch latch = new CountDownLatch(triggerCount);
        try (MlDailyMaintenanceService service = createService(latch, client)) {
            service.start();
            latch.await(1, TimeUnit.SECONDS);
        }

        verify(client, org.mockito.Mockito.atLeast(triggerCount - 1)).execute(same(DeleteExpiredDataAction.INSTANCE), any());
    }

    private MlDailyMaintenanceService createService(CountDownLatch latch, Client client) {
        return new MlDailyMaintenanceService(threadPool, client, () -> {
                latch.countDown();
                return TimeValue.timeValueMillis(100);
            });
    }
}