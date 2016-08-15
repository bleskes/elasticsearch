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
import java.io.IOException;
import java.util.Arrays;

import com.prelert.job.JobConfiguration;
import com.prelert.job.condition.Condition;
import com.prelert.job.condition.Operator;
import com.prelert.job.detectionrules.Connective;
import com.prelert.job.detectionrules.DetectionRule;
import com.prelert.job.detectionrules.RuleAction;
import com.prelert.job.detectionrules.RuleCondition;
import com.prelert.job.detectionrules.RuleConditionType;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.rs.data.Pagination;


/**
 * Integration test for the detection rules functionality.
 */
public class DetectionRulesIntegrationTest extends BaseIntegrationTest
{
    static final String FAREQUOTE_NUMERIC_DETECTION_RULES_ID = "farequote-numeric-detection-rules-test";

    /**
     * Creates a new http client call {@linkplain #close()} once finished
     */
    public DetectionRulesIntegrationTest(String baseUrl)
    {
        super(baseUrl, true);
    }

    @Override
    protected void runTest() throws IOException
    {
        // Always delete the test job first in case it is hanging around
        // from a previous run
        m_EngineApiClient.deleteJob(FAREQUOTE_NUMERIC_DETECTION_RULES_ID);

        createFarequoteWithNumericRule();
        File fareQuotePartData = StandardJobs.farequoteDataFile(m_TestDataHome);
        m_EngineApiClient.fileUpload(FAREQUOTE_NUMERIC_DETECTION_RULES_ID, fareQuotePartData, false);
        test(m_EngineApiClient.closeJob(FAREQUOTE_NUMERIC_DETECTION_RULES_ID));
        verifyNoAalRecord();
        testUpdatingRules();

        //==========================
        // Clean up test jobs
        deleteJob(FAREQUOTE_NUMERIC_DETECTION_RULES_ID);
        m_Logger.info("All tests passed Ok");
    }

    private void createFarequoteWithNumericRule() throws IOException
    {
        // Create a rule that will ignore the large AAL anomalies in farequote data

        DetectionRule rule = new DetectionRule();
        rule.setRuleAction(RuleAction.FILTER_RESULTS);
        rule.setConditionsConnective(Connective.OR);
        RuleCondition condition = new RuleCondition();
        condition.setConditionType(RuleConditionType.NUMERICAL_ACTUAL);
        condition.setFieldName("airline");
        condition.setFieldValue("AAL");
        condition.setCondition(new Condition(Operator.LT, "1000"));
        rule.setRuleConditions(Arrays.asList(condition));

        JobConfiguration config = StandardJobs.farequoteJobConfiguration();
        config.setId(FAREQUOTE_NUMERIC_DETECTION_RULES_ID);
        config.getAnalysisConfig().getDetectors().get(0).setDetectorRules(Arrays.asList(rule));
        config.getAnalysisConfig().setInfluencers(Arrays.asList("airline"));

        m_EngineApiClient.createJob(config);
    }

    public void verifyNoAalRecord() throws IOException
    {
        Pagination<AnomalyRecord> records = m_EngineApiClient
                .prepareGetRecords(FAREQUOTE_NUMERIC_DETECTION_RULES_ID).get();
        test(records.getHitCount() < 100);
        test(records.getDocuments().stream().filter(r -> r.getByFieldValue().equals("AAL")).count() == 0);
    }

    public void testUpdatingRules() throws IOException
    {
        test(m_EngineApiClient.getJob(FAREQUOTE_NUMERIC_DETECTION_RULES_ID).getDocument()
                .getAnalysisConfig().getDetectors().get(0).getDetectorRules().isEmpty() == false);
        String update = "{\"detectors\":[{\"index\":0,\"detectorRules\":[]}]}";
        test(m_EngineApiClient.updateJob(FAREQUOTE_NUMERIC_DETECTION_RULES_ID, update));
        test(m_EngineApiClient.getJob(FAREQUOTE_NUMERIC_DETECTION_RULES_ID).getDocument()
                .getAnalysisConfig().getDetectors().get(0).getDetectorRules().isEmpty());
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

        try (DetectionRulesIntegrationTest test = new DetectionRulesIntegrationTest(baseUrl))
        {
            test.runTest();
        }
    }
}
