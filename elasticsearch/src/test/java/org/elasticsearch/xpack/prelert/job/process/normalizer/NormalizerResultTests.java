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
package org.elasticsearch.xpack.prelert.job.process.normalizer;

import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.io.stream.Writeable.Reader;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.prelert.support.AbstractSerializingTestCase;

public class NormalizerResultTests extends AbstractSerializingTestCase<NormalizerResult> {

    private static final double EPSILON = 0.0000000001;

    public void testDefaultConstructor() {
        NormalizerResult msg = new NormalizerResult();
        assertNull(msg.getLevel());
        assertNull(msg.getPartitionFieldName());
        assertNull(msg.getPartitionFieldValue());
        assertNull(msg.getPersonFieldName());
        assertNull(msg.getFunctionName());
        assertNull(msg.getValueFieldName());
        assertEquals(0.0, msg.getProbability(), EPSILON);
        assertEquals(0.0, msg.getNormalizedScore(), EPSILON);
    }

    @Override
    protected NormalizerResult createTestInstance() {
        NormalizerResult msg = new NormalizerResult();
        msg.setLevel("leaf");
        msg.setPartitionFieldName("part");
        msg.setPartitionFieldValue("something");
        msg.setPersonFieldName("person");
        msg.setFunctionName("mean");
        msg.setValueFieldName("value");
        msg.setProbability(0.005);
        msg.setNormalizedScore(98.7);
        return msg;
    }

    @Override
    protected Reader<NormalizerResult> instanceReader() {
        return NormalizerResult::new;
    }

    @Override
    protected NormalizerResult parseInstance(XContentParser parser, ParseFieldMatcher matcher) {
        return NormalizerResult.PARSER.apply(parser, () -> matcher);
    }
}
