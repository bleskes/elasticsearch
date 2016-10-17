
package org.elasticsearch.xpack.prelert.job.detectionrules.verification;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.xpack.prelert.job.condition.Condition;
import org.elasticsearch.xpack.prelert.job.condition.Operator;
import org.elasticsearch.xpack.prelert.job.condition.verification.ConditionVerifier;
import org.elasticsearch.xpack.prelert.job.detectionrules.RuleCondition;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;

import java.util.EnumSet;

public final class RuleConditionVerifier {
    private static EnumSet<Operator> VALID_CONDITION_OPERATORS = EnumSet.of(Operator.LT, Operator.LTE, Operator.GT, Operator.GTE);

    private RuleConditionVerifier() {
        // Hide default constructor
    }

    public static boolean verify(RuleCondition ruleCondition) throws ElasticsearchParseException {
        verifyFieldsBoundToType(ruleCondition);
        verifyFieldValueRequiresFieldName(ruleCondition);
        return true;
    }

    private static void verifyFieldsBoundToType(RuleCondition ruleCondition) throws ElasticsearchParseException {
        switch (ruleCondition.getConditionType()) {
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

    private static void verifyCategorical(RuleCondition ruleCondition) throws ElasticsearchParseException {
        checkCategoricalHasNoField(Condition.CONDITION_FIELD.getPreferredName(), ruleCondition.getCondition());
        checkCategoricalHasNoField(RuleCondition.FIELD_VALUE_FIELD.getPreferredName(), ruleCondition.getFieldValue());
        checkCategoricalHasField(RuleCondition.VALUE_LIST_FIELD.getPreferredName(), ruleCondition.getValueList());
    }

    private static void checkCategoricalHasNoField(String fieldName, Object fieldValue) throws ElasticsearchParseException {
        if (fieldValue != null) {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_DETECTION_RULE_CONDITION_CATEGORICAL_INVALID_OPTION, fieldName);
            throw ExceptionsHelper.parseException(msg, ErrorCodes.DETECTOR_RULE_CONDITION_INVALID_OPTION);
        }
    }

    private static void checkCategoricalHasField(String fieldName, Object fieldValue) throws ElasticsearchParseException {
        if (fieldValue == null) {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_DETECTION_RULE_CONDITION_CATEGORICAL_MISSING_OPTION, fieldName);
            throw ExceptionsHelper.parseException(msg, ErrorCodes.DETECTOR_RULE_CONDITION_MISSING_FIELD);
        }
    }

    private static void verifyNumerical(RuleCondition ruleCondition) throws ElasticsearchParseException {
        checkNumericalHasNoField(RuleCondition.VALUE_LIST_FIELD.getPreferredName(), ruleCondition.getValueList());
        checkNumericalHasField(Condition.CONDITION_FIELD.getPreferredName(), ruleCondition.getCondition());
        if (ruleCondition.getFieldName() != null && ruleCondition.getFieldValue() == null) {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_DETECTION_RULE_CONDITION_NUMERICAL_WITH_FIELD_NAME_REQUIRES_FIELD_VALUE);
            throw ExceptionsHelper.parseException(msg, ErrorCodes.DETECTOR_RULE_CONDITION_MISSING_FIELD);
        }
        checkNumericalConditionOparatorsAreValid(ruleCondition);
        ConditionVerifier.verify(ruleCondition.getCondition());
    }

    private static void checkNumericalHasNoField(String fieldName, Object fieldValue) throws ElasticsearchParseException {
        if (fieldValue != null) {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_DETECTION_RULE_CONDITION_NUMERICAL_INVALID_OPTION, fieldName);
            throw ExceptionsHelper.parseException(msg, ErrorCodes.DETECTOR_RULE_CONDITION_INVALID_OPTION);
        }
    }

    private static void checkNumericalHasField(String fieldName, Object fieldValue) throws ElasticsearchParseException {
        if (fieldValue == null) {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_DETECTION_RULE_CONDITION_NUMERICAL_MISSING_OPTION, fieldName);
            throw ExceptionsHelper.parseException(msg, ErrorCodes.DETECTOR_RULE_CONDITION_MISSING_FIELD);
        }
    }

    private static void verifyFieldValueRequiresFieldName(RuleCondition ruleCondition) throws ElasticsearchParseException {
        if (ruleCondition.getFieldValue() != null && ruleCondition.getFieldName() == null) {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_DETECTION_RULE_CONDITION_MISSING_FIELD_NAME,
                    ruleCondition.getFieldValue());
            throw ExceptionsHelper.parseException(msg, ErrorCodes.DETECTOR_RULE_CONDITION_MISSING_FIELD);
        }
    }

    private static void checkNumericalConditionOparatorsAreValid(RuleCondition ruleCondition) throws ElasticsearchParseException {
        Operator operator = ruleCondition.getCondition().getOperator();
        if (!VALID_CONDITION_OPERATORS.contains(operator)) {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_DETECTION_RULE_CONDITION_NUMERICAL_INVALID_OPERATOR, operator);
            throw ExceptionsHelper.parseException(msg, ErrorCodes.CONDITION_INVALID_ARGUMENT);
        }
    }
}
