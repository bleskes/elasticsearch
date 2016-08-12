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
 * are owned by Prelert Ltd. No part of this source code    *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ************************************************************/

package com.prelert.rs.job.update;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import com.prelert.job.Detector;
import com.prelert.job.JobDetails;
import com.prelert.job.JobException;
import com.prelert.job.config.DefaultDetectorDescription;
import com.prelert.job.detectionrules.DetectionRule;
import com.prelert.job.detectionrules.verification.DetectionRuleVerifier;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobConfigurationException;
import com.prelert.job.manager.JobManager;
import com.prelert.job.messages.Messages;
import com.prelert.rs.provider.JobConfigurationParseException;

class DetectorsUpdater extends AbstractUpdater
{
    private static final String DETECTOR_INDEX = "index";
    private static final String DESCRIPTION = "description";
    private static final String DETECTOR_RULES = "detectorRules";
    private static final Set<String> REQUIRED_PARAMS = Sets.newLinkedHashSet(
            Arrays.asList(DETECTOR_INDEX));
    private static final Set<String> OPTIONAL_PARAMS = Sets.newLinkedHashSet(
            Arrays.asList(DESCRIPTION, DETECTOR_RULES));

    private final StringWriter m_ConfigWriter;
    private List<UpdateParams> m_Updates;

    public DetectorsUpdater(JobManager jobManager, JobDetails job, String updateKey, StringWriter
            configWriter)
    {
        super(jobManager, job, updateKey);
        m_Updates = new ArrayList<>();
        m_ConfigWriter = configWriter;
    }

    @Override
    void prepareUpdate(JsonNode node) throws JobConfigurationException
    {
        JobDetails job = job();
        parseUpdate(node);
        int detectorsCount = job.getAnalysisConfig().getDetectors().size();
        for (UpdateParams update : m_Updates)
        {
            validateDetectorIndex(update, detectorsCount);
            fillDefaultDescriptionIfSetEmpty(job, update);
            validateDetectorRules(job, update);
        }
    }

