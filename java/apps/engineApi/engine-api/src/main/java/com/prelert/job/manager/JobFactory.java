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
import java.util.concurrent.atomic.AtomicLong;

import com.prelert.job.Detector;
import com.prelert.job.JobConfiguration;
import com.prelert.job.JobDetails;
import com.prelert.job.config.DefaultDetectorDescription;
import com.prelert.job.config.verification.JobConfigurationException;
import com.prelert.job.config.verification.JobConfigurationVerifier;

/**
 * A factory that creates new jobs.
 * If the hostname constructor is used the hostname is converted to
 * lowercase to make it compatible with Elasticsearch index names
 */
class JobFactory
{
    private final AtomicLong m_IdSequence;
    private final DateTimeFormatter m_JobIdDateFormat;
    private final String m_Hostname;

    public JobFactory()
    {
        this(null);
    }

    public JobFactory(String hostname)
    {
        m_Hostname = hostname == null ? null : hostname.toLowerCase();
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
     * @throws JobConfigurationException
     */
    public JobDetails create(JobConfiguration jobConfig)
            throws JobConfigurationException
    {
        String jobId = jobConfig.getId();
        if (jobId == null || jobId.isEmpty())
        {
            jobId = generateJobId();
        }
        JobDetails jobDetails = new JobDetails(jobId, jobConfig);
        fillDefaults(jobDetails);
        return jobDetails;
    }

    /**
     * If hostname is null the job Id is a concatenation of the date in
     * 'yyyyMMddHHmmss' format and a sequence number that is a minimum of
     * 5 digits wide left padded with zeros<br>
     * If hostname is not null the Id is the concatenation of the date in
     * 'yyyyMMddHHmmss' format the hostname and a sequence number that is a
     * minimum of 5 digits wide left padded with zeros. If hostname is long
     * and it is truncated so the job Id does not exceed the maximum length<br>
     *
     * e.g. the first Id created 23rd November 2013 at 11am
     *     '20131125110000-serverA-00001'
     *
     * @return The new unique job Id
     */
    String generateJobId()
    {
        String dateStr = m_JobIdDateFormat.format(LocalDateTime.now());
        if (m_Hostname != null) {
            int hostnameMaxLen =JobConfigurationVerifier.MAX_JOB_ID_LENGTH - dateStr.length() - 7;

            return String.format("%s-%s-%05d", dateStr,
                                m_Hostname.substring(0, Math.min(m_Hostname.length(), hostnameMaxLen)),
                                m_IdSequence.incrementAndGet());
        }
        else
        {
            return String.format("%s-%05d", dateStr, m_IdSequence.incrementAndGet());
        }
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
