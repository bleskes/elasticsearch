/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
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
package org.elasticsearch.xpack.ml.job.config;

import org.elasticsearch.common.io.stream.Writeable.Reader;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.ml.job.messages.Messages;
import org.elasticsearch.xpack.ml.support.AbstractSerializingTestCase;

public class ModelDebugConfigTests extends AbstractSerializingTestCase<ModelDebugConfig> {

    public void testVerify_GivenBoundPercentileLessThanZero() {
        IllegalArgumentException e = ESTestCase.expectThrows(IllegalArgumentException.class, () -> new ModelDebugConfig(-1.0, ""));
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_MODEL_DEBUG_CONFIG_INVALID_BOUNDS_PERCENTILE, ""), e.getMessage());
    }

    public void testVerify_GivenBoundPercentileGreaterThan100() {
        IllegalArgumentException e = ESTestCase.expectThrows(IllegalArgumentException.class, () -> new ModelDebugConfig(100.1, ""));
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_MODEL_DEBUG_CONFIG_INVALID_BOUNDS_PERCENTILE, ""), e.getMessage());
    }

    public void testVerify_GivenValid() {
        new ModelDebugConfig(93.0, "");
        new ModelDebugConfig(93.0, "foo,bar");
    }

    @Override
    protected ModelDebugConfig createTestInstance() {
        return new ModelDebugConfig(randomDouble(), randomAsciiOfLengthBetween(1, 30));
    }

    @Override
    protected Reader<ModelDebugConfig> instanceReader() {
        return ModelDebugConfig::new;
    }

    @Override
    protected ModelDebugConfig parseInstance(XContentParser parser) {
        return ModelDebugConfig.PARSER.apply(parser, null);
    }
}
