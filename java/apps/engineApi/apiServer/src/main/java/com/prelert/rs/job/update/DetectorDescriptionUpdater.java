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
import com.prelert.job.UnknownJobException;
import com.prelert.job.config.DefaultDetectorDescription;
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

    private List<UpdateParams> m_Updates;

    public DetectorDescriptionUpdater(JobManager jobManager, JobDetails job, String updateKey)
    {
        super(jobManager, job, updateKey);
        m_Updates = new ArrayList<>();
    }

    @Override
    void prepareUpdate(JsonNode node) throws JobConfigurationException
    {
        JobDetails job = job();
        parseParams(node);
        int detectorsCount = job.getAnalysisConfig().getDetectors().size();
        for (UpdateParams update : m_Updates)
        {
            validateDetectorIndex(update, detectorsCount);
            fillDefaultDescriptionIfEmpty(job, update);
        }
    }

    private void parseParams(JsonNode node) throws JobConfigurationException
    {
        if (!node.isArray())
        {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_UPDATE_DETECTOR_DESCRIPTION_INVALID);
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
        Map<String, Object> updateParams = convertToMap(node, () -> Messages.getMessage(
                Messages.JOB_CONFIG_UPDATE_DETECTOR_DESCRIPTION_MISSING_PARAMS, PARAMS));
        if (!PARAMS.equals(updateParams.keySet()))
        {
            String msg = Messages.getMessage(
                    Messages.JOB_CONFIG_UPDATE_DETECTOR_DESCRIPTION_MISSING_PARAMS, PARAMS);
            throw new JobConfigurationException(msg, ErrorCodes.INVALID_VALUE);
        }
        Object detectorIndex = updateParams.get(DETECTOR_INDEX);
        Object description = updateParams.get(DESCRIPTION);
        if (!(detectorIndex instanceof Integer))
        {
            String msg = Messages.getMessage(
                    Messages.JOB_CONFIG_UPDATE_DETECTOR_DESCRIPTION_DETECTOR_INDEX_SHOULD_BE_INTEGER,
                    detectorIndex);
            throw new JobConfigurationException(msg, ErrorCodes.INVALID_VALUE);
        }
        if (!(description instanceof String))
        {
            String msg = Messages.getMessage(
                    Messages.JOB_CONFIG_UPDATE_DETECTOR_DESCRIPTION_SHOULD_BE_STRING, description);
            throw new JobConfigurationException(msg, ErrorCodes.INVALID_VALUE);
        }
        m_Updates.add(new UpdateParams((int) detectorIndex, (String) description));
    }

    private void validateDetectorIndex(UpdateParams update, int detectorsCount)
            throws JobConfigurationException
    {
        if (update.detectorIndex < 0 || update.detectorIndex >= detectorsCount)
        {
            String msg = Messages.getMessage(
                    Messages.JOB_CONFIG_UPDATE_DETECTOR_DESCRIPTION_INVALID_DETECTOR_INDEX, 0,
                    detectorsCount - 1, update.detectorIndex);
            throw new JobConfigurationException(msg, ErrorCodes.INVALID_VALUE);
        }
    }

    private void fillDefaultDescriptionIfEmpty(JobDetails job, UpdateParams update)
    {
        if (update.detectorDescription.isEmpty())
        {
            update.detectorDescription = DefaultDetectorDescription.of(
                    job.getAnalysisConfig().getDetectors().get(update.detectorIndex));
        }
    }

    @Override
    void commit() throws UnknownJobException, JobConfigurationException
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

        public UpdateParams(int detectorIndex, String detectorDescription)
        {
            this.detectorIndex = detectorIndex;
            this.detectorDescription = detectorDescription;
        }
    }
}
