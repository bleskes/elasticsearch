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
package org.elasticsearch.xpack.ml.datafeed;

import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.ml.support.AbstractSerializingTestCase;

import static org.hamcrest.Matchers.is;

public class ChunkingConfigTests extends AbstractSerializingTestCase<ChunkingConfig> {

    @Override
    protected ChunkingConfig createTestInstance() {
        return createRandomizedChunk();
    }

    @Override
    protected Writeable.Reader<ChunkingConfig> instanceReader() {
        return ChunkingConfig::new;
    }

    @Override
    protected ChunkingConfig parseInstance(XContentParser parser) {
        return ChunkingConfig.PARSER.apply(parser, null);
    }

    public void testConstructorGivenAutoAndTimeSpan() {
        expectThrows(IllegalArgumentException.class, () ->new ChunkingConfig(ChunkingConfig.Mode.AUTO, TimeValue.timeValueMillis(1000)));
    }

    public void testConstructorGivenOffAndTimeSpan() {
        expectThrows(IllegalArgumentException.class, () ->new ChunkingConfig(ChunkingConfig.Mode.OFF, TimeValue.timeValueMillis(1000)));
    }

    public void testConstructorGivenManualAndNoTimeSpan() {
        expectThrows(IllegalArgumentException.class, () ->new ChunkingConfig(ChunkingConfig.Mode.MANUAL, null));
    }

    public void testIsEnabled() {
        assertThat(ChunkingConfig.newAuto().isEnabled(), is(true));
        assertThat(ChunkingConfig.newManual(TimeValue.timeValueMillis(1000)).isEnabled(), is(true));
        assertThat(ChunkingConfig.newOff().isEnabled(), is(false));
    }

    public static ChunkingConfig createRandomizedChunk() {
        ChunkingConfig.Mode mode = randomFrom(ChunkingConfig.Mode.values());
        TimeValue timeSpan = null;
        if (mode == ChunkingConfig.Mode.MANUAL) {
            timeSpan = TimeValue.parseTimeValue(randomPositiveTimeValue(), "test");
        }
        return new ChunkingConfig(mode, timeSpan);
     }
}