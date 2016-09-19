
package org.elasticsearch.prelert.job.detectionrules;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@JsonInclude(Include.NON_NULL)
public class DetectionRule
{
    public static final String RULE_ACTION = "ruleAction";
    public static final String TARGET_FIELD_NAME = "targetFieldName";
    public static final String TARGET_FIELD_VALUE = "targetFieldValue";
    public static final String CONDITIONS_CONNECTIVE = "conditionsConnective";
    public static final String RULE_CONDITIONS = "ruleConditions";

    private RuleAction ruleAction;
    private String targetFieldName;
    private String targetFieldValue;
    private Connective conditionsConnective;
    private List<RuleCondition> ruleConditions;

    public DetectionRule()
    {
        ruleAction = RuleAction.FILTER_RESULTS;
        conditionsConnective = Connective.OR;
        ruleConditions = new ArrayList<>();
    }

    public void setRuleAction(RuleAction ruleAction)
    {
        this.ruleAction = ruleAction;
    }

    public RuleAction getRuleAction()
    {
        return ruleAction;
    }

    public void setTargetFieldName(String targetFieldName)
    {
        this.targetFieldName = targetFieldName;
    }

    public String getTargetFieldName()
    {
        return targetFieldName;
    }

    public void setTargetFieldValue(String targetFieldValue)
    {
        this.targetFieldValue = targetFieldValue;
    }

    public String getTargetFieldValue()
    {
        return targetFieldValue;
    }

    public void setConditionsConnective(Connective conditionsConnective)
    {
        this.conditionsConnective = conditionsConnective;
    }

    public Connective getConditionsConnective()
    {
        return conditionsConnective;
    }

    public void setRuleConditions(List<RuleCondition> ruleConditions)
    {
        this.ruleConditions = ruleConditions;
    }

    public List<RuleCondition> getRuleConditions()
    {
        return ruleConditions;
    }

    public Set<String> extractReferencedLists()
    {
        return ruleConditions.stream().map(RuleCondition::getValueList).filter(Objects::nonNull)
                .collect(Collectors.toSet());
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
        return Objects.equals(ruleAction, other.ruleAction)
                && Objects.equals(targetFieldName, other.targetFieldName)
                && Objects.equals(targetFieldValue, other.targetFieldValue)
                && Objects.equals(conditionsConnective, other.conditionsConnective)
                && Objects.equals(ruleConditions, other.ruleConditions);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(ruleAction, targetFieldName, targetFieldValue,
                conditionsConnective, ruleConditions);
    }
}
