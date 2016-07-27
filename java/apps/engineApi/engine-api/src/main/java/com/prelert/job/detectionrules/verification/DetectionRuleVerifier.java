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

import java.util.List;

import com.prelert.job.detectionrules.DetectionRule;
import com.prelert.job.detectionrules.RuleCondition;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobConfigurationException;
import com.prelert.job.messages.Messages;

public class DetectionRuleVerifier
{
    public static void verify(DetectionRule rule) throws JobConfigurationException
    {
        checkTargetNameIsSetWhenTargetFieldIsSet(rule);
        checkAtLeastOneRuleCondition(rule);
        verifyRuleConditions(rule);
    }

    private static void checkTargetNameIsSetWhenTargetFieldIsSet(DetectionRule rule)
            throws JobConfigurationException
    {
        if (rule.getTargetValue() != null && rule.getTargetField() == null)
        {
            String msg = Messages.getMessage(
                    Messages.JOB_CONFIG_DETECTION_RULE_MISSING_TARGET_FIELD,
                    rule.getTargetValue());
            throw new JobConfigurationException(msg, ErrorCodes.DETECTOR_RULE_MISSING_FIELD);
        }
    }

    private static void checkAtLeastOneRuleCondition(DetectionRule rule)
            throws JobConfigurationException
    {
        List<RuleCondition> ruleConditions = rule.getRuleConditions();
        if (ruleConditions == null || ruleConditions.isEmpty())
        {
            String msg = Messages.getMessage(
                    Messages.JOB_CONFIG_DETECTION_RULE_REQUIRES_AT_LEAST_ONE_CONDITION);
            throw new JobConfigurationException(msg, ErrorCodes.DETECTOR_RULE_REQUIRES_ONE_OR_MORE_CONDITIONS);
        }
    }

    private static void verifyRuleConditions(DetectionRule rule) throws JobConfigurationException
    {
        for (RuleCondition condition : rule.getRuleConditions())
        {
            RuleConditionVerifier.verify(condition);
        }
    }
}
