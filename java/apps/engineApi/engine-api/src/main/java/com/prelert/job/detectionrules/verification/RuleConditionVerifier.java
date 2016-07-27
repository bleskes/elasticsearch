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

import com.prelert.job.condition.verification.ConditionVerifier;
import com.prelert.job.detectionrules.RuleCondition;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobConfigurationException;
import com.prelert.job.messages.Messages;

public class RuleConditionVerifier
{
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

    private static void verifyCategorical(RuleCondition ruleCondition) throws JobConfigurationException
    {
        if (ruleCondition.getCondition() != null)
        {
            String msg = Messages.getMessage(
                    Messages.JOB_CONFIG_DETECTION_RULE_CONDITION_CATEGORICAL_CONDITION_NOT_SUPPORTED);
            throw new JobConfigurationException(msg, ErrorCodes.DETECTOR_RULE_CONDITION_INVALID_OPTION);
        }
        if (ruleCondition.getValueList() == null)
        {
            String msg = Messages.getMessage(
                    Messages.JOB_CONFIG_DETECTION_RULE_CONDITION_CATEGORICAL_REQUIRES_VALUE_LIST);
            throw new JobConfigurationException(msg, ErrorCodes.DETECTOR_RULE_CONDITION_MISSING_FIELD);
        }
    }

    private static void verifyNumerical(RuleCondition ruleCondition) throws JobConfigurationException
    {
        if (ruleCondition.getValueList() != null)
        {
            String msg = Messages.getMessage(
                    Messages.JOB_CONFIG_DETECTION_RULE_CONDITION_NUMERICAL_VALUE_LIST_NOT_SUPPORTED);
            throw new JobConfigurationException(msg, ErrorCodes.DETECTOR_RULE_CONDITION_INVALID_OPTION);
        }
        if (ruleCondition.getCondition() == null)
        {
            String msg = Messages.getMessage(
                    Messages.JOB_CONFIG_DETECTION_RULE_CONDITION_NUMERICAL_REQUIRES_CONDITION);
            throw new JobConfigurationException(msg, ErrorCodes.DETECTOR_RULE_CONDITION_MISSING_FIELD);
        }
        ConditionVerifier.verify(ruleCondition.getCondition());
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
}
