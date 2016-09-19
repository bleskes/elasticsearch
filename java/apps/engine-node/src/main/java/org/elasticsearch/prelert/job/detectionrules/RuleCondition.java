
package org.elasticsearch.prelert.job.detectionrules;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.elasticsearch.prelert.job.condition.Condition;

import java.util.Objects;

@JsonInclude(Include.NON_NULL)
public class RuleCondition
{
    public static final String CONDITION_TYPE = "conditionType";
    public static final String FIELD_NAME = "fieldName";
    public static final String FIELD_VALUE = "fieldValue";
    public static final String CONDITION = "condition";
    public static final String VALUE_LIST = "valueList";

    private RuleConditionType conditionType;

    /**
     * The field name for which the rule applies.
     * Can be null, meaning rule applies to all results.
     */
    private String fieldName;

    /**
     * The value of the field name for which the rule applies.
     * When set, the rule applies only to the results that have
     * the fieldName/fieldValue pair. When null, the rule applies
     * to all values for of the specified field name. Only applicable
     * when fieldName is not null.
     */
    private String fieldValue;

    private Condition condition;

    /**
     * The unique identifier of a list.
     * Required when the rule type is categorical.
     * Should be null for all other types.
     */
    private String valueList;

    public void setConditionType(RuleConditionType conditionType)
    {
        this.conditionType = conditionType;
    }

    public RuleConditionType getConditionType()
    {
        return conditionType;
    }

    public void setFieldName(String fieldName)
    {
        this.fieldName = fieldName;
    }

    public String getFieldName()
    {
        return fieldName;
    }

    public void setFieldValue(String fieldValue)
    {
        this.fieldValue = fieldValue;
    }

    public String getFieldValue()
    {
        return fieldValue;
    }

    public void setCondition(Condition condition)
    {
        this.condition = condition;
    }

    public Condition getCondition()
    {
        return condition;
    }

    public void setValueList(String listId)
    {
        valueList = listId;
    }

    public String getValueList()
    {
        return valueList;
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
        return Objects.equals(conditionType, other.conditionType)
                && Objects.equals(fieldName, other.fieldName)
                && Objects.equals(fieldValue, other.fieldValue)
                && Objects.equals(condition, other.condition)
                && Objects.equals(valueList, other.valueList);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(conditionType, fieldName, fieldValue, condition, valueList);
    }

    public static RuleCondition createCategorical(String fieldName, String valueList)
    {
        RuleCondition condition = new RuleCondition();
        condition.setConditionType(RuleConditionType.CATEGORICAL);
        condition.setFieldName(fieldName);
        condition.setValueList(valueList);
        return condition;
    }
}
