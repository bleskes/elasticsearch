
package org.elasticsearch.xpack.prelert.job.config;

import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.Detector;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DefaultDetectorDescriptionTest extends ESTestCase {

    public void testOf_GivenEmptyDetector() {
        Detector detector = new Detector();

        assertEquals("", DefaultDetectorDescription.of(detector));
    }


    public void testOf_GivenOnlyFieldName() {
        Detector detector = new Detector();
        detector.setFieldName("value");

        assertEquals("value", DefaultDetectorDescription.of(detector));
    }


    public void testOf_GivenOnlyFunctionAndFieldName() {
        Detector detector = new Detector();
        detector.setFunction("min");
        detector.setFieldName("value");

        assertEquals("min(value)", DefaultDetectorDescription.of(detector));
    }


    public void testOf_GivenOnlyFunctionAndFieldNameWithNonWordChars() {
        Detector detector = new Detector();
        detector.setFunction("min");
        detector.setFieldName("val-ue");

        assertEquals("min(\"val-ue\")", DefaultDetectorDescription.of(detector));
    }


    public void testOf_GivenFullyPopulatedDetector() {
        Detector detector = new Detector();
        detector.setFunction("sum");
        detector.setFieldName("value");
        detector.setByFieldName("airline");
        detector.setOverFieldName("region");
        detector.setUseNull(true);
        detector.setPartitionFieldName("planet");
        detector.setExcludeFrequent("true");

        assertEquals("sum(value) by airline over region usenull=true partitionfield=planet excludefrequent=true",
                DefaultDetectorDescription.of(detector));
    }
}
