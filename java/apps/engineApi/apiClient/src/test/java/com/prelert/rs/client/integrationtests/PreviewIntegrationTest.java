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

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import org.apache.http.client.ClientProtocolException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataDescription;
import com.prelert.job.DataDescription.DataFormat;
import com.prelert.job.Detector;
import com.prelert.job.JobConfiguration;
import com.prelert.job.transform.TransformConfig;
import com.prelert.rs.client.EngineApiClient;


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
public class PreviewIntegrationTest implements Closeable
{
    private static final String PREVIEW_WITH_SINGLE_LINE_FORMAT_JOB_ID =
            "preview-with-single-line-format-test";

    private static final Logger LOGGER = Logger.getLogger(PreviewIntegrationTest.class);

    private static final long BUCKET_SPAN = 3600;

    /**
     * The default base Url used in the test
     */
    public static final String API_BASE_URL = "http://localhost:8080/engine/v1";

    private final EngineApiClient m_WebServiceClient;
    private final String m_TestDataHome;
    private final String m_BaseUrl;
    private final String m_JobId;

    /**
     * Creates a new http client call {@linkplain #close()} once finished
     */
    public PreviewIntegrationTest(String testDataHome, String baseUrl, String jobId)
    {
        m_WebServiceClient = new EngineApiClient(baseUrl);
        m_TestDataHome = testDataHome;
        m_BaseUrl = baseUrl;
        m_JobId = jobId;
    }

    @Override
    public void close() throws IOException
    {
        m_WebServiceClient.close();
    }

    public void execute() throws IOException
    {
        // Always delete the test job first in case it is hanging around
        // from a previous run
        deleteJob();

        createSingleLineWithTransformsJob();

        File data = new File(m_TestDataHome +
                "/engine_api_integration_test/preview/input.log");
        String preview = m_WebServiceClient.previewUpload(m_JobId, new FileInputStream(
                data));
        File previewResult = new File(m_TestDataHome
                + "/engine_api_integration_test/preview/previewResult.txt");
        String expectedPreview = new String(Files.readAllBytes(previewResult.toPath()),
                StandardCharsets.UTF_8);

        test(preview.equals(expectedPreview));

        deleteJob();
    }

    private String createSingleLineWithTransformsJob() throws ClientProtocolException,
            IOException
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

        String jobId = m_WebServiceClient.createJob(config);
        if (jobId == null || jobId.isEmpty())
        {
            LOGGER.error("No Job Id returned by create job");
            test(jobId != null && jobId.isEmpty() == false);
        }
        test(m_JobId.equals(jobId));

        return jobId;
    }

    private void deleteJob() throws IOException
    {
        LOGGER.debug("Deleting job " + m_JobId);

        boolean success = m_WebServiceClient.deleteJob(m_JobId);
        if (success == false)
        {
            LOGGER.error("Error deleting job " + m_BaseUrl + "/" + m_JobId);
        }
    }

    /**
     * Throws an IllegalStateException if <code>condition</code> is false.
     *
     * @param condition
     * @throws IllegalStateException
     */
    public static void test(boolean condition)
    throws IllegalStateException
    {
        if (condition == false)
        {
            throw new IllegalStateException();
        }
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
        // configure log4j
        ConsoleAppender console = new ConsoleAppender();
        console.setLayout(new PatternLayout("%d [%p|%c|%C{1}] %m%n"));
        console.setThreshold(Level.INFO);
        console.activateOptions();
        Logger.getRootLogger().addAppender(console);

        String baseUrl = API_BASE_URL;
        if (args.length > 0)
        {
            baseUrl = args[0];
        }

        LOGGER.info("Testing Service at " + baseUrl);

        final String prelertTestDataHome = System.getProperty("prelert.test.data.home");
        if (prelertTestDataHome == null)
        {
            throw new IllegalStateException("Error property prelert.test.data.home is not set");
        }

        PreviewIntegrationTest test = new PreviewIntegrationTest(prelertTestDataHome, baseUrl,
                PREVIEW_WITH_SINGLE_LINE_FORMAT_JOB_ID);
        test.execute();
        test.close();

        LOGGER.info("All tests passed Ok");
    }
}
