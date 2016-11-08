/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
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
package org.elasticsearch.xpack.prelert.job;

import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.io.stream.Writeable.Reader;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.prelert.job.ModelSizeStats.MemoryStatus;
import org.elasticsearch.xpack.prelert.job.quantiles.Quantiles;
import org.elasticsearch.xpack.prelert.support.AbstractSerializingTestCase;
import org.elasticsearch.xpack.prelert.utils.time.TimeUtils;

import java.util.Date;

public class ModelSnapshotTests extends AbstractSerializingTestCase<ModelSnapshot> {
    private static final Date DEFAULT_TIMESTAMP = new Date();
    private static final String DEFAULT_DESCRIPTION = "a snapshot";
    private static final String DEFAULT_ID = "my_id";
    private static final long DEFAULT_PRIORITY = 1234L;
    private static final int DEFAULT_DOC_COUNT = 7;
    private static final Date DEFAULT_LATEST_RESULT_TIMESTAMP = new Date(12345678901234L);
    private static final Date DEFAULT_LATEST_RECORD_TIMESTAMP = new Date(12345678904321L);


    public void testEquals_GivenSameObject() {
        ModelSnapshot modelSnapshot = new ModelSnapshot();

        assertTrue(modelSnapshot.equals(modelSnapshot));
    }


    public void testEquals_GivenObjectOfDifferentClass() {
        ModelSnapshot modelSnapshot = new ModelSnapshot();

        assertFalse(modelSnapshot.equals("a string"));
    }


    public void testEquals_GivenEqualModelSnapshots() {
        ModelSnapshot modelSnapshot1 = createFullyPopulated();
        ModelSnapshot modelSnapshot2 = createFullyPopulated();
        modelSnapshot2.setTimestamp(modelSnapshot1.getTimestamp());

        assertTrue(modelSnapshot1.equals(modelSnapshot2));
        assertTrue(modelSnapshot2.equals(modelSnapshot1));
        assertEquals(modelSnapshot1.hashCode(), modelSnapshot2.hashCode());
    }


    public void testEquals_GivenDifferentTimestamp() {
        ModelSnapshot modelSnapshot1 = createFullyPopulated();
        ModelSnapshot modelSnapshot2 = createFullyPopulated();
        modelSnapshot2.setTimestamp(new Date(modelSnapshot2.getTimestamp().getTime() + 1));

        assertFalse(modelSnapshot1.equals(modelSnapshot2));
        assertFalse(modelSnapshot2.equals(modelSnapshot1));
    }


    public void testEquals_GivenDifferentDescription() {
        ModelSnapshot modelSnapshot1 = createFullyPopulated();
        ModelSnapshot modelSnapshot2 = createFullyPopulated();
        modelSnapshot2.setDescription(modelSnapshot2.getDescription() + " blah");

        assertFalse(modelSnapshot1.equals(modelSnapshot2));
        assertFalse(modelSnapshot2.equals(modelSnapshot1));
    }


    public void testEquals_GivenDifferentRestorePriority() {
        ModelSnapshot modelSnapshot1 = createFullyPopulated();
        ModelSnapshot modelSnapshot2 = createFullyPopulated();
        modelSnapshot2.setRestorePriority(modelSnapshot2.getRestorePriority() + 1);

        assertFalse(modelSnapshot1.equals(modelSnapshot2));
        assertFalse(modelSnapshot2.equals(modelSnapshot1));
    }


    public void testEquals_GivenDifferentId() {
        ModelSnapshot modelSnapshot1 = createFullyPopulated();
        ModelSnapshot modelSnapshot2 = createFullyPopulated();
        modelSnapshot2.setSnapshotId(modelSnapshot2.getSnapshotId() + "_2");

        assertFalse(modelSnapshot1.equals(modelSnapshot2));
        assertFalse(modelSnapshot2.equals(modelSnapshot1));
    }


    public void testEquals_GivenDifferentDocCount() {
        ModelSnapshot modelSnapshot1 = createFullyPopulated();
        ModelSnapshot modelSnapshot2 = createFullyPopulated();
        modelSnapshot2.setSnapshotDocCount(modelSnapshot2.getSnapshotDocCount() + 1);

        assertFalse(modelSnapshot1.equals(modelSnapshot2));
        assertFalse(modelSnapshot2.equals(modelSnapshot1));
    }


