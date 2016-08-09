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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class DetectionRule
{
    public static final String RULE_ACTION = "ruleAction";
    public static final String TARGET_FIELD_NAME = "targetFieldName";
    public static final String TARGET_FIELD_VALUE = "targetFieldValue";
    public static final String CONDITIONS_CONNECTIVE = "conditionsConnective";
    public static final String RULE_CONDITIONS = "ruleConditions";

    private RuleAction m_RuleAction;
    private String m_TargetFieldName;
    private String m_TargetFieldValue;
    private Connective m_ConditionsConnective;
    private List<RuleCondition> m_RuleConditions;

    public DetectionRule()
    {
        m_RuleAction = RuleAction.FILTER_RESULTS;
        m_ConditionsConnective = Connective.OR;
        m_RuleConditions = new ArrayList<>();
    }

    public void setRuleAction(RuleAction ruleAction)
    {
        m_RuleAction = ruleAction;
    }

    public RuleAction getRuleAction()
    {
        return m_RuleAction;
    }

    public void setTargetFieldName(String targetFieldName)
    {
        m_TargetFieldName = targetFieldName;
    }

    public String getTargetFieldName()
    {
        return m_TargetFieldName;
    }

    public void setTargetFieldValue(String targetFieldValue)
    {
        m_TargetFieldValue = targetFieldValue;
    }

    public String getTargetFieldValue()
    {
        return m_TargetFieldValue;
    }

    public void setConditionsConnective(Connective conditionsConnective)
    {
        m_ConditionsConnective = conditionsConnective;
    }

    public Connective getConditionsConnective()
    {
        return m_ConditionsConnective;
    }

    public void setRuleConditions(List<RuleCondition> ruleConditions)
    {
        m_RuleConditions = ruleConditions;
    }

    public List<RuleCondition> getRuleConditions()
    {
        return m_RuleConditions;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (obj instanceof DetectionRule == false)
        {
            return false;
        }

        DetectionRule other = (DetectionRule) obj;
        return Objects.equals(m_RuleAction, other.m_RuleAction)
                && Objects.equals(m_TargetFieldName, other.m_TargetFieldName)
                && Objects.equals(m_TargetFieldValue, other.m_TargetFieldValue)
                && Objects.equals(m_ConditionsConnective, other.m_ConditionsConnective)
                && Objects.equals(m_RuleConditions, other.m_RuleConditions);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(m_RuleAction, m_TargetFieldName, m_TargetFieldValue,
                m_ConditionsConnective, m_RuleConditions);
    }
}
