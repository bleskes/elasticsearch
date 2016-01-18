package com.prelert.rs.resources;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

public class ShutdownThreadBuilderTest
{
    @Test
    public void testBuild() throws InterruptedException
    {
        AtomicBoolean task1Shut = new AtomicBoolean(false);
        AtomicBoolean task2Shut = new AtomicBoolean(false);

        Thread shutdownThread = new ShutdownThreadBuilder()
                .addTask(() -> task1Shut.getAndSet(true))
                .addTask(() -> task2Shut.getAndSet(true))
                .build();
        shutdownThread.start();

        shutdownThread.join();

        assertTrue(task1Shut.get());
        assertTrue(task2Shut.get());
    }
}
