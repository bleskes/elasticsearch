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

import com.prelert.job.Detector;
import com.prelert.job.detectionrules.DetectionRule;
import com.prelert.job.detectionrules.RuleCondition;
import com.prelert.job.detectionrules.RuleConditionType;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobConfigurationException;
import com.prelert.job.messages.Messages;

public class DetectionRuleVerifier
{
    public static boolean verify(DetectionRule rule, Detector detector)
            throws JobConfigurationException
    {
        checkScoping(rule, detector);
        checkTargetNameIsSetWhenTargetFieldIsSet(rule);
        checkAtLeastOneRuleCondition(rule);
        verifyRuleConditions(rule);
        return true;
    }

    private static void checkScoping(DetectionRule rule, Detector detector)
            throws JobConfigurationException
    {
        List<String> analysisFields = detector.extractAnalysisFields();
        String targetField = rule.getTargetField();
        checkTargetFieldIsAnalysisField(analysisFields, targetField);
        for (RuleCondition condition : rule.getRuleConditions())
        {
            String fieldName = condition.getFieldName();
            checkFieldNameIsAnalysisField(analysisFields, fieldName);
            checkFieldNameIsSetWhenRequired(analysisFields, targetField, fieldName);
            checkHierarchy(detector, targetField, fieldName);
        }
    }

    private static void checkTargetFieldIsAnalysisField(List<String> analysisFields,
            String targetField) throws JobConfigurationException
    {
        if (targetField != null && !analysisFields.contains(targetField))
        {
            String msg = Messages.getMessage(
                    Messages.JOB_CONFIG_DETECTION_RULE_INVALID_TARGET_FIELD,
                    analysisFields, targetField);
            throw new JobConfigurationException(msg, ErrorCodes.DETECTOR_RULE_INVALID_TARGET_FIELD);
        }
    }

    private static void checkFieldNameIsAnalysisField(List<String> analysisFields, String fieldName)
            throws JobConfigurationException
    {
        if (fieldName != null && !analysisFields.contains(fieldName))
        {
            String msg = Messages.getMessage(
                    Messages.JOB_CONFIG_DETECTION_RULE_CONDITION_INVALID_FIELD_NAME,
                    analysisFields, fieldName);
            throw new JobConfigurationException(msg, ErrorCodes.DETECTOR_RULE_CONDITION_INVALID_FIELD_NAME);
        }
    }

    private static void checkFieldNameIsSetWhenRequired(List<String> analysisFields,
            String targetField, String fieldName) throws JobConfigurationException
    {
        if (targetField != null && analysisFields.size() > 1 && fieldName == null)
        {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_DETECTION_RULE_REQUIRES_CONDITION_FIELDS);
            throw new JobConfigurationException(msg, ErrorCodes.DETECTOR_RULE_REQUIRES_ONE_OR_MORE_CONDITIONS);
        }
    }

    private static void checkHierarchy(Detector detector, String targetField, String fieldName)
            throws JobConfigurationException
    {
        if (targetField != null && fieldName != null
                && !isTargetFieldHigherScopeThanFieldName(detector, targetField, fieldName))
        {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_DETECTION_RULE_HIERARCHY_VIOLATION,
                    fieldName, targetField);
            throw new JobConfigurationException(msg, ErrorCodes.DETECTOR_RULE_HIERARCHY_VIOLATION);
        }
    }

    private static boolean isTargetFieldHigherScopeThanFieldName(Detector detector,
            String targetField, String fieldName)
    {
        ScopingLevel targetFieldLevel = ScopingLevel.from(detector, targetField);
        ScopingLevel fieldNameLevel = ScopingLevel.from(detector, fieldName);
        return targetFieldLevel.isHigherThan(fieldNameLevel);
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
            checkNoTargetFieldWhenCategoricalConditionsExist(rule, condition);
            RuleConditionVerifier.verify(condition);
        }
    }

    private static void checkNoTargetFieldWhenCategoricalConditionsExist(DetectionRule rule,
            RuleCondition condition) throws JobConfigurationException
    {
        if (condition.getConditionType() == RuleConditionType.CATEGORICAL
                && rule.getTargetField() != null)
        {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_DETECTION_RULE_CONDITION_CATEGORICAL_TARGET_FIELD_NOT_SUPPORTED);
            throw new JobConfigurationException(msg, ErrorCodes.DETECTOR_RULE_CONDITION_INVALID_OPTION);
        }
    }
}
