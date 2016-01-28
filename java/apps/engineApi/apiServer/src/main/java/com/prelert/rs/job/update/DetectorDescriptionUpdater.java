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

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import com.prelert.job.JobDetails;
import com.prelert.job.UnknownJobException;
import com.prelert.job.config.verification.JobConfigurationException;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.manager.JobManager;
import com.prelert.job.messages.Messages;

class DetectorDescriptionUpdater extends AbstractUpdater
{
    private static final String DETECTOR_INDEX = "index";
    private static final String DESCRIPTION = "description";
    private static final Set<String> PARAMS = Sets.newLinkedHashSet(
            Arrays.asList(DETECTOR_INDEX, DESCRIPTION));

    public DetectorDescriptionUpdater(JobManager jobManager, String jobId)
    {
        super(jobManager, jobId);
    }

    @Override
    void update(JsonNode node) throws UnknownJobException, JobConfigurationException
    {
        Optional<JobDetails> job = jobManager().getJob(jobId());
        if (!job.isPresent())
        {
            throw new UnknownJobException(jobId());
        }
        UpdateParams params = parseParams(node);
        int detectorsCount = job.get().getAnalysisConfig().getDetectors().size();
        if (params.detectorIndex < 0 || params.detectorIndex >= detectorsCount)
        {
            String msg = Messages.getMessage(
                    Messages.JOB_CONFIG_UPDATE_DETECTOR_DESCRIPTION_INVALID_DETECTOR_INDEX, 0,
                    detectorsCount - 1, params.detectorIndex);
            throw new JobConfigurationException(msg, ErrorCodes.INVALID_VALUE);
        }
        if (jobManager().updateDetectorDescription(jobId(), params.detectorIndex, params.name) == false)
        {
            throw new JobConfigurationException(
                    Messages.getMessage(Messages.JOB_CONFIG_UPDATE_DETECTOR_DESCRIPTION_FAILED),
                    ErrorCodes.UNKNOWN_ERROR);
        }
    }

    private UpdateParams parseParams(JsonNode node) throws JobConfigurationException
    {
        Map<String, Object> updateParams = convertToMap(node);
        if (!PARAMS.equals(updateParams.keySet()))
        {
            String msg = Messages.getMessage(
                    Messages.JOB_CONFIG_UPDATE_DETECTOR_DESCRIPTION_MISSING_PARAMS, PARAMS);
            throw new JobConfigurationException(msg, ErrorCodes.INVALID_VALUE);
        }
        Object detectorIndex = updateParams.get(DETECTOR_INDEX);
        Object name = updateParams.get(DESCRIPTION);
        if (!(detectorIndex instanceof Integer))
        {
            String msg = Messages.getMessage(
                    Messages.JOB_CONFIG_UPDATE_DETECTOR_DESCRIPTION_DETECTOR_INDEX_SHOULD_BE_INTEGER,
                    detectorIndex);
            throw new JobConfigurationException(msg, ErrorCodes.INVALID_VALUE);
        }
        if (!(name instanceof String))
        {
            String msg = Messages.getMessage(
                    Messages.JOB_CONFIG_UPDATE_DETECTOR_DESCRIPTION_SHOULD_BE_STRING, name);
            throw new JobConfigurationException(msg, ErrorCodes.INVALID_VALUE);
        }
        return new UpdateParams((int) detectorIndex, (String) name);
    }

    private Map<String, Object> convertToMap(JsonNode node) throws JobConfigurationException
    {
        try
        {
            return JSON_MAPPER.convertValue(node, new TypeReference<Map<String, Object>>() {});
        }
        catch (IllegalArgumentException e)
        {
            String msg = Messages.getMessage(
                    Messages.JOB_CONFIG_UPDATE_DETECTOR_DESCRIPTION_MISSING_PARAMS, PARAMS);
            throw new JobConfigurationException(msg, ErrorCodes.INVALID_VALUE, e);
        }
    }

    private static class UpdateParams
    {
        final int detectorIndex;
        final String name;

        public UpdateParams(int detectorIndex, String name)
        {
            this.detectorIndex = detectorIndex;
            this.name = name;
        }
    }
}
