
package org.elasticsearch.xpack.prelert.job;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.io.stream.Writeable.Reader;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.prelert.job.detectionrules.Connective;
import org.elasticsearch.xpack.prelert.job.detectionrules.DetectionRule;
import org.elasticsearch.xpack.prelert.job.detectionrules.RuleCondition;
import org.elasticsearch.xpack.prelert.job.detectionrules.RuleConditionType;
import org.elasticsearch.xpack.prelert.support.AbstractSerializingTestCase;

public class DetectorTest extends AbstractSerializingTestCase<Detector> {

    public void testEquals_GivenEqual() {
        Detector detector1 = new Detector("foo", "mean", "field");
        detector1.setByFieldName("by");
        detector1.setOverFieldName("over");
        detector1.setPartitionFieldName("partition");
        detector1.setUseNull(false);

        Detector detector2 = new Detector("foo", "mean", "field");
        detector2.setByFieldName("by");
        detector2.setOverFieldName("over");
        detector2.setPartitionFieldName("partition");
        detector2.setUseNull(false);

        assertTrue(detector1.equals(detector2));
        assertTrue(detector2.equals(detector1));
        assertEquals(detector1.hashCode(), detector2.hashCode());
    }


    public void testEquals_GivenDifferentDetectorDescription() {
        Detector detector1 = createDetector();
        Detector detector2 = createDetector();
        detector2.setDetectorDescription("bar");

        assertFalse(detector1.equals(detector2));
    }


    public void testEquals_GivenDifferentByFieldName() {
        Detector detector1 = createDetector();
        Detector detector2 = createDetector();

        assertEquals(detector1, detector2);

        detector2.setByFieldName("aa");
        assertFalse(detector1.equals(detector2));
    }


    public void testEquals_GivenDifferentRules() {
        Detector detector1 = createDetector();
        Detector detector2 = createDetector();
        detector2.getDetectorRules().get(0).setConditionsConnective(Connective.AND);

        assertFalse(detector1.equals(detector2));
        assertFalse(detector2.equals(detector1));
    }


    public void testExtractAnalysisFields() {
        Detector detector = createDetector();
        assertEquals(Arrays.asList("by", "over", "partition"), detector.extractAnalysisFields());
        detector.setPartitionFieldName(null);
        assertEquals(Arrays.asList("by", "over"), detector.extractAnalysisFields());
        detector.setByFieldName(null);
        assertEquals(Arrays.asList("over"), detector.extractAnalysisFields());
        detector.setOverFieldName(null);
        assertTrue(detector.extractAnalysisFields().isEmpty());
    }


    public void testExtractReferencedLists() {
        Detector detector = createDetector();
        detector.setDetectorRules(Arrays.asList(new DetectionRule(), new DetectionRule()));
        detector.getDetectorRules().get(0).setRuleConditions(Arrays.asList(
                RuleCondition.createCategorical("foo", "list1")));
        detector.getDetectorRules().get(1).setRuleConditions(Arrays.asList(
                RuleCondition.createCategorical("bar", "list2")));

        assertEquals(new HashSet<String>(Arrays.asList("list1", "list2")), detector.extractReferencedLists());
    }

    private Detector createDetector() {
        Detector detector = new Detector("foo", "mean", "field");
        detector.setByFieldName("by");
        detector.setOverFieldName("over");
        detector.setPartitionFieldName("partition");
        detector.setUseNull(true);

        DetectionRule rule = new DetectionRule();
        detector.setDetectorRules(Arrays.asList(rule));

        return detector;
    }

    @Override
    protected Detector createTestInstance() {
        Detector detector;
        if (randomBoolean()) {
            detector = new Detector(frequently() ? randomAsciiOfLengthBetween(1, 100) : null,
                    randomFrom(Detector.COUNT_WITHOUT_FIELD_FUNCTIONS));
        } else {
            detector = new Detector(frequently() ? randomAsciiOfLengthBetween(1, 100) : null, randomFrom(Detector.FIELD_NAME_FUNCTIONS),
                    randomAsciiOfLengthBetween(1, 20));
        }
        if (randomBoolean()) {
            detector.setByFieldName(randomAsciiOfLengthBetween(1, 20));
        }
        if (randomBoolean()) {
            detector.setOverFieldName(randomAsciiOfLengthBetween(1, 20));
        }
        if (randomBoolean()) {
            detector.setPartitionFieldName(randomAsciiOfLengthBetween(1, 20));
        }
        if (randomBoolean()) {
            detector.setExcludeFrequent(randomAsciiOfLengthBetween(1, 20));
        }
        if (randomBoolean()) {
            int size = randomInt(10);
            List<DetectionRule> detectorRules = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                DetectionRule detectionRule = new DetectionRule();
                if (randomBoolean()) {
                    detectionRule.setConditionsConnective(randomFrom(Connective.values()));
                }
                if (randomBoolean()) {
                    detectionRule.setTargetFieldName(randomAsciiOfLengthBetween(1, 20));
                }
                if (randomBoolean()) {
                    detectionRule.setTargetFieldValue(randomAsciiOfLengthBetween(1, 20));
                }
                if (randomBoolean()) {
                    detectionRule.setRuleConditions(Collections.singletonList(new RuleCondition(randomFrom(RuleConditionType.values()))));
                }
                detectorRules.add(detectionRule);
            }
            detector.setDetectorRules(detectorRules);
        }
        if (randomBoolean()) {
            detector.setUseNull(randomBoolean());
        }
        return detector;
    }

    @Override
    protected Reader<Detector> instanceReader() {
        return Detector::new;
    }

    @Override
    protected Detector parseInstance(XContentParser parser, ParseFieldMatcher matcher) {
        return Detector.PARSER.apply(parser, () -> matcher);
    }
}
