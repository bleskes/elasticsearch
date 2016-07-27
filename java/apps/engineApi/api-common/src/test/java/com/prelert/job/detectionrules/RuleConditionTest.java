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

import org.junit.Test;

import com.prelert.job.condition.Condition;
import com.prelert.job.condition.Operator;
import com.prelert.job.detectionrules.RuleConditionType;
import com.prelert.job.detectionrules.RuleCondition;

public class RuleConditionTest
{
    @Test
    public void testDefaultConstructor()
    {
        RuleCondition condition = new RuleCondition();
        assertNull(condition.getConditionType());
        assertNull(condition.getFieldName());
        assertNull(condition.getFieldValue());
        assertNull(condition.getCondition());
    }

    @Test
    public void testEqualsGivenSameObject()
    {
        RuleCondition condition = new RuleCondition();
        assertTrue(condition.equals(condition));
    }

    @Test
    public void testEqualsGivenString()
    {
        assertFalse(new RuleCondition().equals("a string"));
    }

    @Test
    public void testEqualsGivenDifferentType()
    {
        RuleCondition condition1 = createFullyPopulated();
        RuleCondition condition2 = createFullyPopulated();
        condition2.setConditionType(RuleConditionType.CATEGORICAL);
        assertFalse(condition1.equals(condition2));
        assertFalse(condition2.equals(condition1));
    }

    @Test
    public void testEqualsGivenDifferentFieldName()
    {
        RuleCondition condition1 = createFullyPopulated();
        RuleCondition condition2 = createFullyPopulated();
        condition2.setFieldName(condition2.getFieldName() + "aaa");
        assertFalse(condition1.equals(condition2));
        assertFalse(condition2.equals(condition1));
    }

    @Test
    public void testEqualsGivenDifferentFieldValue()
    {
        RuleCondition condition1 = createFullyPopulated();
        RuleCondition condition2 = createFullyPopulated();
        condition2.setFieldValue(condition2.getFieldValue() + "aaa");
        assertFalse(condition1.equals(condition2));
        assertFalse(condition2.equals(condition1));
    }

    @Test
    public void testEqualsGivenDifferentCondition()
    {
        RuleCondition condition1 = createFullyPopulated();
        RuleCondition condition2 = createFullyPopulated();
        condition2.setCondition(new Condition(Operator.GT, "5"));
        assertFalse(condition1.equals(condition2));
        assertFalse(condition2.equals(condition1));
    }

    @Test
    public void testEqualsGivenDifferentValueList()
    {
        RuleCondition condition1 = createFullyPopulated();
        RuleCondition condition2 = createFullyPopulated();
        condition2.setValueList(condition2.getValueList() + "aaa");
        assertFalse(condition1.equals(condition2));
        assertFalse(condition2.equals(condition1));
    }

    @Test
    public void testEqualsGivenEqual()
    {
        RuleCondition condition1 = createFullyPopulated();
        RuleCondition condition2 = createFullyPopulated();
        assertTrue(condition1.equals(condition2));
        assertTrue(condition2.equals(condition1));
        assertEquals(condition1.hashCode(), condition2.hashCode());
    }

    private static RuleCondition createFullyPopulated()
    {
        RuleCondition condition = new RuleCondition();
        condition.setConditionType(RuleConditionType.NUMERICAL_ACTUAL);
        condition.setFieldName("metricName");
        condition.setFieldValue("cpu");
        condition.setCondition(new Condition(Operator.LT, "5"));
        condition.setValueList("myList");
        return condition;
    }
}
