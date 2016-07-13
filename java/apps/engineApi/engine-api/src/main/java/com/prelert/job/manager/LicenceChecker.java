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

import org.apache.log4j.Logger;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.LicenseViolationException;
import com.prelert.job.exceptions.TooManyJobsException;
import com.prelert.job.messages.Messages;

/**
 * There are 2 situations where the licence is checked
 * <ol>
 * <li>On Job creation</li>
 * <li>Where a closed job is reactivated by sending data when it is closed</li>
 * </ol>
 * Specific method for each case.
 */
class LicenceChecker
{
    // Licence checking is done when the job is either
    // created or reactivated by having data sent to it
    private enum JobState {
        CREATE, REACTIVATE
    }

    private static final Logger LOGGER = Logger.getLogger(LicenceChecker.class);
    private final BackendInfo m_BackendInfo;

    public LicenceChecker(BackendInfo backendInfo)
    {
        m_BackendInfo = backendInfo;
    }


    /**
     * Use on job creation.
     * Checks these conditions
     *   <ul>
     *     <li>Check the number of running jobs against the licence</li>
     *     <li>Check the new job doesn't have more detectors than the licence allows</li>
     *     <li>If the job uses partitions check partitions are licensed</li>
     *   </ul>
     *
     * @param config
     * @param numberOfRunningJobs
     * @return
     * @throws TooManyJobsException
     * @throws LicenseViolationException
     */
    public boolean checkLicenceViolationsOnCreate(AnalysisConfig config, int numberOfRunningJobs)
    throws LicenseViolationException
    {
        checkNumberOfJobsAgainstLicenseLimit(null, numberOfRunningJobs, JobState.CREATE);
        checkTooManyDetectorsAgainstLicenseLimit(config);
        checkPartitionsAllowedAgainstLicense(config);

        return true;
    }

    /**
     * Use on job reactivation.
     * Checks these conditions
     *   <ul>
     *     <li>Check the number of running jobs against the licence</li>
     *     <li>Check the total number of detectors in running jobs against the licence</li>
     *     <li>Check against the hardware limit (number of CPUs)</li>
     *   </ul>
     *
     * @param jobId
     * @param numberOfJobs Number of Scheduled and Running jobs
     * @param numDetectorsInJob Detectors in this new job that is about to start
     * @param numberOfDetectors Total number of
     * @param
     * @return true
     * @throws TooManyJobsException
     * @throws LicenseViolationException
     */
    public boolean checkLicenceViolationsOnReactivate(String jobId, int numberOfJobs,
                                                    int numDetectorsInJob, int numberOfDetectors)
    throws TooManyJobsException, LicenseViolationException
    {
        checkNumberOfJobsAgainstLicenseLimit(jobId, numberOfJobs, JobState.REACTIVATE);
        checkNumberOfRunningDetectorsAgainstLicenseLimit(jobId, numberOfDetectors, numDetectorsInJob);
        checkNumberOfJobsAgainstHardwareLimit(jobId, numberOfJobs);

        return true;
    }


    private void checkNumberOfJobsAgainstLicenseLimit(String jobId, int numJobs, JobState jobState)
    throws LicenseViolationException
    {
        if (m_BackendInfo.isLicenseJobLimitViolated(numJobs))
        {
            String message;
            if (jobState == JobState.CREATE)
            {
                message = Messages.getMessage(Messages.LICENSE_LIMIT_JOBS,  m_BackendInfo.getLicenseJobLimit());
            }
            else
            {
                message = Messages.getMessage(Messages.LICENSE_LIMIT_JOBS_REACTIVATE,  jobId,
                                            m_BackendInfo.getLicenseJobLimit());
            }

            LOGGER.info(message);
            throw new LicenseViolationException(message, ErrorCodes.LICENSE_VIOLATION);
        }
    }

    private void checkNumberOfJobsAgainstHardwareLimit(String jobId, int numJobs)
    throws TooManyJobsException
    {
        if (m_BackendInfo.isCpuLimitViolated(numJobs))
        {
            String message = Messages.getMessage(Messages.CPU_LIMIT_JOB, jobId);

            LOGGER.info(message);
            throw new TooManyJobsException(numJobs,
                    message, ErrorCodes.TOO_MANY_JOBS_RUNNING_CONCURRENTLY);
        }
    }

    private void checkTooManyDetectorsAgainstLicenseLimit(AnalysisConfig config)
    throws LicenseViolationException
    {
        // Negative m_MaxDetectorsPerJob means unlimited
        if (m_BackendInfo.getMaxRunningDetectors() >= 0
                && config != null
                && config.getDetectors().size() > m_BackendInfo.getMaxRunningDetectors())
        {
            String message = Messages.getMessage(
                                Messages.LICENSE_LIMIT_DETECTORS,
                                m_BackendInfo.getMaxRunningDetectors(),
                                config.getDetectors().size());

            LOGGER.info(message);
            throw new LicenseViolationException(message, ErrorCodes.LICENSE_VIOLATION);
        }
    }


    private void checkNumberOfRunningDetectorsAgainstLicenseLimit(String jobId,
                                                                int currentNumDetectors,
                                                                int additionalDetectors)
    throws LicenseViolationException
    {
        if (m_BackendInfo.isLicenseDetectorLimitViolated(currentNumDetectors, additionalDetectors))
        {
            String message = Messages.getMessage(Messages.LICENSE_LIMIT_DETECTORS_REACTIVATE,
                                                jobId, m_BackendInfo.getMaxRunningDetectors());

            LOGGER.info(message);
            throw new LicenseViolationException(message, ErrorCodes.LICENSE_VIOLATION);
        }
    }

    private void checkPartitionsAllowedAgainstLicense(AnalysisConfig config)
    throws LicenseViolationException
    {
        if (!m_BackendInfo.arePartitionsAllowed() && config != null)
        {
            for (com.prelert.job.Detector detector : config.getDetectors())
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

}
