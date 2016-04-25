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

import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.apache.log4j.Logger;

import com.prelert.job.JobDetails;
import com.prelert.job.audit.Auditor;

/**
 * Log the number of running jobs and detectors to the auditor
 * every 40 to 80 minutes
 */
public class ActivityAudit
{
    private static final Logger LOGGER = Logger.getLogger(ActivityAudit.class);
    private static final int MIN_DELAY_MINUTES = 40;

    private final Supplier<Auditor> m_AuditorSupplier;
    private final Supplier<List<JobDetails>> m_RunningJobsSupplier;

    private final ScheduledExecutorService m_ScheduledService;

    public ActivityAudit(Supplier<Auditor> auditorSupplier,
                      Supplier<List<JobDetails>> runningJobsSupplier)
    {
        m_AuditorSupplier = auditorSupplier;
        m_RunningJobsSupplier = runningJobsSupplier;

        // creates daemon threads
        m_ScheduledService = Executors.newSingleThreadScheduledExecutor(
                (Runnable r) -> {
                    Thread thread = Executors.defaultThreadFactory().newThread(r);
                    thread.setDaemon(true);
                    thread.setName("Licence-Usage-Audit-Thread");
                    return thread;
                });
    }

    /**
     * Schedule event 40 - 80 mins from now
     */
    public void scheduleNextAudit()
    {
        Random rand = new Random();
        long delay = MIN_DELAY_MINUTES + rand.nextInt(MIN_DELAY_MINUTES);
        m_ScheduledService.schedule(() -> this.report(), delay, TimeUnit.MINUTES);
    }

    private void report()
    {
        try
        {
            List<JobDetails> jobDetails = m_RunningJobsSupplier.get();
            int numDetectors = 0;
            for (JobDetails jd : jobDetails)
            {
                numDetectors += jd.getAnalysisConfig().getDetectors().size();
            }

            Auditor auditor = m_AuditorSupplier.get();
            auditor.activity(buildActivityMessage(jobDetails.size(), numDetectors));

            if (jobDetails.isEmpty() == false)
            {
                auditor.info(buildUsageMessage(jobDetails));
            }
        }
        catch (Exception e)
        {
            LOGGER.error(e);
        }
        finally
        {
            scheduleNextAudit();
        }
    }

    private String buildActivityMessage(Integer numJobs, Integer numDetectors)
    {
        return "Activity: " + numJobs + " jobs and a total of " + numDetectors + " detectors";
    }

    private String buildUsageMessage(List<JobDetails> jobDetails)
    {
        StringBuilder sb = new StringBuilder();

        for (JobDetails job : jobDetails)
        {
            sb.append("jobId=").append(job.getId());
            sb.append(" numDetectors=").append(Integer.toString(job.getAnalysisConfig().getDetectors().size()));
            sb.append(" status=").append(job.getStatus().toString());
            if (job.getSchedulerStatus() != null)
            {
                sb.append(" scheduledStatus=").append(job.getSchedulerStatus().toString());
            }

            if (job.getCounts() != null)
            {
                sb.append(" processedRecords=").append(Long.toString(job.getCounts().getProcessedRecordCount()));
                sb.append(" inputBytes=").append(Long.toString(job.getCounts().getInputBytes()));
            }
            if (job.getModelSizeStats() != null)
            {
                sb.append(" modelMemory=").append(Long.toString(job.getModelSizeStats().getModelBytes()));
            }
            if (job.getCreateTime() != null)
            {
                sb.append(" createTime=").append(job.getCreateTime().toString());
            }
            if (job.getLastDataTime() != null)
            {
                sb.append(" lastDataTime=").append(job.getLastDataTime().toString());
            }

            sb.append(System.lineSeparator());
        }

        return sb.toString();
    }
}
