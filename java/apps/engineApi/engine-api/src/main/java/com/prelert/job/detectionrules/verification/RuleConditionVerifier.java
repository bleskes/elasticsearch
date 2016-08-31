/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2016     *
 *                                                          *
 *----------------------------------------------------------*
 *----------------------------------------------------------*
 * WARNING:                                                 *
 * THIS FILE CONTAINS UNPUBLISHED PROPRIETARY               *
 * SOURCE CODE WHICH IS THE PROPERTY OF PRELERT LTD AND     *
 * PARENT OR SUBSIDIARY COMPANIES.                          *
 * PLEASE READ THE FOLLOWING AND TAKE CAREFUL NOTE:         *
 *                                                          *
 * This source code is confidential and any person who      *
 * receives a copy of it, or believes that they are viewing *
 * it without permission is asked to notify Prelert Ltd     *
 * on +44 (0)20 3567 1249 or email to legal@prelert.com.    *
 * All intellectual property rights in this source code     *
 * are owned by Prelert Ltd.  No part of this source code   *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ************************************************************/

package com.prelert.job.detectionrules.verification;

import java.util.EnumSet;

import com.prelert.job.condition.Operator;
import com.prelert.job.condition.verification.ConditionVerifier;
import com.prelert.job.detectionrules.RuleCondition;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobConfigurationException;
import com.prelert.job.messages.Messages;

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
