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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataCounts;
import com.prelert.job.Detector;
import com.prelert.job.JobDetails;
import com.prelert.job.JobSchedulerStatus;
import com.prelert.job.JobStatus;
import com.prelert.job.ModelSizeStats;
import com.prelert.job.audit.Auditor;

public class ActivityAuditTest
{
    @Mock private Supplier<Auditor> m_AuditorSupplier;
    @Mock private Supplier<List<JobDetails>> m_AllJobsSupplier;
    @Mock private Supplier<List<JobDetails>> m_ActiveJobsSupplier;
    @Mock private Auditor m_Auditor;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testReportNoJobs() throws InterruptedException
    {
        ActivityAudit aa = new ActivityAudit(m_AuditorSupplier, m_AllJobsSupplier, m_ActiveJobsSupplier);
        List<JobDetails> allJobs = new ArrayList<>();
        when(m_AllJobsSupplier.get()).thenReturn(allJobs);
        when(m_ActiveJobsSupplier.get()).thenReturn(allJobs);

        when(m_AuditorSupplier.get()).thenReturn(m_Auditor);

        aa.report();

        verify(m_Auditor).activity(0, 0, 0, 0);
    }

    @Test
    public void testReportActiveJobs() throws InterruptedException
    {
        ActivityAudit aa = new ActivityAudit(m_AuditorSupplier, m_AllJobsSupplier, m_ActiveJobsSupplier);

        JobDetails job1 = mock(JobDetails.class);
        JobDetails job2 = mock(JobDetails.class);

        DataCounts counts = mock(DataCounts.class);
        when(counts.getProcessedRecordCount()).thenReturn(42L);
        when(counts.getInputBytes()).thenReturn(9999L);

        ModelSizeStats mss = mock(ModelSizeStats.class);
        when(mss.getModelBytes()).thenReturn(33333333L);

        when(job2.getId()).thenReturn("job2id");
        when(job2.getStatus()).thenReturn(JobStatus.PAUSED);
        when(job2.getSchedulerStatus()).thenReturn(JobSchedulerStatus.STARTED);
        when(job2.getCounts()).thenReturn(counts);
        when(job2.getModelSizeStats()).thenReturn(mss);
        when(job2.getCreateTime()).thenReturn(new Date(1234567890000L));
        when(job2.getLastDataTime()).thenReturn(new Date(1345678902000L));

        List<JobDetails> allJobs = Arrays.asList(job1, job2);
        List<JobDetails> activeJobs = Arrays.asList(job2);
        when(m_AllJobsSupplier.get()).thenReturn(allJobs);
        when(m_ActiveJobsSupplier.get()).thenReturn(activeJobs);

        when(m_AuditorSupplier.get()).thenReturn(m_Auditor);

        AnalysisConfig config1 = mock(AnalysisConfig.class);
        AnalysisConfig config2 = mock(AnalysisConfig.class);

        Detector d = mock(Detector.class);
        List<Detector> detectors1 = Arrays.asList(d, d, d, d, d, d, d, d, d, d, d, d, d, d, d);
        List<Detector> detectors2 = Arrays.asList(d, d, d, d);
        when(config1.getDetectors()).thenReturn(detectors1);
        when(config2.getDetectors()).thenReturn(detectors2);

        when(job1.getAnalysisConfig()).thenReturn(config1);
        when(job2.getAnalysisConfig()).thenReturn(config2);

        aa.report();

        verify(m_Auditor).activity(2, 19, 1, 4);
        verify(m_Auditor).activity("jobId=job2id numDetectors=4 status=PAUSED scheduledStatus=STARTED "
                + "processedRecords=42 inputBytes=9999 modelMemory=33333333 "
                + "createTime=Fri Feb 13 23:31:30 GMT 2009 lastDataTime=Thu Aug 23 00:41:42 BST 2012\n");
    }

}
