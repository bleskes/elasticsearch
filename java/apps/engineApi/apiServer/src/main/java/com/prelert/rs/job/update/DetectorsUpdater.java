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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import com.prelert.job.JobDetails;
import com.prelert.job.JobException;
import com.prelert.job.config.DefaultDetectorDescription;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobConfigurationException;
import com.prelert.job.manager.JobManager;
import com.prelert.job.messages.Messages;

class DetectorsUpdater extends AbstractUpdater
{
    private static final String DETECTOR_INDEX = "index";
    private static final String DESCRIPTION = "description";
    private static final Set<String> REQUIRED_PARAMS = Sets.newLinkedHashSet(Arrays.asList(DETECTOR_INDEX));
    private static final Set<String> OPTIONAL_PARAMS = Sets.newLinkedHashSet(Arrays.asList(DESCRIPTION));

    private List<UpdateParams> m_Updates;

    public DetectorsUpdater(JobManager jobManager, JobDetails job, String updateKey)
    {
        super(jobManager, job, updateKey);
        m_Updates = new ArrayList<>();
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
        Object detectorIndex = updateParams.get(DETECTOR_INDEX);
        if (!(detectorIndex instanceof Integer))
        {
            String msg = Messages.getMessage(
                    Messages.JOB_CONFIG_UPDATE_DETECTORS_DETECTOR_INDEX_SHOULD_BE_INTEGER,
                    detectorIndex);
            throw new JobConfigurationException(msg, ErrorCodes.INVALID_VALUE);
        }
        UpdateParams parsedParams = new UpdateParams((int) detectorIndex);

        if (updateParams.containsKey(DESCRIPTION))
        {
            parsedParams.detectorDescription = parseDescription(updateParams.get(DESCRIPTION));
        }

        m_Updates.add(parsedParams);
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

    @Override
    void commit() throws JobException
    {
        for (UpdateParams update : m_Updates)
        {
            if (jobManager().updateDetectorDescription(
                    jobId(), update.detectorIndex, update.detectorDescription) == false)
            {
                throw new JobConfigurationException(
                        Messages.getMessage(Messages.JOB_CONFIG_UPDATE_FAILED),
                        ErrorCodes.UNKNOWN_ERROR);
            }
        }
    }

    private static class UpdateParams
    {
        final int detectorIndex;
        String detectorDescription;

        public UpdateParams(int detectorIndex)
        {
            this.detectorIndex = detectorIndex;
        }
    }
}
