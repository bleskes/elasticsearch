/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.prelert.job.detectionrules.verification;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.xpack.prelert.job.Detector;
import org.elasticsearch.xpack.prelert.job.detectionrules.DetectionRule;
import org.elasticsearch.xpack.prelert.job.detectionrules.RuleCondition;
import org.elasticsearch.xpack.prelert.job.detectionrules.RuleConditionType;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class DetectionRuleVerifier {
    /**
     * Functions that do not support rules:
     * <ul>
     * <li>lat_long - because it is a multivariate feature
     * <li>metric - because having the same conditions on min,max,mean is
     * error-prone
     * </ul>
     */
    private static final Set<String> FUNCTIONS_WITHOUT_RULE_SUPPORT = new HashSet<>(Arrays.asList(Detector.LAT_LONG, Detector.METRIC));

    private DetectionRuleVerifier() {
        // Hide default constructor
    }

    public static boolean verify(DetectionRule rule, Detector detector) throws ElasticsearchParseException {
        checkFunctionSupportsRules(detector);
        checkScoping(rule, detector);
        checkTargetFieldNameIsSetWhenTargetFieldValueIsSet(rule);
        checkAtLeastOneRuleCondition(rule);
        verifyRuleConditions(rule);
        return true;
    }

    private static void checkFunctionSupportsRules(Detector detector) throws ElasticsearchParseException {
        // When function is null it defaults to metric
        String function = detector.getFunction() == null ? Detector.METRIC : detector.getFunction();

        if (FUNCTIONS_WITHOUT_RULE_SUPPORT.contains(function)) {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_DETECTION_RULE_NOT_SUPPORTED_BY_FUNCTION, function);
            throw ExceptionsHelper.parseException(msg, ErrorCodes.DETECTOR_RULES_NOT_SUPPORTED_BY_FUNCTION);
        }
    }

    private static void checkScoping(DetectionRule rule, Detector detector) throws ElasticsearchParseException {
        String targetFieldName = rule.getTargetFieldName();
        checkTargetFieldNameIsValid(detector.extractAnalysisFields(), targetFieldName);
        List<String> validFieldNameOptions = getValidFieldNameOptions(rule, detector);
        for (RuleCondition condition : rule.getRuleConditions()) {
            checkFieldNameIsValid(validFieldNameOptions, condition.getFieldName());
        }
    }

    private static List<String> getValidFieldNameOptions(DetectionRule rule, Detector detector) {
        List<String> result = new ArrayList<>();
        if (detector.getOverFieldName() != null) {
            result.add(detector.getByFieldName() == null ? detector.getOverFieldName() : detector.getByFieldName());
        } else if (detector.getByFieldName() != null) {
            result.add(detector.getByFieldName());
        }

        if (rule.getTargetFieldName() != null) {
            ScopingLevel targetLevel = ScopingLevel.from(detector, rule.getTargetFieldName());
            result = result.stream().filter(field -> targetLevel.isHigherThan(ScopingLevel.from(detector, field)))
                    .collect(Collectors.toList());
        }

        if (isEmptyFieldNameAllowed(detector, rule)) {
            result.add(null);
        }
        return result;
    }

    private static void checkFieldNameIsValid(List<String> validOptions, String fieldName) throws ElasticsearchParseException {
        if (!validOptions.contains(fieldName)) {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_DETECTION_RULE_CONDITION_INVALID_FIELD_NAME, validOptions, fieldName);
            throw ExceptionsHelper.parseException(msg, ErrorCodes.DETECTOR_RULE_CONDITION_INVALID_FIELD_NAME);
        }
    }

    private static boolean isEmptyFieldNameAllowed(Detector detector, DetectionRule rule) {
        List<String> analysisFields = detector.extractAnalysisFields();
        return analysisFields.isEmpty() || (rule.getTargetFieldName() != null && analysisFields.size() == 1);
    }

    private static void checkTargetFieldNameIsValid(List<String> analysisFields, String targetFieldName)
            throws ElasticsearchParseException {
        if (targetFieldName != null && !analysisFields.contains(targetFieldName)) {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_DETECTION_RULE_INVALID_TARGET_FIELD_NAME, analysisFields, targetFieldName);
            throw ExceptionsHelper.parseException(msg, ErrorCodes.DETECTOR_RULE_INVALID_TARGET_FIELD);
        }
    }

    private static void checkTargetFieldNameIsSetWhenTargetFieldValueIsSet(DetectionRule rule) throws ElasticsearchParseException {
        if (rule.getTargetFieldValue() != null && rule.getTargetFieldName() == null) {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_DETECTION_RULE_MISSING_TARGET_FIELD_NAME, rule.getTargetFieldValue());
            throw ExceptionsHelper.parseException(msg, ErrorCodes.DETECTOR_RULE_MISSING_FIELD);
        }
    }

    private static void checkAtLeastOneRuleCondition(DetectionRule rule) throws ElasticsearchParseException {
        List<RuleCondition> ruleConditions = rule.getRuleConditions();
        if (ruleConditions == null || ruleConditions.isEmpty()) {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_DETECTION_RULE_REQUIRES_AT_LEAST_ONE_CONDITION);
            throw ExceptionsHelper.parseException(msg, ErrorCodes.DETECTOR_RULE_REQUIRES_ONE_OR_MORE_CONDITIONS);
        }
    }

    private static void verifyRuleConditions(DetectionRule rule) throws ElasticsearchParseException {
        for (RuleCondition condition : rule.getRuleConditions()) {
            checkNoTargetFieldWhenCategoricalConditionsExist(rule, condition);
        }
    }

    private static void checkNoTargetFieldWhenCategoricalConditionsExist(DetectionRule rule, RuleCondition condition)
            throws ElasticsearchParseException {
        if (condition.getConditionType() == RuleConditionType.CATEGORICAL && rule.getTargetFieldName() != null) {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_DETECTION_RULE_CONDITION_CATEGORICAL_INVALID_OPTION,
                    DetectionRule.TARGET_FIELD_NAME_FIELD.getPreferredName());
            throw ExceptionsHelper.parseException(msg, ErrorCodes.DETECTOR_RULE_CONDITION_INVALID_OPTION);
        }
    }
}
