
package org.elasticsearch.xpack.prelert.job.detectionrules.verification;

import org.elasticsearch.xpack.prelert.job.condition.Operator;
import org.elasticsearch.xpack.prelert.job.condition.verification.ConditionVerifier;
import org.elasticsearch.xpack.prelert.job.detectionrules.RuleCondition;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobConfigurationException;
import org.elasticsearch.xpack.prelert.job.messages.Messages;

import java.util.EnumSet;

public final class RuleConditionVerifier
{
    private static EnumSet<Operator> VALID_CONDITION_OPERATORS = EnumSet.of(Operator.LT,
            Operator.LTE, Operator.GT, Operator.GTE);

    private RuleConditionVerifier()
    {
        // Hide default constructor
    }

    public static boolean verify(RuleCondition ruleCondition) throws JobConfigurationException
    {
        verifyFieldsBoundToType(ruleCondition);
        verifyFieldValueRequiresFieldName(ruleCondition);
        return true;
    }

    private static void verifyFieldsBoundToType(RuleCondition ruleCondition) throws JobConfigurationException
    {
        switch (ruleCondition.getConditionType())
        {
            case CATEGORICAL:
                verifyCategorical(ruleCondition);
                break;
            case NUMERICAL_ACTUAL:
            case NUMERICAL_TYPICAL:
            case NUMERICAL_DIFF_ABS:
                verifyNumerical(ruleCondition);
                break;
            default:
                throw new IllegalStateException();
        }
    }

    private static void verifyCategorical(RuleCondition ruleCondition)
            throws JobConfigurationException
    {
        checkCategoricalHasNoField(RuleCondition.CONDITION, ruleCondition.getCondition());
        checkCategoricalHasNoField(RuleCondition.FIELD_VALUE, ruleCondition.getFieldValue());
        checkCategoricalHasField(RuleCondition.VALUE_LIST, ruleCondition.getValueList());
    }

    private static void checkCategoricalHasNoField(String fieldName, Object fieldValue)
            throws JobConfigurationException
    {
        if (fieldValue != null)
        {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_DETECTION_RULE_CONDITION_CATEGORICAL_INVALID_OPTION, fieldName);
            throw new JobConfigurationException(msg, ErrorCodes.DETECTOR_RULE_CONDITION_INVALID_OPTION);
        }
    }

    private static void checkCategoricalHasField(String fieldName, Object fieldValue) throws JobConfigurationException
    {
        if (fieldValue == null)
        {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_DETECTION_RULE_CONDITION_CATEGORICAL_MISSING_OPTION, fieldName);
            throw new JobConfigurationException(msg, ErrorCodes.DETECTOR_RULE_CONDITION_MISSING_FIELD);
        }
    }

    private static void verifyNumerical(RuleCondition ruleCondition) throws JobConfigurationException
    {
        checkNumericalHasNoField(RuleCondition.VALUE_LIST, ruleCondition.getValueList());
        checkNumericalHasField(RuleCondition.CONDITION, ruleCondition.getCondition());
        if (ruleCondition.getFieldName() != null && ruleCondition.getFieldValue() == null)
        {
            String msg = Messages.getMessage(
                    Messages.JOB_CONFIG_DETECTION_RULE_CONDITION_NUMERICAL_WITH_FIELD_NAME_REQUIRES_FIELD_VALUE);
            throw new JobConfigurationException(msg, ErrorCodes.DETECTOR_RULE_CONDITION_MISSING_FIELD);
        }
        checkNumericalConditionOparatorsAreValid(ruleCondition);
        ConditionVerifier.verify(ruleCondition.getCondition());
    }

    private static void checkNumericalHasNoField(String fieldName, Object fieldValue)
            throws JobConfigurationException
    {
        if (fieldValue != null)
        {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_DETECTION_RULE_CONDITION_NUMERICAL_INVALID_OPTION, fieldName);
            throw new JobConfigurationException(msg, ErrorCodes.DETECTOR_RULE_CONDITION_INVALID_OPTION);
        }
    }

    private static void checkNumericalHasField(String fieldName, Object fieldValue)
            throws JobConfigurationException
    {
        if (fieldValue == null)
        {
            String msg = Messages.getMessage(
                    Messages.JOB_CONFIG_DETECTION_RULE_CONDITION_NUMERICAL_MISSING_OPTION,
                    fieldName);
            throw new JobConfigurationException(msg, ErrorCodes.DETECTOR_RULE_CONDITION_MISSING_FIELD);
        }
    }

    private static void verifyFieldValueRequiresFieldName(RuleCondition ruleCondition)
            throws JobConfigurationException
    {
        if (ruleCondition.getFieldValue() != null && ruleCondition.getFieldName() == null)
        {
            String msg = Messages.getMessage(
                    Messages.JOB_CONFIG_DETECTION_RULE_CONDITION_MISSING_FIELD_NAME,
                    ruleCondition.getFieldValue());
            throw new JobConfigurationException(msg, ErrorCodes.DETECTOR_RULE_CONDITION_MISSING_FIELD);
        }
    }

    private static void checkNumericalConditionOparatorsAreValid(RuleCondition ruleCondition)
            throws JobConfigurationException
    {
        Operator operator = ruleCondition.getCondition().getOperator();
        if (!VALID_CONDITION_OPERATORS.contains(operator))
        {
            String msg = Messages.getMessage(
                    Messages.JOB_CONFIG_DETECTION_RULE_CONDITION_NUMERICAL_INVALID_OPERATOR, operator);
            throw new JobConfigurationException(msg, ErrorCodes.CONDITION_INVALID_ARGUMENT);
        }
    }
}
