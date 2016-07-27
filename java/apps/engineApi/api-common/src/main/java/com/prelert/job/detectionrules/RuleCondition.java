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

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.prelert.job.condition.Condition;

@JsonInclude(Include.NON_NULL)
public class RuleCondition
{
    public static final String CONDITION_TYPE = "conditionType";
    public static final String FIELD_NAME = "fieldName";
    public static final String FIELD_VALUE = "fieldValue";
    public static final String CONDITION = "condition";
    public static final String VALUE_LIST = "valueList";

    private RuleConditionType m_ConditionType;

    /**
     * The field name for which the rule applies.
     * Can be null, meaning rule applies to all results.
     */
    private String m_FieldName;

    /**
     * The value of the field name for which the rule applies.
     * When set, the rule applies only to the results that have
     * the fieldName/fieldValue pair. When null, the rule applies
     * to all values for of the specified field name. Only applicable
     * when fieldName is not null.
     */
    private String m_FieldValue;

    private Condition m_Condition;

    /**
     * The unique identifier of a list.
     * Required when the rule type is categorical.
     * Should be null for all other types.
     */
    private String m_ValueList;

    public void setConditionType(RuleConditionType conditionType)
    {
        m_ConditionType = conditionType;
    }

    public RuleConditionType getConditionType()
    {
        return m_ConditionType;
    }

    public void setFieldName(String fieldName)
    {
        m_FieldName = fieldName;
    }

    public String getFieldName()
    {
        return m_FieldName;
    }

    public void setFieldValue(String fieldValue)
    {
        m_FieldValue = fieldValue;
    }

    public String getFieldValue()
    {
        return m_FieldValue;
    }

    public void setCondition(Condition condition)
    {
        m_Condition = condition;
    }

    public Condition getCondition()
    {
        return m_Condition;
    }

    public void setValueList(String listId)
    {
        m_ValueList = listId;
    }

    public String getValueList()
    {
        return m_ValueList;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (obj instanceof RuleCondition == false)
        {
            return false;
        }

        RuleCondition other = (RuleCondition) obj;
        return Objects.equals(m_ConditionType, other.m_ConditionType)
                && Objects.equals(m_FieldName, other.m_FieldName)
                && Objects.equals(m_FieldValue, other.m_FieldValue)
                && Objects.equals(m_Condition, other.m_Condition)
                && Objects.equals(m_ValueList, other.m_ValueList);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(m_ConditionType, m_FieldName, m_FieldValue, m_Condition, m_ValueList);
    }
}
