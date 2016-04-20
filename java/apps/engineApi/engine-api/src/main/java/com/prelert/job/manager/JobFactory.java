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

package com.prelert.job.manager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import com.prelert.job.Detector;
import com.prelert.job.JobConfiguration;
import com.prelert.job.JobDetails;
import com.prelert.job.config.DefaultDetectorDescription;
import com.prelert.job.config.verification.JobConfigurationException;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.LicenseViolationException;
import com.prelert.job.messages.Messages;

/**
 * A factory that creates new jobs.
 */
class JobFactory
{
    private static final Logger LOGGER = Logger.getLogger(JobFactory.class);

    private final BackendInfo m_BackendInfo;
    private final AtomicLong m_IdSequence;
    private final DateTimeFormatter m_JobIdDateFormat;

    public JobFactory(BackendInfo backendInfo)
    {
        m_BackendInfo = Objects.requireNonNull(backendInfo);
        m_IdSequence = new AtomicLong();
        m_JobIdDateFormat = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    }

    /**
     * Creates a new job according to the given configuration.
     * If no {@code id} was specified, a unique id is generated.
     * The method also checks that license related limits are not violated
     * by creating this job.
     *
     * @param jobConfig the job configuration
     * @param numberOfRunningJobs the number of running jobs at the moment of creation
     * @return the created {@Code JobDetails} object
     * @throws LicenseViolationException
     * @throws JobConfigurationException
     */
    public JobDetails create(JobConfiguration jobConfig, int numberOfRunningJobs)
            throws LicenseViolationException, JobConfigurationException
    {
        checkTooManyJobsAgainstLicenseLimit(numberOfRunningJobs);
        checkTooManyDetectorsAgainstLicenseLimit(jobConfig);
        checkPartitionsAllowedAgainstLicense(jobConfig);

        String jobId = jobConfig.getId();
        if (jobId == null || jobId.isEmpty())
        {
            jobId = generateJobId();
        }
        JobDetails jobDetails = new JobDetails(jobId, jobConfig);
        fillDefaults(jobDetails);
        return jobDetails;
    }

    private void checkTooManyJobsAgainstLicenseLimit(int numberOfRunningJobs)
            throws LicenseViolationException
    {
        if (m_BackendInfo.isLicenseJobLimitViolated(numberOfRunningJobs))
        {
            String message = Messages.getMessage(Messages.LICENSE_LIMIT_JOBS,
                    m_BackendInfo.getLicenseJobLimit());

            LOGGER.info(message);
            throw new LicenseViolationException(message, ErrorCodes.LICENSE_VIOLATION);
        }
    }

    private void checkTooManyDetectorsAgainstLicenseLimit(JobConfiguration jobConfig)
    throws LicenseViolationException
    {
        // Negative m_MaxDetectorsPerJob means unlimited
        if (m_BackendInfo.getMaxRunningDetectors() >= 0 &&
            jobConfig.getAnalysisConfig() != null &&
            jobConfig.getAnalysisConfig().getDetectors().size() > m_BackendInfo.getMaxRunningDetectors())
        {
            String message = Messages.getMessage(
                                Messages.LICENSE_LIMIT_DETECTORS,
                                m_BackendInfo.getMaxRunningDetectors(),
                                jobConfig.getAnalysisConfig().getDetectors().size());

            LOGGER.info(message);
            throw new LicenseViolationException(message, ErrorCodes.LICENSE_VIOLATION);
        }
    }

    private void checkPartitionsAllowedAgainstLicense(JobConfiguration jobConfig)
    throws LicenseViolationException
    {
        if (!m_BackendInfo.arePartitionsAllowed() && jobConfig.getAnalysisConfig() != null)
        {
            for (com.prelert.job.Detector detector :
                        jobConfig.getAnalysisConfig().getDetectors())
            {
                String partitionFieldName = detector.getPartitionFieldName();
                if (partitionFieldName != null &&
                    partitionFieldName.length() > 0)
                {
                    String message = Messages.getMessage(Messages.LICENSE_LIMIT_PARTITIONS);
                    LOGGER.info(message);
                    throw new LicenseViolationException(message, ErrorCodes.LICENSE_VIOLATION);
                }
            }
        }
    }

    /**
     * The job id is a concatenation of the date in 'yyyyMMddHHmmss' format
     * and a sequence number that is a minimum of 5 digits wide left padded
     * with zeros.<br>
     * e.g. the first Id created 23rd November 2013 at 11am
     *     '20131125110000-00001'
     *
     * @return The new unique job Id
     */
    private String generateJobId()
    {
        return String.format("%s-%05d", m_JobIdDateFormat.format(LocalDateTime.now()),
                        m_IdSequence.incrementAndGet());
    }

    private void fillDefaults(JobDetails jobDetails)
    {
        for (Detector detector : jobDetails.getAnalysisConfig().getDetectors())
        {
            if (detector.getDetectorDescription() == null ||
                    detector.getDetectorDescription().isEmpty())
            {
                detector.setDetectorDescription(DefaultDetectorDescription.of(detector));
            }
        }

        // Disable auto-close for scheduled jobs
        if (jobDetails.getSchedulerConfig() != null)
        {
            jobDetails.setTimeout(0);
        }
    }
}
