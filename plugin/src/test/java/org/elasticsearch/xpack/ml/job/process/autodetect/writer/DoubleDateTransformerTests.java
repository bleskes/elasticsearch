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
package org.elasticsearch.xpack.ml.job.process.autodetect.writer;

import org.elasticsearch.test.ESTestCase;

public class DoubleDateTransformerTests extends ESTestCase {

    public void testTransform_GivenTimestampIsNotMilliseconds() throws CannotParseTimestampException {
        DoubleDateTransformer transformer = new DoubleDateTransformer(false);

        assertEquals(1000000, transformer.transform("1000"));
    }

    public void testTransform_GivenTimestampIsMilliseconds() throws CannotParseTimestampException {
        DoubleDateTransformer transformer = new DoubleDateTransformer(true);

        assertEquals(1000, transformer.transform("1000"));
    }

    public void testTransform_GivenTimestampIsNotValidDouble() throws CannotParseTimestampException {
        DoubleDateTransformer transformer = new DoubleDateTransformer(false);

        CannotParseTimestampException e = ESTestCase.expectThrows(CannotParseTimestampException.class,
                () -> transformer.transform("invalid"));
        assertEquals("Cannot parse timestamp 'invalid' as epoch value", e.getMessage());
    }
}
