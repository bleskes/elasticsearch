
package org.elasticsearch.xpack.prelert.job.config.verification;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.Detector;
import org.elasticsearch.xpack.prelert.job.condition.Condition;
import org.elasticsearch.xpack.prelert.job.condition.Operator;
import org.elasticsearch.xpack.prelert.job.detectionrules.DetectionRule;
import org.elasticsearch.xpack.prelert.job.detectionrules.RuleCondition;
import org.elasticsearch.xpack.prelert.job.detectionrules.RuleConditionType;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.junit.Assert;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


/**
 * Test the valid detector/functions/field combinations i.e. the
 * logic encoded in the table below
 * <p>
 * <table cellpadding="4px" border="1" width="90%"><colgroup><col class="col_1" /><col class="col_2" /><col class="col_3" /><col class="col_4" /><col class="col_5" /></colgroup>
 * <thead>
 * <tr><th align="left" valign="top">name</th><th align="left" valign="top">description</th><th align="left" valign="top">fieldName</th><th align="left" valign="top">byFieldName</th><th align="left" valign="top">overFieldName</th></tr></thead><tbody>
 * <tr><td align="left" valign="top"><p><a class="link" href="functions-count.html" title="count">count</a></p></td><td align="left" valign="top"><p>individual count</p></td><td align="left" valign="top"><p>N/A</p></td><td align="left" valign="top"><p>optional</p></td><td align="left" valign="top"><p>optional</p></td></tr>
 * <tr><td align="left" valign="top"><p><a class="link" href="functions-high_count.html" title="high_count">high_count</a></p></td><td align="left" valign="top"><p>individual count (high only)</p></td><td align="left" valign="top"><p>N/A</p></td><td align="left" valign="top"><p>optional</p></td><td align="left" valign="top"><p>optional</p></td></tr>
 * <tr><td align="left" valign="top"><p><a class="link" href="functions-low_count.html" title="low_count">low_count</a></p></td><td align="left" valign="top"><p>individual count (low only)</p></td><td align="left" valign="top"><p>N/A</p></td><td align="left" valign="top"><p>optional</p></td><td align="left" valign="top"><p>optional</p></td></tr>
 * <tr><td align="left" valign="top"><p><a class="link" href="functions-non_zero_count.html" title="non_zero_count or nzc">non_zero_count or nzc</a></p></td><td align="left" valign="top"><p>"count, but zeros are null, not zero"</p></td><td align="left" valign="top"><p>N/A</p></td><td align="left" valign="top"><p>optional</p></td><td align="left" valign="top"><p>N/A</p></td></tr>
 * <tr><td align="left" valign="top"><p><a class="link" href="functions-low_non_zero_count.html" title="low_non_zero_count or low_nzc">low_non_zero_count or low_nzc</a></p></td><td align="left" valign="top"><p>"low count, but zeros are null, not zero"</p></td><td align="left" valign="top"><p>N/A</p></td><td align="left" valign="top"><p>optional</p></td><td align="left" valign="top"><p>N/A</p></td></tr>
 * <tr><td align="left" valign="top"><p><a class="link" href="functions-high_non_zero_count.html" title="high_non_zero_count or high_nzc">high_non_zero_count or high_nzc</a></p></td><td align="left" valign="top"><p>"high count, but zeros are null, not zero"</p></td><td align="left" valign="top"><p>N/A</p></td><td align="left" valign="top"><p>optional</p></td><td align="left" valign="top"><p>N/A</p></td></tr>
 * <tr><td align="left" valign="top"><p><a class="link" href="functions-distinct_count.html" title="distinct_count or dc">distinct_count or dc</a></p></td><td align="left" valign="top"><p>distinct count</p></td><td align="left" valign="top"><p>required</p></td><td align="left" valign="top"><p>N/A</p></td><td align="left" valign="top"><p>required</p></td></tr>
 * <tr><td align="left" valign="top"><p><a class="link" href="functions-rare.html" title="rare">rare</a></p></td><td align="left" valign="top"><p>rare items</p></td><td align="left" valign="top"><p>N/A</p></td><td align="left" valign="top"><p>required</p></td><td align="left" valign="top"><p>optional</p></td></tr>
 * <tr><td align="left" valign="top"><p><a class="link" href="functions-freq_rare.html" title="freq_rare">freq_rare</a></p></td><td align="left" valign="top"><p>frequently rare items</p></td><td align="left" valign="top"><p>N/A</p></td><td align="left" valign="top"><p>required</p></td><td align="left" valign="top"><p>required</p></td></tr>
 * <tr><td align="left" valign="top"><p><a class="link" href="functions-metric.html" title="metric">metric</a></p></td><td align="left" valign="top"><p>all of mean, min, max and sum</p></td><td align="left" valign="top"><p>required</p></td><td align="left" valign="top"><p>optional</p></td><td align="left" valign="top"><p>optional</p></td></tr>
 * <tr><td align="left" valign="top"><p><a class="link" href="functions-mean.html" title="mean or avg">mean or avg</a></p></td><td align="left" valign="top"><p>arithmetic mean</p></td><td align="left" valign="top"><p>required</p></td><td align="left" valign="top"><p>optional</p></td><td align="left" valign="top"><p>optional</p></td></tr>
 * <tr><td align="left" valign="top"><p><a class="link" href="functions-min.html" title="min">min</a></p></td><td align="left" valign="top"><p>arithmetic minimum</p></td><td align="left" valign="top"><p>required</p></td><td align="left" valign="top"><p>optional</p></td><td align="left" valign="top"><p>optional</p></td></tr>
 * <tr><td align="left" valign="top"><p><a class="link" href="functions-max.html" title="max">max</a></p></td><td align="left" valign="top"><p>arithmetic maximum</p></td><td align="left" valign="top"><p>required</p></td><td align="left" valign="top"><p>optional</p></td><td align="left" valign="top"><p>optional</p></td></tr>
 * <tr><td align="left" valign="top"><p><a class="link" href="functions-sum.html" title="sum">sum</a></p></td><td align="left" valign="top"><p>arithmetic sum</p></td><td align="left" valign="top"><p>required</p></td><td align="left" valign="top"><p>optional</p></td><td align="left" valign="top"><p>optional</p></td></tr>
 * </tbody>
 * </table>
 */