    private void parseUpdate(JsonNode node) throws JobConfigurationException
    {
        if (!node.isArray())
        {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_UPDATE_DETECTORS_INVALID);
            throw new JobConfigurationException(msg, ErrorCodes.INVALID_VALUE);
        }
        Iterator<JsonNode> iterator = node.iterator();
        while (iterator.hasNext())
        {
            parseArrayElement(iterator.next());
        }
    }

    private void parseArrayElement(JsonNode node) throws JobConfigurationException
    {
        Map<String, Object> updateParams = convertToMap(node, () -> createInvalidParamsMsg());
        Set<String> updateKeys = updateParams.keySet();
        if (updateKeys.size() < 2 || !updateKeys.containsAll(REQUIRED_PARAMS)
                || updateKeys.stream().anyMatch(s -> !isValidParam(s)))
        {
            throw new JobConfigurationException(createInvalidParamsMsg(), ErrorCodes.INVALID_VALUE);
        }
        parseUpdateParams(updateParams);
    }

    private static String createInvalidParamsMsg()
    {
        return Messages.getMessage(Messages.JOB_CONFIG_UPDATE_DETECTORS_MISSING_PARAMS,
                REQUIRED_PARAMS, OPTIONAL_PARAMS);
    }

    private static boolean isValidParam(String key)
    {
        return REQUIRED_PARAMS.contains(key) || OPTIONAL_PARAMS.contains(key);
    }

    private void parseUpdateParams(Map<String, Object> updateParams) throws JobConfigurationException
    {
        UpdateParams parsedParams = new UpdateParams(
                parseDetectorIndex(updateParams.get(DETECTOR_INDEX)));

        if (updateParams.containsKey(DESCRIPTION))
        {
            parsedParams.detectorDescription = parseDescription(updateParams.get(DESCRIPTION));
        }

        if (updateParams.containsKey(DETECTOR_RULES))
        {
            parsedParams.detectorRules = parseDetectorRules(updateParams.get(DETECTOR_RULES));
        }

        m_Updates.add(parsedParams);
    }

    private static int parseDetectorIndex(Object detectorIndex) throws JobConfigurationException
    {
        if (!(detectorIndex instanceof Integer))
        {
            String msg = Messages.getMessage(
                    Messages.JOB_CONFIG_UPDATE_DETECTORS_DETECTOR_INDEX_SHOULD_BE_INTEGER,
                    detectorIndex);
            throw new JobConfigurationException(msg, ErrorCodes.INVALID_VALUE);
        }
        return (int) detectorIndex;
    }

    private static String parseDescription(Object description) throws JobConfigurationException
    {
        if (!(description instanceof String))
        {
            String msg = Messages.getMessage(
                    Messages.JOB_CONFIG_UPDATE_DETECTORS_DESCRIPTION_SHOULD_BE_STRING, description);
            throw new JobConfigurationException(msg, ErrorCodes.INVALID_VALUE);
        }
        return (String) description;
    }

    private static List<DetectionRule> parseDetectorRules(Object rules) throws JobConfigurationException
    {
        try
        {
            return JSON_MAPPER.convertValue(rules, new TypeReference<List<DetectionRule>>() {});
        }
        catch (IllegalArgumentException e)
        {
            throw new JobConfigurationParseException(
                    Messages.getMessage(Messages.JOB_CONFIG_UPDATE_DETECTOR_RULES_PARSE_ERROR),
                    e.getCause(), ErrorCodes.INVALID_VALUE);
        }
    }

    private void validateDetectorIndex(UpdateParams update, int detectorsCount)
            throws JobConfigurationException
    {
        if (update.detectorIndex < 0 || update.detectorIndex >= detectorsCount)
        {
            String msg = Messages.getMessage(
                    Messages.JOB_CONFIG_UPDATE_DETECTORS_INVALID_DETECTOR_INDEX, 0,
                    detectorsCount - 1, update.detectorIndex);
            throw new JobConfigurationException(msg, ErrorCodes.INVALID_VALUE);
        }
    }

    private void fillDefaultDescriptionIfSetEmpty(JobDetails job, UpdateParams update)
    {
        if (update.detectorDescription != null && update.detectorDescription.isEmpty())
        {
            update.detectorDescription = DefaultDetectorDescription.of(
                    job.getAnalysisConfig().getDetectors().get(update.detectorIndex));
        }
    }

    private static void validateDetectorRules(JobDetails job, UpdateParams update)
            throws JobConfigurationException
    {
        if (update.detectorRules == null || update.detectorRules.isEmpty())
        {
            return;
        }
        Detector detector = job.getAnalysisConfig().getDetectors().get(update.detectorIndex);
        for (DetectionRule rule : update.detectorRules)
        {
            DetectionRuleVerifier.verify(rule, detector);
        }
    }

    @Override
    void commit() throws JobException
    {
        for (UpdateParams update : m_Updates)
        {
            if (!update.commit())
            {
                throw new JobConfigurationException(
                        Messages.getMessage(Messages.JOB_CONFIG_UPDATE_FAILED),
                        ErrorCodes.UNKNOWN_ERROR);
            }
        }
    }

    private class UpdateParams
    {
        final int detectorIndex;
        String detectorDescription;
        List<DetectionRule> detectorRules;

        public UpdateParams(int detectorIndex)
        {
            this.detectorIndex = detectorIndex;
        }

        boolean commit() throws JobException
        {
            return commitDescription() && commitRules();
        }

        private boolean commitDescription() throws JobException
        {
            return detectorDescription == null
                    || jobManager().updateDetectorDescription(jobId(), detectorIndex, detectorDescription);
        }

        private boolean commitRules() throws JobException
        {
            if (detectorRules == null)
            {
                return true;
            }
            if (jobManager().updateDetectorRules(jobId(), detectorIndex, detectorRules))
            {
                writeRules();
                return true;
            }
            return false;
        }

        private void writeRules() throws JobConfigurationException
        {
            String rulesJson = "";
            try
            {
                rulesJson = JSON_MAPPER.writeValueAsString(detectorRules);
            }
            catch (JsonProcessingException e)
            {
                throw new JobConfigurationException("Failed to write detectorRules",
                        ErrorCodes.UNKNOWN_ERROR, e);
            }
            m_ConfigWriter.write("[detectorRules]\n");
            m_ConfigWriter.write("detectorIndex = " + detectorIndex + "\n");
            m_ConfigWriter.write("rulesJson = " + rulesJson + "\n");
        }
    }
}
