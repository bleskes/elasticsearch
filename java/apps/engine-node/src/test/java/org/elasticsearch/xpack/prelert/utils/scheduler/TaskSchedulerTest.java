package org.elasticsearch.xpack.prelert.utils.scheduler;

import org.elasticsearch.test.ESTestCase;
import org.junit.Before;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class TaskSchedulerTest extends ESTestCase {

    private AtomicInteger taskCount;

    @Before
    public void setUpTests() {
        taskCount = new AtomicInteger(0);
    }

    public void testTaskRunsPeriodically() throws InterruptedException {
        AtomicLong startMillis = new AtomicLong();
        CountDownLatch latch = new CountDownLatch(3);

        Runnable task = () -> {
            if (taskCount.incrementAndGet() == 1) {
                startMillis.set(System.currentTimeMillis());
            }
            latch.countDown();
        };

        TaskScheduler scheduler = new TaskScheduler(task, () -> LocalDateTime.now(ZoneId.systemDefault()).plus(50, ChronoUnit.MILLIS));
        scheduler.start();
        latch.await();
        scheduler.stop(1, TimeUnit.SECONDS);
        long endMillis = System.currentTimeMillis();

        assertTrue(taskCount.get() == 3);
        assertTrue(endMillis - startMillis.get() > 90);
        assertTrue(endMillis - startMillis.get() < 160);
    }

    public void testStop_BlocksUntilRunningTaskTerminates() throws InterruptedException {
        CountDownLatch firstTaskStartedLatch = new CountDownLatch(1);
        AtomicLong firstTaskStart = new AtomicLong(0);
        AtomicLong end = new AtomicLong(0);

        Runnable task = () -> {
            int id = taskCount.incrementAndGet();
            long now = System.currentTimeMillis();
            if (id == 1) {
                firstTaskStart.getAndSet(now);
                firstTaskStartedLatch.countDown();
            }
            while (now - firstTaskStart.get() <= 100) {
                now = System.currentTimeMillis();
            }
            end.getAndSet(now);
        };

        TaskScheduler scheduler = new TaskScheduler(task, () -> LocalDateTime.now(ZoneId.systemDefault()));
        scheduler.start();
        firstTaskStartedLatch.await();

        assertTrue(scheduler.stop(1, TimeUnit.SECONDS));

        assertTrue(System.currentTimeMillis() >= end.get());
        assertEquals(1, taskCount.get());
        assertTrue(end.get() - firstTaskStart.get() >= 100);
    }
}
