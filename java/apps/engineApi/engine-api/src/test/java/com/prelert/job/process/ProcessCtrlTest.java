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
 ***********************************************************/
package com.prelert.job.process;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataDescription;
import com.prelert.job.JobDetails;

public class ProcessCtrlTest
{
    @Mock
    private Logger m_Logger;

    @Before
    public void setUp()
    {
        System.setProperty(ProcessCtrl.MAX_ANOMALY_RECORDS_PROPERTY, "333");

        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testBuildEnvironment()
    {
        ProcessBuilder pb = new ProcessBuilder();
        ProcessCtrl.buildEnvironment(pb);

        assertEquals(2, pb.environment().size());

        assertEquals(ProcessCtrl.PRELERT_HOME, pb.environment().get(ProcessCtrl.PRELERT_HOME_ENV));
        assertEquals(ProcessCtrl.LIB_PATH, pb.environment().get(ProcessCtrl.LIB_PATH_ENV));
    }

    @Test
    public void testBuildAutoDetectCommand()
    {
        JobDetails job = new JobDetails();
        job.setId("unit-test-job");

        AnalysisConfig ac = new AnalysisConfig();
        ac.setBatchSpan(100l);
        ac.setBucketSpan(120l);
        ac.setLatency(360l);
        ac.setPeriod(20l);
        ac.setSummaryCountFieldName("summaryField");
        job.setAnalysisConfig(ac);

        DataDescription dd = new DataDescription();
        dd.setFieldDelimiter('|');
        dd.setTimeField("tf");
        job.setDataDescription(dd);

        List<String> command = ProcessCtrl.buildAutoDetectCommand(job, m_Logger, null);

        assertEquals(13, command.size());
        assertTrue(command.contains(ProcessCtrl.AUTODETECT_PATH));
        assertTrue(command.contains(ProcessCtrl.BATCH_SPAN_ARG + "100"));
        assertTrue(command.contains(ProcessCtrl.BUCKET_SPAN_ARG + "120"));
        assertTrue(command.contains(ProcessCtrl.LATENCY_ARG + "360"));
        assertTrue(command.contains(ProcessCtrl.PERIOD_ARG + "20"));
        assertTrue(command.contains(ProcessCtrl.SUMMARY_COUNT_FIELD_ARG + "summaryField"));

        assertTrue(command.contains(ProcessCtrl.LENGTH_ENCODED_INPUT_ARG));
        assertTrue(command.contains(ProcessCtrl.MAX_ANOMALY_RECORDS_ARG));

        assertTrue(command.contains(ProcessCtrl.TIME_FIELD_ARG + "tf"));
        assertTrue(command.contains(ProcessCtrl.LOG_ID_ARG + "unit-test-job"));

        int expectedPersistInterval = 10800 + ProcessCtrl.calculateStaggeringInterval(job.getId());
        assertTrue(command.contains(ProcessCtrl.PERSIST_INTERVAL_ARG + expectedPersistInterval));
        int expectedMaxQuantileInterval = 21600 + ProcessCtrl.calculateStaggeringInterval(job.getId());
        assertTrue(command.contains(ProcessCtrl.MAX_QUANTILE_INTERVAL_ARG + expectedMaxQuantileInterval));
        assertTrue(command.contains(ProcessCtrl.PERSIST_URL_BASE_ARG +
                        "http://localhost:" + ProcessCtrl.ES_HTTP_PORT + "/prelertresults-unit-test-job"));
    }

    @Test
    public void testBuildAutoDetectCommand_defaultTimeField()
    {
        JobDetails job = new JobDetails();
        job.setId("unit-test-job");

        List<String> command = ProcessCtrl.buildAutoDetectCommand(job, m_Logger, null);

        assertTrue(command.contains(ProcessCtrl.TIME_FIELD_ARG + "time"));
    }

    @Test
    public void testBuildAutoDetectCommand_givenPersistModelState()
    {
        JobDetails job = new JobDetails();
        job.setId("unit-test-job");

        System.setProperty(ProcessCtrl.DONT_PERSIST_MODEL_STATE, "true");

        int expectedPersistInterval = 10800 + ProcessCtrl.calculateStaggeringInterval(job.getId());

        List<String> command = ProcessCtrl.buildAutoDetectCommand(job, m_Logger, null);
        assertFalse(command.contains(ProcessCtrl.PERSIST_INTERVAL_ARG + expectedPersistInterval));

        System.getProperties().remove(ProcessCtrl.DONT_PERSIST_MODEL_STATE);

        command = ProcessCtrl.buildAutoDetectCommand(job, m_Logger, null);
        assertTrue(command.contains(ProcessCtrl.PERSIST_INTERVAL_ARG + expectedPersistInterval));
    }

    @Test
    public void test_MaxAnomalyDetectorProperty()
    {
        assertTrue(ProcessCtrl.MAX_ANOMALY_RECORDS_ARG.endsWith("333"));
    }

    @Test
    public void testBuildNormaliserCommand() throws IOException
    {
        String jobId = "unit-test-job";

        List<String> command = ProcessCtrl.buildNormaliserCommand(jobId, 300);

        assertEquals(4, command.size());
        assertTrue(command.contains(ProcessCtrl.NORMALIZE_PATH));
        assertTrue(command.contains(ProcessCtrl.BUCKET_SPAN_ARG + "300"));
        assertTrue(command.contains(ProcessCtrl.LOG_ID_ARG + jobId));
        assertTrue(command.contains(ProcessCtrl.LENGTH_ENCODED_INPUT_ARG));
    }


}
