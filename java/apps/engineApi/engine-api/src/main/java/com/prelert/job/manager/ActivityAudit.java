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

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.prelert.job.audit.Auditor;

/**
 * Log the number of running jobs and detectors to the auditor
 * every 40 to 80 minutes
 */
public class ActivityAudit
{
    private final Supplier<Auditor> m_AuditorSupplier;
    private final Supplier<Integer> m_NumJobsSupplier;
    private final Supplier<Integer> m_NumDetectorsSupplier;
    private final ScheduledExecutorService m_ScheduledService;

    public ActivityAudit(Supplier<Auditor> auditorSupplier,
                      Supplier<Integer> numJobsSupplier,
                      Supplier<Integer> numDetectorsSupplier)
    {
        m_AuditorSupplier = auditorSupplier;
        m_NumJobsSupplier = numJobsSupplier;
        m_NumDetectorsSupplier = numDetectorsSupplier;

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
        long delay = 40 + rand.nextInt(40);
        m_ScheduledService.schedule(() -> this.report(), delay, TimeUnit.MINUTES);
    }

    private void report()
    {
        Auditor auditor = m_AuditorSupplier.get();
        auditor.info(buildMessage(m_NumJobsSupplier.get(), m_NumDetectorsSupplier.get()));

        scheduleNextAudit();
    }

    private String buildMessage(Integer numJobs, Integer numDetectors)
    {
        return "Activity: " + numJobs + " jobs and a total of " + numDetectors + " detectors";
    }
}
