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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.prelert.job.Detector;
import com.prelert.job.detectionrules.DetectionRule;
import com.prelert.job.detectionrules.RuleCondition;
import com.prelert.job.detectionrules.RuleConditionType;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobConfigurationException;
import com.prelert.job.messages.Messages;

public final class DetectionRuleVerifier
{
    /**
     * Functions that do not support rules:
     * <ul>
     *   <li> lat_long - because it is a multivariate feature
     *   <li> metric - because having the same conditions on min,max,mean is error-prone
     * </ul>
     */
    private static final Set<String> FUNCTIONS_WITHOUT_RULE_SUPPORT = new HashSet<>(
            Arrays.asList(Detector.LAT_LONG, Detector.METRIC));

    private DetectionRuleVerifier()
    {
        // Hide default constructor
    }

    public static boolean verify(DetectionRule rule, Detector detector)
            throws JobConfigurationException
    {
        checkFunctionSupportsRules(detector);
        checkScoping(rule, detector);
        checkTargetFieldNameIsSetWhenTargetFieldValueIsSet(rule);
        checkAtLeastOneRuleCondition(rule);
        verifyRuleConditions(rule);
        return true;
    }

    private static void checkFunctionSupportsRules(Detector detector)
            throws JobConfigurationException
    {
        // When function is null it defaults to metric
        String function = detector.getFunction() == null ? Detector.METRIC : detector.getFunction();

        if (FUNCTIONS_WITHOUT_RULE_SUPPORT.contains(function))
        {
            String msg = Messages.getMessage(
                    Messages.JOB_CONFIG_DETECTION_RULE_NOT_SUPPORTED_BY_FUNCTION, function);
            throw new JobConfigurationException(msg, ErrorCodes.DETECTOR_RULES_NOT_SUPPORTED_BY_FUNCTION);
        }
    }

    private static void checkScoping(DetectionRule rule, Detector detector)
            throws JobConfigurationException
    {
        String targetFieldName = rule.getTargetFieldName();
        checkTargetFieldNameIsValid(detector.extractAnalysisFields(), targetFieldName);
        List<String> validFieldNameOptions = getValidFieldNameOptions(rule, detector);
        for (RuleCondition condition : rule.getRuleConditions())
        {
            checkFieldNameIsValid(validFieldNameOptions, condition.getFieldName());
        }
    }

    private static List<String> getValidFieldNameOptions(DetectionRule rule, Detector detector)
    {
        List<String> result = new ArrayList<>();
        if (detector.getOverFieldName() != null)
        {
            result.add(detector.getByFieldName() == null ?
                    detector.getOverFieldName() : detector.getByFieldName());
        }
        else if (detector.getByFieldName() != null)
        {
            result.add(detector.getByFieldName());
        }

        if (rule.getTargetFieldName() != null)
        {
            ScopingLevel targetLevel = ScopingLevel.from(detector, rule.getTargetFieldName());
            result = result.stream()
                    .filter(field -> targetLevel.isHigherThan(ScopingLevel.from(detector, field)))
                    .collect(Collectors.toList());
        }

        if (isEmptyFieldNameAllowed(detector, rule))
        {
            result.add(null);
        }
        return result;
    }

    private static void checkFieldNameIsValid(List<String> validOptions, String fieldName)
            throws JobConfigurationException
    {
        if (!validOptions.contains(fieldName))
        {
            String msg = Messages.getMessage(
                    Messages.JOB_CONFIG_DETECTION_RULE_CONDITION_INVALID_FIELD_NAME, validOptions,
                    fieldName);
            throw new JobConfigurationException(msg, ErrorCodes.DETECTOR_RULE_CONDITION_INVALID_FIELD_NAME);
        }
    }

    private static boolean isEmptyFieldNameAllowed(Detector detector, DetectionRule rule)
    {
        List<String> analysisFields = detector.extractAnalysisFields();
        return analysisFields.isEmpty()
                || (rule.getTargetFieldName() != null && analysisFields.size() == 1);
    }

    private static void checkTargetFieldNameIsValid(List<String> analysisFields,
            String targetFieldName) throws JobConfigurationException
    {
        if (targetFieldName != null && !analysisFields.contains(targetFieldName))
        {
            String msg = Messages.getMessage(
                    Messages.JOB_CONFIG_DETECTION_RULE_INVALID_TARGET_FIELD_NAME,
                    analysisFields, targetFieldName);
            throw new JobConfigurationException(msg, ErrorCodes.DETECTOR_RULE_INVALID_TARGET_FIELD);
        }
    }

    private static void checkTargetFieldNameIsSetWhenTargetFieldValueIsSet(DetectionRule rule)
            throws JobConfigurationException
    {
        if (rule.getTargetFieldValue() != null && rule.getTargetFieldName() == null)
        {
            String msg = Messages.getMessage(
                    Messages.JOB_CONFIG_DETECTION_RULE_MISSING_TARGET_FIELD_NAME,
                    rule.getTargetFieldValue());
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
                && rule.getTargetFieldName() != null)
        {
            String msg = Messages.getMessage(
                    Messages.JOB_CONFIG_DETECTION_RULE_CONDITION_CATEGORICAL_INVALID_OPTION,
                    DetectionRule.TARGET_FIELD_NAME);
            throw new JobConfigurationException(msg, ErrorCodes.DETECTOR_RULE_CONDITION_INVALID_OPTION);
        }
    }
}
