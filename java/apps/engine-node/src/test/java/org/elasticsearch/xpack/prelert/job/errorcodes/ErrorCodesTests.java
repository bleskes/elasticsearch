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
package org.elasticsearch.xpack.prelert.job.errorcodes;

import org.elasticsearch.test.ESTestCase;

import java.util.HashSet;
import java.util.Set;

/**
 * This test ensures that all the error values in {@linkplain ErrorCodes} are
 * unique so no 2 conditions can return the same error code. This tests is
 * designed to catch copy/paste errors.
 */
public class ErrorCodesTests extends ESTestCase {
    public void testErrorCodesAreUnique() throws IllegalArgumentException, IllegalAccessException {
        ErrorCodes[] values = ErrorCodes.class.getEnumConstants();

        Set<Long> errorValueSet = new HashSet<>();

        for (ErrorCodes value : values) {
            errorValueSet.add(value.getValue());
        }

        assertEquals(values.length, errorValueSet.size());
    }
}
