
package org.elasticsearch.xpack.prelert.job;

import java.util.Date;

import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.io.stream.Writeable.Reader;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.prelert.job.ModelSizeStats.MemoryStatus;
import org.elasticsearch.xpack.prelert.support.AbstractSerializingTestCase;

public class ModelSizeStatsTest extends AbstractSerializingTestCase<ModelSizeStats> {

    public void testDefaultConstructor() {
        ModelSizeStats stats = new ModelSizeStats();
        assertEquals("modelSizeStats", stats.getModelSizeStatsId());
        assertEquals(0, stats.getModelBytes());
        assertEquals(0, stats.getTotalByFieldCount());
        assertEquals(0, stats.getTotalOverFieldCount());
        assertEquals(0, stats.getTotalPartitionFieldCount());
        assertEquals(0, stats.getBucketAllocationFailuresCount());
        assertEquals(MemoryStatus.OK, stats.getMemoryStatus());
    }


    public void testSetModelSizeStatsId() {
        ModelSizeStats stats = new ModelSizeStats();

        stats.setModelSizeStatsId("foo");

        assertEquals("foo", stats.getModelSizeStatsId());
    }


    public void testSetMemoryStatus_GivenNull() {
        ModelSizeStats stats = new ModelSizeStats();

        NullPointerException ex = expectThrows(NullPointerException.class, () -> stats.setMemoryStatus(null));

        assertEquals("[memory_status] must not be null", ex.getMessage());
    }


    public void testSetMemoryStatus_GivenSoftLimit() {
        ModelSizeStats stats = new ModelSizeStats();

        stats.setMemoryStatus(MemoryStatus.SOFT_LIMIT);

        assertEquals(MemoryStatus.SOFT_LIMIT, stats.getMemoryStatus());
    }

    @Override
    protected ModelSizeStats createTestInstance() {
        ModelSizeStats stats = new ModelSizeStats();
        if (randomBoolean()) {
            stats.setBucketAllocationFailuresCount(randomPositiveLong());
        }
        if (randomBoolean()) {
            stats.setModelBytes(randomPositiveLong());
        }
        if (randomBoolean()) {
            stats.setTotalByFieldCount(randomPositiveLong());
        }
        if (randomBoolean()) {
            stats.setTotalOverFieldCount(randomPositiveLong());
        }
        if (randomBoolean()) {
            stats.setTotalPartitionFieldCount(randomPositiveLong());
        }
        if (randomBoolean()) {
            stats.setLogTime(new Date(randomLong()));
        }
        if (randomBoolean()) {
            stats.setTimestamp(new Date(randomLong()));
        }
        if (randomBoolean()) {
            stats.setMemoryStatus(randomFrom(MemoryStatus.values()));
        }
        if (randomBoolean()) {
            stats.setModelSizeStatsId(randomAsciiOfLengthBetween(1, 20));
        }
        return stats;
    }

    @Override
    protected Reader<ModelSizeStats> instanceReader() {
        return ModelSizeStats::new;
    }

    @Override
    protected ModelSizeStats parseInstance(XContentParser parser, ParseFieldMatcher matcher) {
        return ModelSizeStats.PARSER.apply(parser, () -> matcher);
    }
}
