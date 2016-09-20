
package org.elasticsearch.xpack.prelert.job;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ModelSizeStatsTest {
    @Test
    public void testDefaultConstructor() {
        ModelSizeStats stats = new ModelSizeStats();
        assertEquals("modelSizeStats", stats.getModelSizeStatsId());
        assertEquals(0, stats.getModelBytes());
        assertEquals(0, stats.getTotalByFieldCount());
        assertEquals(0, stats.getTotalOverFieldCount());
        assertEquals(0, stats.getTotalPartitionFieldCount());
        assertEquals(0, stats.getBucketAllocationFailuresCount());
        assertEquals("OK", stats.getMemoryStatus());
    }

    @Test
    public void testSetModelSizeStatsId() {
        ModelSizeStats stats = new ModelSizeStats();

        stats.setModelSizeStatsId("foo");

        assertEquals("foo", stats.getModelSizeStatsId());
    }

    @Test
    public void testSetMemoryStatus_GivenNull() {
        ModelSizeStats stats = new ModelSizeStats();

        stats.setMemoryStatus(null);

        assertEquals("OK", stats.getMemoryStatus());
    }

    @Test
    public void testSetMemoryStatus_GivenEmpty() {
        ModelSizeStats stats = new ModelSizeStats();

        stats.setMemoryStatus("");

        assertEquals("OK", stats.getMemoryStatus());
    }

    @Test
    public void testSetMemoryStatus_GivenSoftLimit() {
        ModelSizeStats stats = new ModelSizeStats();

        stats.setMemoryStatus("SOFT_LIMIT");

        assertEquals("SOFT_LIMIT", stats.getMemoryStatus());
    }
}
