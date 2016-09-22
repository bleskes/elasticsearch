
package org.elasticsearch.xpack.prelert.job.detectionrules;

import org.elasticsearch.xpack.prelert.integration.hack.ESTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RuleConditionTypeTest extends ESTestCase {

    public void testForString() {
        assertEquals(RuleConditionType.CATEGORICAL, RuleConditionType.forString("categorical"));
        assertEquals(RuleConditionType.CATEGORICAL, RuleConditionType.forString("CATEGORICAL"));
        assertEquals(RuleConditionType.NUMERICAL_ACTUAL, RuleConditionType.forString("numerical_actual"));
        assertEquals(RuleConditionType.NUMERICAL_ACTUAL, RuleConditionType.forString("NUMERICAL_ACTUAL"));
        assertEquals(RuleConditionType.NUMERICAL_TYPICAL, RuleConditionType.forString("numerical_typical"));
        assertEquals(RuleConditionType.NUMERICAL_TYPICAL, RuleConditionType.forString("NUMERICAL_TYPICAL"));
        assertEquals(RuleConditionType.NUMERICAL_DIFF_ABS, RuleConditionType.forString("numerical_diff_abs"));
        assertEquals(RuleConditionType.NUMERICAL_DIFF_ABS, RuleConditionType.forString("NUMERICAL_DIFF_ABS"));
    }
}