public class DetectorVerifierTest extends ESTestCase {

    /**
     * Test the good/bad detector configurations
     *
     * @throws ElasticsearchParseException
     */
    public void testVerify() throws ElasticsearchParseException {
        // if nothing else is set the count functions (excluding distinct count)
        // are the only allowable functions
        Detector d = new Detector(Detector.COUNT);
        DetectorVerifier.verify(d, false);
        DetectorVerifier.verify(d, true);

        Set<String> difference = new HashSet<String>(Detector.ANALYSIS_FUNCTIONS);
        difference.remove(Detector.COUNT);
        difference.remove(Detector.HIGH_COUNT);
        difference.remove(Detector.LOW_COUNT);
        difference.remove(Detector.NON_ZERO_COUNT);
        difference.remove(Detector.NZC);
        difference.remove(Detector.LOW_NON_ZERO_COUNT);
        difference.remove(Detector.LOW_NZC);
        difference.remove(Detector.HIGH_NON_ZERO_COUNT);
        difference.remove(Detector.HIGH_NZC);
        difference.remove(Detector.TIME_OF_DAY);
        difference.remove(Detector.TIME_OF_WEEK);
        for (String f : difference) {
            try {
                d = new Detector(f);
                DetectorVerifier.verify(d, false);
                Assert.fail("ElasticsearchParseException not thrown when expected");
            } catch (ElasticsearchParseException e) {
            }
            try {
                d = new Detector(f);
                DetectorVerifier.verify(d, true);
                Assert.fail("ElasticsearchParseException not thrown when expected");
            } catch (ElasticsearchParseException e) {
            }
        }

        // certain fields aren't allowed with certain functions
        // first do the over field
        for (String f : new String[]{Detector.NON_ZERO_COUNT, Detector.NZC,
                Detector.LOW_NON_ZERO_COUNT, Detector.LOW_NZC, Detector.HIGH_NON_ZERO_COUNT,
                Detector.HIGH_NZC}) {
            d = new Detector(f);
            d.setOverFieldName("over");
            try {
                DetectorVerifier.verify(d, false);
                Assert.fail("ElasticsearchParseException not thrown when expected");
            } catch (ElasticsearchParseException e) {
            }
            try {
                DetectorVerifier.verify(d, true);
                Assert.fail("ElasticsearchParseException not thrown when expected");
            } catch (ElasticsearchParseException e) {
            }
        }

        // these functions cannot have just an over field
        difference = new HashSet<String>(Detector.ANALYSIS_FUNCTIONS);
        difference.remove(Detector.COUNT);
        difference.remove(Detector.HIGH_COUNT);
        difference.remove(Detector.LOW_COUNT);
        difference.remove(Detector.TIME_OF_DAY);
        difference.remove(Detector.TIME_OF_WEEK);
        for (String f : difference) {
            d = new Detector(f);
            d.setOverFieldName("over");
            try {
                DetectorVerifier.verify(d, false);
                Assert.fail("ElasticsearchParseException not thrown when expected");
            } catch (ElasticsearchParseException e) {
            }
            try {
                DetectorVerifier.verify(d, true);
                Assert.fail("ElasticsearchParseException not thrown when expected");
            } catch (ElasticsearchParseException e) {
            }
        }

        // these functions can have just an over field
        for (String f : new String[]{Detector.COUNT, Detector.HIGH_COUNT,
                Detector.LOW_COUNT}) {
            d = new Detector(f);
            d.setOverFieldName("over");
            DetectorVerifier.verify(d, false);
            DetectorVerifier.verify(d, true);
        }

        for (String f : new String[]{Detector.RARE, Detector.FREQ_RARE}) {
            d = new Detector(f);
            d.setByFieldName("by");
            d.setOverFieldName("over");
            DetectorVerifier.verify(d, false);
            DetectorVerifier.verify(d, true);
        }


        // some functions require a fieldname
        for (String f : new String[]{Detector.DISTINCT_COUNT, Detector.DC,
                Detector.HIGH_DISTINCT_COUNT, Detector.HIGH_DC, Detector.LOW_DISTINCT_COUNT, Detector.LOW_DC,
                Detector.INFO_CONTENT, Detector.LOW_INFO_CONTENT, Detector.HIGH_INFO_CONTENT,
                Detector.METRIC, Detector.MEAN, Detector.HIGH_MEAN, Detector.LOW_MEAN, Detector.AVG,
                Detector.HIGH_AVG, Detector.LOW_AVG, Detector.MAX, Detector.MIN, Detector.SUM,
                Detector.LOW_SUM, Detector.HIGH_SUM, Detector.NON_NULL_SUM,
                Detector.LOW_NON_NULL_SUM, Detector.HIGH_NON_NULL_SUM, Detector.POPULATION_VARIANCE,
                Detector.LOW_POPULATION_VARIANCE, Detector.HIGH_POPULATION_VARIANCE}) {
            d = new Detector(f, "f");
            d.setOverFieldName("over");
            DetectorVerifier.verify(d, false);
            try {
                DetectorVerifier.verify(d, true);
                Assert.assertFalse(Detector.METRIC.equals(f));
            } catch (ElasticsearchParseException e) {
                // "metric" is not allowed as the function for pre-summarised input
                Assert.assertEquals(Detector.METRIC, f);
            }
        }

        // these functions cannot have a field name
        difference = new HashSet<String>(Detector.ANALYSIS_FUNCTIONS);
        difference.remove(Detector.METRIC);
        difference.remove(Detector.MEAN);
        difference.remove(Detector.LOW_MEAN);
        difference.remove(Detector.HIGH_MEAN);
        difference.remove(Detector.AVG);
        difference.remove(Detector.LOW_AVG);
        difference.remove(Detector.HIGH_AVG);
        difference.remove(Detector.MEDIAN);
        difference.remove(Detector.MIN);
        difference.remove(Detector.MAX);
        difference.remove(Detector.SUM);
        difference.remove(Detector.LOW_SUM);
        difference.remove(Detector.HIGH_SUM);
        difference.remove(Detector.NON_NULL_SUM);
        difference.remove(Detector.LOW_NON_NULL_SUM);
        difference.remove(Detector.HIGH_NON_NULL_SUM);
        difference.remove(Detector.POPULATION_VARIANCE);
        difference.remove(Detector.LOW_POPULATION_VARIANCE);
        difference.remove(Detector.HIGH_POPULATION_VARIANCE);
        difference.remove(Detector.DISTINCT_COUNT);
        difference.remove(Detector.HIGH_DISTINCT_COUNT);
        difference.remove(Detector.LOW_DISTINCT_COUNT);
        difference.remove(Detector.DC);
        difference.remove(Detector.LOW_DC);
        difference.remove(Detector.HIGH_DC);
        difference.remove(Detector.INFO_CONTENT);
        difference.remove(Detector.LOW_INFO_CONTENT);
        difference.remove(Detector.HIGH_INFO_CONTENT);
        difference.remove(Detector.LAT_LONG);
        for (String f : difference) {
            d = new Detector(f, "f");
            d.setOverFieldName("over");
            try {
                DetectorVerifier.verify(d, false);
                Assert.fail("ElasticsearchParseException not thrown when expected");
            } catch (ElasticsearchParseException e) {
            }
            try {
                DetectorVerifier.verify(d, true);
                Assert.fail("ElasticsearchParseException not thrown when expected");
            } catch (ElasticsearchParseException e) {
            }
        }

        // these can have a by field
        for (String f : new String[]{Detector.COUNT, Detector.HIGH_COUNT,
                Detector.LOW_COUNT, Detector.RARE,
                Detector.NON_ZERO_COUNT, Detector.NZC}) {
            d = new Detector(f);
            d.setByFieldName("b");
            DetectorVerifier.verify(d, false);
            DetectorVerifier.verify(d, true);
        }

        d = new Detector(Detector.FREQ_RARE);
        d.setByFieldName("b");
        d.setOverFieldName("over");
        DetectorVerifier.verify(d, false);
        DetectorVerifier.verify(d, true);
        d.setOverFieldName(null);

        // some functions require a fieldname
        for (String f : new String[]{Detector.METRIC, Detector.MEAN, Detector.HIGH_MEAN,
                Detector.LOW_MEAN, Detector.AVG, Detector.HIGH_AVG, Detector.LOW_AVG,
                Detector.MEDIAN, Detector.MAX, Detector.MIN, Detector.SUM, Detector.LOW_SUM,
                Detector.HIGH_SUM, Detector.NON_NULL_SUM, Detector.LOW_NON_NULL_SUM,
                Detector.HIGH_NON_NULL_SUM, Detector.POPULATION_VARIANCE,
                Detector.LOW_POPULATION_VARIANCE, Detector.HIGH_POPULATION_VARIANCE,
                Detector.DISTINCT_COUNT, Detector.DC,
                Detector.HIGH_DISTINCT_COUNT, Detector.HIGH_DC, Detector.LOW_DISTINCT_COUNT,
                Detector.LOW_DC, Detector.INFO_CONTENT, Detector.LOW_INFO_CONTENT,
                Detector.HIGH_INFO_CONTENT, Detector.LAT_LONG}) {
            d = new Detector(f, "f");
            d.setByFieldName("b");
            DetectorVerifier.verify(d, false);
            try {
                DetectorVerifier.verify(d, true);
                Assert.assertFalse(Detector.METRIC.equals(f));
            } catch (ElasticsearchParseException e) {
                // "metric" is not allowed as the function for pre-summarised input
                Assert.assertEquals(Detector.METRIC, f);
            }
        }


        // these functions don't work with fieldname
        for (String f : new String[]{Detector.COUNT, Detector.HIGH_COUNT,
                Detector.LOW_COUNT, Detector.NON_ZERO_COUNT, Detector.NZC,
                Detector.RARE, Detector.FREQ_RARE, Detector.TIME_OF_DAY,
                Detector.TIME_OF_WEEK}) {
            try {
                d = new Detector(f, "field");
                d.setByFieldName("b");
                DetectorVerifier.verify(d, false);
                Assert.fail("ElasticsearchParseException not thrown when expected");
            } catch (ElasticsearchParseException e) {
            }
            try {
                d = new Detector(f, "field");
                d.setByFieldName("b");
                DetectorVerifier.verify(d, true);
                Assert.fail("ElasticsearchParseException not thrown when expected");
            } catch (ElasticsearchParseException e) {
            }
        }


        // these functions cant have an over field
        d.setOverFieldName(null);
        for (String f : new String[]{Detector.HIGH_COUNT,
                Detector.LOW_COUNT, Detector.NON_ZERO_COUNT, Detector.NZC,
                Detector.RARE, Detector.FREQ_RARE, Detector.TIME_OF_DAY,
                Detector.TIME_OF_WEEK}) {
            try {
                d = new Detector(f, "field");
                d.setByFieldName("b");
                DetectorVerifier.verify(d, false);
                Assert.fail("ElasticsearchParseException not thrown when expected");
            } catch (ElasticsearchParseException e) {
            }
            try {
                d = new Detector(f, "field");
                d.setByFieldName("b");
                DetectorVerifier.verify(d, true);
                Assert.fail("ElasticsearchParseException not thrown when expected");
            } catch (ElasticsearchParseException e) {
            }
        }

        d = new Detector(Detector.FREQ_RARE, "field");
        d.setByFieldName("b");
        d.setOverFieldName("over");
        try {
            DetectorVerifier.verify(d, false);
            Assert.fail("ElasticsearchParseException not thrown when expected");
        } catch (ElasticsearchParseException e) {
        }
        try {
            DetectorVerifier.verify(d, true);
            Assert.fail("ElasticsearchParseException not thrown when expected");
        } catch (ElasticsearchParseException e) {
        }


        for (String f : new String[]{Detector.HIGH_COUNT,
                Detector.LOW_COUNT, Detector.NON_ZERO_COUNT, Detector.NZC}) {
            d = new Detector(f);
            d.setByFieldName("by");
            DetectorVerifier.verify(d, false);
            DetectorVerifier.verify(d, true);
        }

        for (String f : new String[]{Detector.COUNT, Detector.HIGH_COUNT,
                Detector.LOW_COUNT}) {
            d = new Detector(f);
            d.setOverFieldName("over");
            DetectorVerifier.verify(d, false);
            DetectorVerifier.verify(d, true);
        }

        for (String f : new String[]{Detector.HIGH_COUNT,
                Detector.LOW_COUNT}) {
            d = new Detector(f);
            d.setByFieldName("by");
            d.setOverFieldName("over");
            DetectorVerifier.verify(d, false);
            DetectorVerifier.verify(d, true);
        }

        for (String f : new String[]{Detector.NON_ZERO_COUNT, Detector.NZC}) {
            try {
                d = new Detector(f, "field");
                d.setByFieldName("by");
                d.setOverFieldName("over");
                DetectorVerifier.verify(d, false);
                Assert.fail("ElasticsearchParseException not thrown when expected");
            } catch (ElasticsearchParseException e) {
            }
            try {
                d = new Detector(f, "field");
                d.setByFieldName("by");
                d.setOverFieldName("over");
                DetectorVerifier.verify(d, true);
                Assert.fail("ElasticsearchParseException not thrown when expected");
            } catch (ElasticsearchParseException e) {
            }
        }
    }


