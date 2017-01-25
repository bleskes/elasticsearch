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

public class DateFormatDateTransformerTests extends ESTestCase {

    public void testTransform_GivenValidTimestamp() throws CannotParseTimestampException {
        DateFormatDateTransformer transformer = new DateFormatDateTransformer("yyyy-MM-dd HH:mm:ssXXX");

        assertEquals(1388534400000L, transformer.transform("2014-01-01 00:00:00Z"));
    }

    public void testTransform_GivenInvalidTimestamp() throws CannotParseTimestampException {
        DateFormatDateTransformer transformer = new DateFormatDateTransformer("yyyy-MM-dd HH:mm:ssXXX");

        CannotParseTimestampException e = ESTestCase.expectThrows(CannotParseTimestampException.class,
                () -> transformer.transform("invalid"));
        assertEquals("Cannot parse date 'invalid' with format string 'yyyy-MM-dd HH:mm:ssXXX'", e.getMessage());
    }
}
