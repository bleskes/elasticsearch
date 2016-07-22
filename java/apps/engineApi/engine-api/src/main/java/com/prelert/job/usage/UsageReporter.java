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

package com.prelert.job.usage;

import org.apache.log4j.Logger;

import com.prelert.job.JobException;
import com.prelert.job.UnknownJobException;
import com.prelert.job.persistence.UsagePersister;
import com.prelert.settings.PrelertSettings;

/**
 * Reports the number of bytes, fields and records read.
 * Persistence is done via {@linkplain UsagePersister}
 * The main difference betweeen this and the {@linkplain com.prelert.job.status.StatusReporter}
 * is that this writes hourly reports i.e. how much data was read in an hour
 */
public class UsageReporter
{
    public static final String UPDATE_INTERVAL_PROP = "usage.update.interval";
    private static final long UPDATE_AFTER_COUNT_SECS = 300;

    private String m_JobId;
    private Logger m_Logger;

    private long m_BytesReadSinceLastReport;
    private long m_FieldsReadSinceLastReport;
    private long m_RecordsReadSinceLastReport;

    private long m_LastUpdateTimeMs;
    private long m_UpdateIntervalMs = UPDATE_AFTER_COUNT_SECS * 1000;

    private UsagePersister m_Persister;

    public UsageReporter(String jobId, UsagePersister persister, Logger logger)
    {
        m_BytesReadSinceLastReport = 0;
        m_FieldsReadSinceLastReport = 0;
        m_RecordsReadSinceLastReport = 0;

        m_JobId = jobId;
        m_Persister = persister;
        m_Logger = logger;

        m_LastUpdateTimeMs = System.currentTimeMillis();

        long interval = PrelertSettings.getSettingOrDefault(UPDATE_INTERVAL_PROP, UPDATE_AFTER_COUNT_SECS);
        m_UpdateIntervalMs = interval * 1000;
        m_Logger.info("Setting usage update interval to " + interval + " seconds");
    }

    /**
     * Add <code>bytesRead</code> to the running total
     * @param bytesRead
     */
    public void addBytesRead(long bytesRead)
    {
        m_BytesReadSinceLastReport += bytesRead;

        long now = System.currentTimeMillis();

        if (now - m_LastUpdateTimeMs > m_UpdateIntervalMs)
        {
            reportUsage(now);
        }
    }

    public void addFieldsRecordsRead(long fieldsRead)
    {
        m_FieldsReadSinceLastReport += fieldsRead;
        ++m_RecordsReadSinceLastReport;
    }

    public long getBytesReadSinceLastReport()
    {
        return m_BytesReadSinceLastReport;
    }

    public long getFieldsReadSinceLastReport()
    {
        return m_FieldsReadSinceLastReport;
    }

    public long getRecordsReadSinceLastReport()
    {
        return m_RecordsReadSinceLastReport;
    }


    public String getJobId()
    {
        return m_JobId;
    }

    public Logger getLogger()
    {
        return m_Logger;
    }

    /**
     * Logs total bytes written and calls {@linkplain persistUsageCounts()}
     * m_BytesReadSinceLastReport, m_FieldsReadSinceLastReport and
     * m_RecordsReadSinceLastReport are reset to 0 after this has been called.
     */
    public void reportUsage()
    {
        reportUsage(System.currentTimeMillis());
    }

    /**
     * See {@linkplain #reportUsage()}
     *
     * @param epochMs The time now - saved as the last update time
     * @throws JobException
     * @throws UnknownJobException
     */
    private void reportUsage(long epochMs)
    {
        m_Logger.info(String.format("An additional %dKiB, %d fields and %d records read by job %s",
                m_BytesReadSinceLastReport >> 10, m_FieldsReadSinceLastReport,
                m_RecordsReadSinceLastReport, m_JobId));

        try
        {
            m_Persister.persistUsage(m_JobId, m_BytesReadSinceLastReport,
                    m_FieldsReadSinceLastReport, m_RecordsReadSinceLastReport);
        }
        catch (JobException e)
        {
            m_Logger.error("Error persisting usage for job " + m_JobId, e);
        }

        m_LastUpdateTimeMs = epochMs;

        m_BytesReadSinceLastReport = 0;
        m_FieldsReadSinceLastReport = 0;
        m_RecordsReadSinceLastReport = 0;
    }
}
