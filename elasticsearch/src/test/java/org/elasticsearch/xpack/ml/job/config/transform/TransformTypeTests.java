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
package org.elasticsearch.xpack.ml.job.config.transform;

import org.elasticsearch.test.ESTestCase;

import java.util.EnumSet;
import java.util.Set;

public class TransformTypeTests extends ESTestCase {

    public void testFromString() {
        Set<TransformType> all = EnumSet.allOf(TransformType.class);

        for (TransformType type : all) {
            assertEquals(type.prettyName(), type.toString());

            TransformType created = TransformType.fromString(type.prettyName());
            assertEquals(type, created);
        }
    }

    public void testFromString_UnknownType() {
        ESTestCase.expectThrows(IllegalArgumentException.class, () -> TransformType.fromString("random_type"));
    }

    public void testForString() {
        assertEquals(TransformType.fromString("domain_split"), TransformType.DOMAIN_SPLIT);
        assertEquals(TransformType.fromString("concat"), TransformType.CONCAT);
        assertEquals(TransformType.fromString("extract"), TransformType.REGEX_EXTRACT);
        assertEquals(TransformType.fromString("split"), TransformType.REGEX_SPLIT);
        assertEquals(TransformType.fromString("exclude"), TransformType.EXCLUDE);
        assertEquals(TransformType.fromString("lowercase"), TransformType.LOWERCASE);
        assertEquals(TransformType.fromString("uppercase"), TransformType.UPPERCASE);
        assertEquals(TransformType.fromString("trim"), TransformType.TRIM);
    }

    public void testValidOrdinals() {
        assertEquals(0, TransformType.DOMAIN_SPLIT.ordinal());
        assertEquals(1, TransformType.CONCAT.ordinal());
        assertEquals(2, TransformType.REGEX_EXTRACT.ordinal());
        assertEquals(3, TransformType.REGEX_SPLIT.ordinal());
        assertEquals(4, TransformType.EXCLUDE.ordinal());
        assertEquals(5, TransformType.LOWERCASE.ordinal());
        assertEquals(6, TransformType.UPPERCASE.ordinal());
        assertEquals(7, TransformType.TRIM.ordinal());
    }

}