    public void testEquals_GivenDifferentModelSizeStats() {
        ModelSnapshot modelSnapshot1 = createFullyPopulated();
        ModelSnapshot modelSnapshot2 = createFullyPopulated();
        ModelSizeStats.Builder modelSizeStats = new ModelSizeStats.Builder();
        modelSizeStats.setModelBytes(42L);
        modelSnapshot2.setModelSizeStats(modelSizeStats);

        assertFalse(modelSnapshot1.equals(modelSnapshot2));
        assertFalse(modelSnapshot2.equals(modelSnapshot1));
    }


    public void testEquals_GivenDifferentQuantiles() {
        ModelSnapshot modelSnapshot1 = createFullyPopulated();
        ModelSnapshot modelSnapshot2 = createFullyPopulated();
        modelSnapshot2.setQuantiles(new Quantiles(modelSnapshot2.getQuantiles().getTimestamp(), "different state"));

        assertFalse(modelSnapshot1.equals(modelSnapshot2));
        assertFalse(modelSnapshot2.equals(modelSnapshot1));
    }


    public void testEquals_GivenDifferentLatestResultTimestamp() {
        ModelSnapshot modelSnapshot1 = createFullyPopulated();
        ModelSnapshot modelSnapshot2 = createFullyPopulated();
        modelSnapshot2.setLatestResultTimeStamp(
                new Date(modelSnapshot2.getLatestResultTimeStamp().getTime() + 1));

        assertFalse(modelSnapshot1.equals(modelSnapshot2));
        assertFalse(modelSnapshot2.equals(modelSnapshot1));
    }


    public void testEquals_GivenDifferentLatestRecordTimestamp() {
        ModelSnapshot modelSnapshot1 = createFullyPopulated();
        ModelSnapshot modelSnapshot2 = createFullyPopulated();
        modelSnapshot2.setLatestRecordTimeStamp(
                new Date(modelSnapshot2.getLatestRecordTimeStamp().getTime() + 1));

        assertFalse(modelSnapshot1.equals(modelSnapshot2));
        assertFalse(modelSnapshot2.equals(modelSnapshot1));
    }


    private static ModelSnapshot createFullyPopulated() {
        ModelSnapshot modelSnapshot = new ModelSnapshot();
        modelSnapshot.setTimestamp(DEFAULT_TIMESTAMP);
        modelSnapshot.setDescription(DEFAULT_DESCRIPTION);
        modelSnapshot.setRestorePriority(DEFAULT_PRIORITY);
        modelSnapshot.setSnapshotId(DEFAULT_ID);
        modelSnapshot.setSnapshotDocCount(DEFAULT_DOC_COUNT);
        modelSnapshot.setModelSizeStats(new ModelSizeStats.Builder());
        modelSnapshot.setLatestResultTimeStamp(DEFAULT_LATEST_RESULT_TIMESTAMP);
        modelSnapshot.setLatestRecordTimeStamp(DEFAULT_LATEST_RECORD_TIMESTAMP);
        modelSnapshot.setQuantiles(new Quantiles(DEFAULT_TIMESTAMP, "state"));
        return modelSnapshot;
    }

    @Override
    protected ModelSnapshot createTestInstance() {
        ModelSnapshot modelSnapshot = new ModelSnapshot();
        modelSnapshot.setTimestamp(new Date(TimeUtils.dateStringToEpoch(randomTimeValue())));
        modelSnapshot.setDescription(randomAsciiOfLengthBetween(1, 20));
        modelSnapshot.setRestorePriority(randomLong());
        modelSnapshot.setSnapshotId(randomAsciiOfLengthBetween(1, 20));
        modelSnapshot.setSnapshotDocCount(randomInt());
        ModelSizeStats.Builder stats = new ModelSizeStats.Builder();
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
            stats.setId(randomAsciiOfLengthBetween(1, 20));
        }
        modelSnapshot.setModelSizeStats(stats);
        modelSnapshot.setLatestResultTimeStamp(new Date(TimeUtils.dateStringToEpoch(randomTimeValue())));
        modelSnapshot.setLatestRecordTimeStamp(new Date(TimeUtils.dateStringToEpoch(randomTimeValue())));
        Quantiles quantiles = new Quantiles(new Date(TimeUtils.dateStringToEpoch(randomTimeValue())), randomAsciiOfLengthBetween(0, 1000));
        modelSnapshot.setQuantiles(quantiles);
        return modelSnapshot;
    }

    @Override
    protected Reader<ModelSnapshot> instanceReader() {
        return ModelSnapshot::new;
    }

    @Override
    protected ModelSnapshot parseInstance(XContentParser parser, ParseFieldMatcher matcher) {
        return ModelSnapshot.PARSER.apply(parser, () -> matcher);
    }
}
