/****************************************************************************
 *                                                                          *
 * Copyright 2016-2016 Prelert Ltd                                          *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 *    http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 *                                                                          *
 ***************************************************************************/

package com.prelert.job.detectionrules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class DetectionRuleTest
{
    @Test
    public void testDefaultConstructor()
    {
        DetectionRule rule = new DetectionRule();
        assertEquals(RuleAction.FILTER_RESULTS, rule.getRuleAction());
        assertEquals(Connective.OR, rule.getConditionsConnective());
        assertEquals(Collections.emptyList(), rule.getRuleConditions());
        assertNull(rule.getTargetFieldName());
        assertNull(rule.getTargetFieldValue());
    }

    @Test
    public void testEqualsGivenSameObject()
    {
        DetectionRule rule = new DetectionRule();
        assertTrue(rule.equals(rule));
    }

    @Test
    public void testEqualsGivenString()
    {
        assertFalse(new DetectionRule().equals("a string"));
    }

    @Test
    public void testEqualsGivenDifferentAction()
    {
        DetectionRule rule1 = createFullyPopulated();
        DetectionRule rule2 = createFullyPopulated();
        rule2.setRuleAction(null);
        assertFalse(rule1.equals(rule2));
        assertFalse(rule2.equals(rule1));
    }

    @Test
    public void testEqualsGivenDifferentTargetFieldName()
    {
        DetectionRule rule1 = createFullyPopulated();
        DetectionRule rule2 = createFullyPopulated();
        rule2.setTargetFieldName(rule2.getTargetFieldName() + "2");
        assertFalse(rule1.equals(rule2));
        assertFalse(rule2.equals(rule1));
    }

    @Test
    public void testEqualsGivenDifferentTargetFieldValue()
    {
        DetectionRule rule1 = createFullyPopulated();
        DetectionRule rule2 = createFullyPopulated();
        rule2.setTargetFieldValue(rule2.getTargetFieldValue() + "2");
        assertFalse(rule1.equals(rule2));
        assertFalse(rule2.equals(rule1));
    }

    @Test
    public void testEqualsGivenDifferentConjunction()
    {
        DetectionRule rule1 = createFullyPopulated();
        DetectionRule rule2 = createFullyPopulated();
        rule2.setConditionsConnective(Connective.OR);
        assertFalse(rule1.equals(rule2));
        assertFalse(rule2.equals(rule1));
    }

    @Test
    public void testEqualsGivenRules()
    {
        DetectionRule rule1 = createFullyPopulated();
        DetectionRule rule2 = createFullyPopulated();
        rule2.setRuleConditions(Collections.emptyList());
        assertFalse(rule1.equals(rule2));
        assertFalse(rule2.equals(rule1));
    }

    @Test
    public void testEqualsGivenEqual()
    {
        DetectionRule rule1 = createFullyPopulated();
        DetectionRule rule2 = createFullyPopulated();
        assertTrue(rule1.equals(rule2));
        assertTrue(rule2.equals(rule1));
        assertEquals(rule1.hashCode(), rule2.hashCode());
    }

    private static DetectionRule createFullyPopulated()
    {
        DetectionRule rule = new DetectionRule();
        rule.setRuleAction(RuleAction.FILTER_RESULTS);
        rule.setTargetFieldName("targetField");
        rule.setTargetFieldValue("targetValue");
        rule.setConditionsConnective(Connective.AND);
        rule.setRuleConditions(Arrays.asList(new RuleCondition()));
        return rule;
    }
}
