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

package com.prelert.job.scheduler;

import java.util.Objects;
import java.util.function.Supplier;

import com.prelert.job.audit.Auditor;
import com.prelert.job.messages.Messages;

/**
 * <p>
 * Keeps track of problems the scheduler encounters and audits
 * messages appropriately.
 * </p>
 * <p>
 * The {@code ProblemTracker} is expected to interact with multiple
 * threads (lookback executor, real-time executor). However, each
 * thread will be accessing in a sequential manner therefore we
 * only need to ensure correct visibility.
 * </p>
 */
class ProblemTracker
{
    private static final int EMPTY_DATA_WARN_COUNT = 10;

    private final Supplier<Auditor> m_Auditor;

    private volatile boolean m_HasProblems;
    private volatile boolean m_HadProblems;
    private volatile String m_PreviousProblem;

    private volatile int m_EmptyDataCount;

    public ProblemTracker(Supplier<Auditor> auditor)
    {
        m_Auditor = Objects.requireNonNull(auditor);
    }

    /**
     * Reports as analysis problem if it is different than the last seen problem
     *
     * @param problemMessage the problem message
     */
    public void reportAnalysisProblem(String problemMessage)
    {
        reportProblem(Messages.JOB_AUDIT_SCHEDULER_DATA_ANALYSIS_ERROR, problemMessage);
    }

    /**
     * Reports as extraction problem if it is different than the last seen problem
     *
     * @param problemMessage the problem message
     */
    public void reportExtractionProblem(String problemMessage)
    {
        reportProblem(Messages.JOB_AUDIT_SCHEDULER_DATA_EXTRACTION_ERROR, problemMessage);
    }

    /**
     * Reports the problem if it is different than the last seen problem
     *
     * @param problemMessage the problem message
     */
    private void reportProblem(String template, String problemMessage)
    {
        m_HasProblems = true;
        if (!Objects.equals(m_PreviousProblem, problemMessage))
        {
            m_PreviousProblem = problemMessage;
            m_Auditor.get().error(Messages.getMessage(template, problemMessage));
        }
    }

    /**
     * Updates the tracking of empty data cycles. If the number of consecutive empty data
     * cycles reaches {@code EMPTY_DATA_WARN_COUNT}, a warning is reported. If non-empty
     * is reported and a warning was issued previously, a recovery info is reported.
     *
     * @param empty Whether data was seen since last report
     * @return {@code true} if an empty data warning was issued, {@code false} otherwise
     */
    public boolean updateEmptyDataCount(boolean empty)
    {
        if (empty && m_EmptyDataCount < EMPTY_DATA_WARN_COUNT)
        {
            m_EmptyDataCount++;
            if (m_EmptyDataCount == EMPTY_DATA_WARN_COUNT)
            {
                m_Auditor.get().warning(Messages.getMessage(Messages.JOB_AUDIT_SCHEDULER_NO_DATA));
                return true;
            }
        }
        else if (!empty)
        {
            if (m_EmptyDataCount >= EMPTY_DATA_WARN_COUNT)
            {
                m_Auditor.get().info(Messages.getMessage(Messages.JOB_AUDIR_SCHEDULER_DATA_SEEN_AGAIN));
            }
            m_EmptyDataCount = 0;
        }
        return false;
    }

    public boolean hasProblems()
    {
        return m_HasProblems;
    }

    /**
     * Issues a recovery message if appropriate and prepares for next report
     */
    public void finishReport()
    {
        if (!m_HasProblems && m_HadProblems)
        {
            m_Auditor.get().info(Messages.getMessage(
                    Messages.JOB_AUDIT_SCHEDULER_RECOVERED));
        }

        m_HadProblems = m_HasProblems;
        m_HasProblems = false;
    }
}
