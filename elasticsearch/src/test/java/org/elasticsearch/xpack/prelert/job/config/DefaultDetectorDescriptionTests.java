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
package org.elasticsearch.xpack.prelert.job.config;

import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.Detector;

public class DefaultDetectorDescriptionTests extends ESTestCase {


    public void testOf_GivenOnlyFunctionAndFieldName() {
        Detector detector = new Detector.Builder("min", "value").build();

        assertEquals("min(value)", DefaultDetectorDescription.of(detector));
    }


    public void testOf_GivenOnlyFunctionAndFieldNameWithNonWordChars() {
        Detector detector = new Detector.Builder("min", "val-ue").build();

        assertEquals("min(\"val-ue\")", DefaultDetectorDescription.of(detector));
    }


    public void testOf_GivenFullyPopulatedDetector() {
        Detector.Builder detector = new Detector.Builder("sum", "value");
        detector.setByFieldName("airline");
        detector.setOverFieldName("region");
        detector.setUseNull(true);
        detector.setPartitionFieldName("planet");
        detector.setExcludeFrequent(Detector.ExcludeFrequent.ALL);

        assertEquals("sum(value) by airline over region usenull=true partitionfield=planet excludefrequent=all",
                DefaultDetectorDescription.of(detector.build()));
    }
}
