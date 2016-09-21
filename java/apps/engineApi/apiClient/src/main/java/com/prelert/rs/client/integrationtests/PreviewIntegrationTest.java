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

package com.prelert.rs.client.integrationtests;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataDescription;
import com.prelert.job.DataDescription.DataFormat;
import com.prelert.job.Detector;
import com.prelert.job.JobConfiguration;
import com.prelert.job.transform.TransformConfig;


/**
 * Integration test for the preview functionality.
 *
 * <br>
 * The system property 'prelert.test.data.home' must be set and point
 * to a directory containing the file:
 * <ol>
 * <li>
 * The first test uses /engine_api_integration_test/preview/input.log as input and
 * /engine_api_integration_test/preview/previewResult.txt as expected output.
 * It also tests {@link DataFormat#SINGLE_LINE} and {@link RegexExtract}.
 * </li>
 * </ol>
 */
public class PreviewIntegrationTest extends BaseIntegrationTest
{
    private static final String PREVIEW_WITH_SINGLE_LINE_FORMAT_JOB_ID =
            "preview-with-single-line-format-test";

    private static final long BUCKET_SPAN = 3600;

    private final String m_JobId;

    /**
     * Creates a new http client call {@linkplain #close()} once finished
     */
    public PreviewIntegrationTest(String baseUrl, String jobId)
    {
        super(baseUrl, true);
        m_JobId = jobId;
    }

    @Override
    public void close() throws IOException
    {
        m_EngineApiClient.close();
    }

    @Override
    public void runTest() throws IOException
    {
        // Always delete the test job first in case it is hanging around
        // from a previous run
        deleteJob(m_JobId);

        createSingleLineWithTransformsJob();

        File data = new File(m_TestDataHome +
                "/engine_api_integration_test/preview/input.log");
        String preview = m_EngineApiClient.previewUpload(m_JobId, new FileInputStream(
                data));
        File previewResult = new File(m_TestDataHome
                + "/engine_api_integration_test/preview/previewResult.txt");
        String expectedPreview = new String(Files.readAllBytes(previewResult.toPath()),
                StandardCharsets.UTF_8);

        test(preview.equals(expectedPreview));

        deleteJob(m_JobId);
    }

    private String createSingleLineWithTransformsJob() throws IOException
    {
        Detector d = new Detector();
        d.setFunction("count");
        d.setByFieldName("message");

        AnalysisConfig ac = new AnalysisConfig();
        ac.setBucketSpan(BUCKET_SPAN);
        ac.setDetectors(Arrays.asList(d));

        DataDescription dd = new DataDescription();
        dd.setFormat(DataFormat.SINGLE_LINE);
        dd.setTimeField("time");
        dd.setTimeFormat("yyyy-MM-dd HH:mm:ss,SSS XXX");

        TransformConfig transform = new TransformConfig();
        transform.setTransform("extract");
        transform.setArguments(Arrays.asList("(.{30}) (.*)"));
        transform.setInputs(Arrays.asList("raw"));
        transform.setOutputs(Arrays.asList("time", "message"));

        JobConfiguration config = new JobConfiguration(ac);
        config.setDescription("Preview with single line format test");
        config.setId(m_JobId);
        config.setDataDescription(dd);
        config.setTransforms(Arrays.asList(transform));

        String jobId = m_EngineApiClient.createJob(config);
        if (jobId == null || jobId.isEmpty())
        {
            m_Logger.error("No Job Id returned by create job");
            test(jobId != null && jobId.isEmpty() == false);
        }
        test(m_JobId.equals(jobId));

        return jobId;
    }


    /**
     * The program takes one argument which is the base Url of the RESTful API.
     * If no arguments are given then {@value #API_BASE_URL} is used.
     *
     * @param args
     * @throws IOException
     * @throws InterruptedException
     */
    public static void main(String[] args)
    throws IOException, InterruptedException
    {
        String baseUrl = API_BASE_URL;
        if (args.length > 0)
        {
            baseUrl = args[0];
        }

        try (PreviewIntegrationTest test = new PreviewIntegrationTest(baseUrl,
                PREVIEW_WITH_SINGLE_LINE_FORMAT_JOB_ID))
        {
            test.runTest();
            test.m_Logger.info("All tests passed Ok");
        }

    }
}
