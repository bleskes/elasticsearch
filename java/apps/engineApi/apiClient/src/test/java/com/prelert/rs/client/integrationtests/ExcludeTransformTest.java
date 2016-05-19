/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2015     *
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

package com.prelert.rs.client.integrationtests;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataCounts;
import com.prelert.job.DataDescription;
import com.prelert.job.DataDescription.DataFormat;
import com.prelert.job.Detector;
import com.prelert.job.JobConfiguration;
import com.prelert.job.JobDetails;
import com.prelert.job.transform.Condition;
import com.prelert.job.transform.Operator;
import com.prelert.job.transform.TransformConfig;
import com.prelert.rs.data.ApiError;
import com.prelert.rs.data.MultiDataPostResult;
import com.prelert.rs.data.SingleDocument;

/**
 * Tests the regex exclude transform functionality.
 * Two excludes are used this test simply verifies the expected
 * number of records were processed and the expected number filtered.
 */
public class ExcludeTransformTest extends BaseIntegrationTest
{
    private static final String JOB_ID = "exclude-test-job";

    public ExcludeTransformTest(String baseUrl)
    {
        super(baseUrl);
    }

    @Override
    protected void runTest() throws IOException
    {
        m_Logger.info("Running Exclude transform test");


        File dnsDataFile = new File(
                m_TestDataHome + "/engine_api_integration_test/transforms/dns_sample.csv");

        createJob();
        uploadDataAndVerify(dnsDataFile);
        closeJobAndVerify();
    }

    private void createJob() throws IOException
    {
        m_EngineApiClient.deleteJob(JOB_ID);


        AnalysisConfig ac = new AnalysisConfig();
        ac.setBucketSpan(300l);
        Detector d = new Detector();
        d.setFunction("high_info_content");
        d.setFieldName("subdomain");
        d.setOverFieldName("hrd");
        ac.setDetectors(Arrays.asList(d));

        DataDescription dd = new DataDescription();
        dd.setFormat(DataFormat.DELIMITED);
        dd.setFieldDelimiter(',');
        dd.setTimeField("datetime");
        dd.setTimeFormat("dd-MMM-yyyy HH:mm:ss.SSS ");

        List<TransformConfig> transforms = new ArrayList<>();

        TransformConfig domain = new TransformConfig();
        domain.setTransform("domain_split");
        domain.setInputs(Arrays.asList("dns_query"));
        domain.setOutputs(Arrays.asList("subdomain", "hrd"));
        transforms.add(domain);

        TransformConfig exclude1 = new TransformConfig();
        exclude1.setTransform("exclude");
        exclude1.setInputs(Arrays.asList("dns_query"));
        Condition c1 = new Condition();
        c1.setOperator(Operator.MATCH);
        c1.setValue(".*google\\.com\\s*$");
        exclude1.setCondition(c1);
        transforms.add(exclude1);

        TransformConfig exclude2 = new TransformConfig();
        exclude2.setTransform("exclude");
        exclude2.setInputs(Arrays.asList("dns_query"));
        Condition c2 = new Condition();
        c2.setOperator(Operator.MATCH);
        c2.setValue(".*prelert\\.com\\s*$");
        exclude2.setCondition(c2);
        transforms.add(exclude2);


        JobConfiguration config = new JobConfiguration();
        config.setId(JOB_ID);
        config.setAnalysisConfig(ac);
        config.setDataDescription(dd);
        config.setTransforms(transforms);

        m_EngineApiClient.createJob(config);
    }

    public void uploadDataAndVerify(File dataFile)
            throws IOException
    {
        FileInputStream stream = new FileInputStream(dataFile);
        MultiDataPostResult result = m_EngineApiClient.streamingUpload(JOB_ID, stream, false);

        test(result.anErrorOccurred() == false);
        test(result.getResponses().size() == 1);
        ApiError error = result.getResponses().get(0).getError();
        test(error == null);

        verifyCounts(result.getResponses().get(0).getUploadSummary());
    }

    public void closeJobAndVerify() throws IOException
    {
        m_EngineApiClient.closeJob(JOB_ID);

        // check that the usage counts have been written to the job details
        SingleDocument<JobDetails> job = m_EngineApiClient.getJob(JOB_ID);
        test(job.isExists());

        verifyCounts(job.getDocument().getCounts());
    }

    private void verifyCounts(DataCounts counts)
    {
        test(counts.getInputRecordCount() == 999);
        test(counts.getProcessedRecordCount() == 776);
        test(counts.getExcludedRecordCount() == 223);
        test(counts.getInputFieldCount() == 2997);
        test(counts.getProcessedFieldCount() == 1552);
        test(counts.getInvalidDateCount() == 0);
        test(counts.getMissingFieldCount() == 0);
        test(counts.getFailedTransformCount() == 0);
        test(counts.getOutOfOrderTimeStampCount() == 0);
    }


    public static void main(String[] args) throws IOException
    {
        String baseUrl = API_BASE_URL;
        if (args.length > 0)
        {
            baseUrl = args[0];
        }

        try (ExcludeTransformTest transformTest = new ExcludeTransformTest(baseUrl))
        {
            transformTest.m_Logger.info("All tests passed Ok");
        }
    }
}
