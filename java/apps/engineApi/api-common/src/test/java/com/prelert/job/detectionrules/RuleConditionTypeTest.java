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

import org.junit.Test;

import com.prelert.job.detectionrules.RuleConditionType;

public class RuleConditionTypeTest
{
    @Test
    public void testForString()
    {
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