    public void testVerifyExcludeFrequent_GivenNotSet() throws ElasticsearchParseException {
        assertTrue(DetectorVerifier.verifyExcludeFrequent(null));
        assertTrue(DetectorVerifier.verifyExcludeFrequent(""));
    }


    public void testVerifyExcludeFrequent_GivenValidWord() throws ElasticsearchParseException {
        assertTrue(DetectorVerifier.verifyExcludeFrequent("true"));
        assertTrue(DetectorVerifier.verifyExcludeFrequent("false"));
        assertTrue(DetectorVerifier.verifyExcludeFrequent("by"));
        assertTrue(DetectorVerifier.verifyExcludeFrequent("over"));
    }

    public void testVerifyExcludeFrequent_GivenInvalidWord() {
        ESTestCase.expectThrows(ElasticsearchParseException.class, () -> DetectorVerifier.verifyExcludeFrequent("bananas"));
    }


    public void testVerifyExcludeFrequent_GivenNumber() throws ElasticsearchParseException {
        assertTrue(DetectorVerifier.verifyExcludeFrequent("0"));
        assertTrue(DetectorVerifier.verifyExcludeFrequent("1"));
        assertTrue(DetectorVerifier.verifyExcludeFrequent("-1"));
    }


    public void testVerify_GivenInvalidDetectionRuleConditionFieldName() {
        Detector detector = new Detector("mean", "metricVale");
        detector.setByFieldName("metricName");
        RuleCondition ruleCondition = new RuleCondition(RuleConditionType.NUMERICAL_ACTUAL);
        ruleCondition.setFieldName("metricValue");
        ruleCondition.setCondition(new Condition(Operator.LT, "5"));
        DetectionRule rule = new DetectionRule();
        rule.setRuleConditions(Arrays.asList(ruleCondition));
        detector.setDetectorRules(Arrays.asList(rule));

        ElasticsearchParseException e = ESTestCase.expectThrows(ElasticsearchParseException.class,
                () -> DetectorVerifier.verify(detector, false));

        assertEquals(1, e.getHeader("errorCode").size());
        assertEquals(ErrorCodes.DETECTOR_RULE_CONDITION_INVALID_FIELD_NAME.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_DETECTION_RULE_CONDITION_INVALID_FIELD_NAME,
                "[metricName]", "metricValue"),
                e.getMessage());
    }


    public void testVerify_GivenInvalidDetectionRuleTargetFieldName()
            throws ElasticsearchParseException {
        Detector detector = new Detector("mean", "metricVale");
        detector.setByFieldName("metricName");
        detector.setPartitionFieldName("instance");
        RuleCondition ruleCondition = new RuleCondition(RuleConditionType.NUMERICAL_ACTUAL);
        ruleCondition.setFieldName("metricName");
        ruleCondition.setCondition(new Condition(Operator.LT, "5"));
        DetectionRule rule = new DetectionRule();
        rule.setTargetFieldName("instancE");
        rule.setRuleConditions(Arrays.asList(ruleCondition));
        detector.setDetectorRules(Arrays.asList(rule));

        ElasticsearchParseException e = ESTestCase.expectThrows(ElasticsearchParseException.class,
                () -> DetectorVerifier.verify(detector, false));

        assertEquals(1, e.getHeader("errorCode").size());
        assertEquals(ErrorCodes.DETECTOR_RULE_INVALID_TARGET_FIELD.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_DETECTION_RULE_INVALID_TARGET_FIELD_NAME,
                "[metricName, instance]", "instancE"),
                e.getMessage());
    }


    public void testVerify_GivenDetectionRuleWithInvalidCondition() {
        Detector detector = new Detector("mean", "metricVale");
        detector.setByFieldName("metricName");
        detector.setPartitionFieldName("instance");
        RuleCondition ruleCondition = new RuleCondition(RuleConditionType.NUMERICAL_ACTUAL);
        ruleCondition.setFieldName("metricName");
        ruleCondition.setFieldValue("CPU");
        ruleCondition.setCondition(new Condition(Operator.LT, "invalid"));
        DetectionRule rule = new DetectionRule();
        rule.setTargetFieldName("instance");
        rule.setRuleConditions(Arrays.asList(ruleCondition));
        detector.setDetectorRules(Arrays.asList(rule));

        ElasticsearchParseException e = ESTestCase.expectThrows(ElasticsearchParseException.class,
                () -> DetectorVerifier.verify(detector, false));

        assertEquals(1, e.getHeader("errorCode").size());
        assertEquals(ErrorCodes.CONDITION_INVALID_ARGUMENT.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_CONDITION_INVALID_VALUE_NUMBER, "invalid"), e.getMessage());
    }


    public void testVerify_GivenValidDetectionRule() throws ElasticsearchParseException {
        Detector detector = new Detector("mean", "metricVale");
        detector.setByFieldName("metricName");
        detector.setPartitionFieldName("instance");
        RuleCondition ruleCondition = new RuleCondition(RuleConditionType.NUMERICAL_ACTUAL);
        ruleCondition.setFieldName("metricName");
        ruleCondition.setFieldValue("CPU");
        ruleCondition.setCondition(new Condition(Operator.LT, "5"));
        DetectionRule rule = new DetectionRule();
        rule.setTargetFieldName("instance");
        rule.setRuleConditions(Arrays.asList(ruleCondition));
        detector.setDetectorRules(Arrays.asList(rule));

        DetectorVerifier.verify(detector, false);
    }
}
