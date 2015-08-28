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
package com.prelert.job.config.verification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.AnalysisLimits;
import com.prelert.job.DataDescription;
import com.prelert.job.DataDescription.DataFormat;
import com.prelert.job.Detector;
import com.prelert.job.JobConfiguration;
import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.transform.Condition;
import com.prelert.job.transform.Operator;
import com.prelert.job.transform.TransformConfig;
import com.prelert.job.transform.TransformType;


public class JobConfigurationVerifierTest
{
    @Rule
    public ExpectedException m_ExpectedException = ExpectedException.none();

    @Test
    public void testCheckValidId_IdTooLong() throws JobConfigurationException
    {
        JobConfiguration conf = new JobConfiguration();
        conf.setReferenceJobId("ref");
        conf.setId("averyveryveryaveryveryveryaveryveryveryaveryveryveryaveryveryveryaveryveryverylongid");

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.JOB_ID_TOO_LONG));

        JobConfigurationVerifier.verify(conf);
    }

    @Test
    public void testCheckValidId_ProhibitedChars() throws JobConfigurationException
    {
        JobConfiguration conf = new JobConfiguration();
        conf.setReferenceJobId("ref");
        conf.setId("?");

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.PROHIBITIED_CHARACTER_IN_JOB_ID));

        JobConfigurationVerifier.verify(conf);
    }

    @Test
    public void testCheckValidId_UpperCaseChars() throws JobConfigurationException
    {
        JobConfiguration conf = new JobConfiguration();
        conf.setReferenceJobId("ref");

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.PROHIBITIED_CHARACTER_IN_JOB_ID));

        conf.setId("UPPERCASE");
        JobConfigurationVerifier.verify(conf);
    }

    @Test
    public void testCheckValidId_ControlChars() throws JobConfigurationException
    {
        JobConfiguration conf = new JobConfiguration();
        conf.setReferenceJobId("ref");

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.PROHIBITIED_CHARACTER_IN_JOB_ID));

        conf.setId("two\nlines");
        JobConfigurationVerifier.verify(conf);
    }

    @Test
    public void jobConfigurationTest()
    throws JobConfigurationException
    {
        JobConfiguration jc = new JobConfiguration();
        try
        {
            JobConfigurationVerifier.verify(jc);
            Assert.assertTrue(false); // shouldn't get here
        }
        catch (JobConfigurationException e)
        {
            Assert.assertTrue(e.getErrorCode() == ErrorCodes.INCOMPLETE_CONFIGURATION);
        }

        jc.setReferenceJobId("ref_id");
        JobConfigurationVerifier.verify(jc);

        jc.setId("bad id with spaces");
        try
        {
            JobConfigurationVerifier.verify(jc);
            Assert.assertTrue(false); // shouldn't get here
        }
        catch (JobConfigurationException e)
        {
            Assert.assertTrue(e.getErrorCode() == ErrorCodes.PROHIBITIED_CHARACTER_IN_JOB_ID);
        }


        jc.setId("bad_id_with_UPPERCASE_chars");
        try
        {
            JobConfigurationVerifier.verify(jc);
            Assert.assertTrue(false); // shouldn't get here
        }
        catch (JobConfigurationException e)
        {
            Assert.assertTrue(e.getErrorCode() == ErrorCodes.PROHIBITIED_CHARACTER_IN_JOB_ID);
        }

        jc.setId("a very  very very very very very very very very very very very very very very very very very very very long id");
        try
        {
            JobConfigurationVerifier.verify(jc);
            Assert.assertTrue(false); // shouldn't get here
        }
        catch (JobConfigurationException e)
        {
            Assert.assertTrue(e.getErrorCode() == ErrorCodes.JOB_ID_TOO_LONG);
        }


        jc.setId(null);
        jc.setReferenceJobId(null);
        jc.setAnalysisConfig(new AnalysisConfig());
        try
        {
            JobConfigurationVerifier.verify(jc);
            Assert.assertTrue(false); // shouldn't get here
        }
        catch (JobConfigurationException e)
        {
            Assert.assertTrue(e.getErrorCode() == ErrorCodes.INCOMPLETE_CONFIGURATION);
        }



        AnalysisConfig ac = new AnalysisConfig();
        Detector d = new Detector();
        d.setFieldName("a");
        d.setByFieldName("b");
        d.setFunction("max");
        ac.setDetectors(Arrays.asList(new Detector[] {d}));

        jc.setAnalysisConfig(ac);
        JobConfigurationVerifier.verify(jc); // ok

        jc.setAnalysisLimits(new AnalysisLimits(-1, null));
        try
        {
            JobConfigurationVerifier.verify(jc);
        }
        catch (JobConfigurationException e)
        {
            Assert.assertTrue(false); // shouldn't get here
        }

        jc.setAnalysisLimits(new AnalysisLimits(1000, 4L));
        JobConfigurationVerifier.verify(jc);

        DataDescription dc = new DataDescription();
        dc.setTimeFormat("YYY_KKKKajsatp*");

        jc.setDataDescription(dc);
        try
        {
            JobConfigurationVerifier.verify(jc);
            Assert.assertTrue(false); // shouldn't get here
        }
        catch (JobConfigurationException e)
        {
            Assert.assertEquals(ErrorCodes.INVALID_DATE_FORMAT, e.getErrorCode());
        }


        dc = new DataDescription();
        jc.setDataDescription(dc);

        jc.setTimeout(-1L);
        try
        {
            JobConfigurationVerifier.verify(jc);
            Assert.assertTrue(false); // shouldn't get here
        }
        catch (JobConfigurationException e)
        {
            Assert.assertEquals(ErrorCodes.INVALID_VALUE, e.getErrorCode());
        }

        jc.setTimeout(300L);
        JobConfigurationVerifier.verify(jc);

    }


    @Test
    public void testCheckTransformOutputIsUsed_throws() throws JobConfigurationException
    {
        JobConfiguration jc = buildJobConfigurationNoTransforms();

        TransformConfig tc = new TransformConfig();
        tc.setTransform(TransformType.Names.DOMAIN_SPLIT_NAME);
        tc.setInputs(Arrays.asList("dns"));

        jc.setTransforms(Arrays.asList(tc));

        try
        {
            JobConfigurationVerifier.verify(jc);
            fail("verify should throw"); // shouldn't get here
        }
        catch (JobConfigurationException e)
        {
            assertTrue(e instanceof JobConfigurationException);
            assertEquals(ErrorCodes.TRANSFORM_OUTPUTS_UNUSED, e.getErrorCode());
        }


        jc.getAnalysisConfig().getDetectors().get(0).setFieldName(TransformType.DOMAIN_SPLIT.defaultOutputNames().get(0));
        assertTrue(JobConfigurationVerifier.verify(jc));
    }

    @Test
    public void testCheckTransformOutputIsUsed_outputIsSummaryCountField() throws JobConfigurationException
    {
        JobConfiguration jc = buildJobConfigurationNoTransforms();
        jc.getAnalysisConfig().setSummaryCountFieldName("summaryCountField");

        TransformConfig tc = new TransformConfig();
        tc.setTransform(TransformType.Names.DOMAIN_SPLIT_NAME);
        tc.setInputs(Arrays.asList("dns"));
        tc.setOutputs(Arrays.asList("summaryCountField"));

        jc.setTransforms(Arrays.asList(tc));

        try
        {
            JobConfigurationVerifier.verify(jc);
            fail("verify should throw"); // shouldn't get here
        }
        catch (JobConfigurationException e)
        {
            assertTrue(e instanceof JobConfigurationException);
            assertEquals(ErrorCodes.DUPLICATED_TRANSFORM_OUTPUT_NAME, e.getErrorCode());
        }

        jc.getAnalysisConfig().getDetectors().get(0).setFieldName(TransformType.DOMAIN_SPLIT.defaultOutputNames().get(0));
        tc.setOutputs(TransformType.DOMAIN_SPLIT.defaultOutputNames());
        assertTrue(JobConfigurationVerifier.verify(jc));
    }

    @Test
    public void testCheckTransformOutputIsUsed_transformHasNoOutput()
    throws JobConfigurationException
    {
        JobConfiguration jc = buildJobConfigurationNoTransforms();

        // The exclude filter has no output
        TransformConfig tc = new TransformConfig();
        tc.setTransform(TransformType.Names.EXCLUDE_NAME);
        tc.setCondition(new Condition(Operator.MATCH, "whitelisted_host"));
        tc.setInputs(Arrays.asList("dns"));

        jc.setTransforms(Arrays.asList(tc));

        JobConfigurationVerifier.verify(jc);
    }

    @Test
    public void testVerify_GivenDataFormatIsSingleLineAndNullTransforms()
            throws JobConfigurationException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage(
                "When the data format is SINGLE_LINE, transforms are required.");
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(
                ErrorCodes.DATA_FORMAT_IS_SINGLE_LINE_BUT_NO_TRANSFORMS));

        JobConfiguration config = buildJobConfigurationNoTransforms();
        config.getDataDescription().setFormat(DataFormat.SINGLE_LINE);

        JobConfigurationVerifier.verify(config);
    }

    @Test
    public void testVerify_GivenDataFormatIsSingleLineAndEmptyTransforms()
            throws JobConfigurationException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage(
                "When the data format is SINGLE_LINE, transforms are required.");
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(
                ErrorCodes.DATA_FORMAT_IS_SINGLE_LINE_BUT_NO_TRANSFORMS));

        JobConfiguration config = buildJobConfigurationNoTransforms();
        config.setTransforms(new ArrayList<>());
        config.getDataDescription().setFormat(DataFormat.SINGLE_LINE);

        JobConfigurationVerifier.verify(config);
    }

    @Test
    public void testVerify_GivenDataFormatIsSingleLineAndNonEmptyTransforms()
            throws JobConfigurationException
    {
        ArrayList<TransformConfig> transforms = new ArrayList<>();
        TransformConfig transform = new TransformConfig();
        transform.setTransform("trim");
        transform.setInputs(Arrays.asList("raw"));
        transform.setOutputs(Arrays.asList("time"));
        transforms.add(transform);
        JobConfiguration config = buildJobConfigurationNoTransforms();
        config.setTransforms(transforms);
        config.getDataDescription().setFormat(DataFormat.SINGLE_LINE);

        JobConfigurationVerifier.verify(config);
    }

    private JobConfiguration buildJobConfigurationNoTransforms()
    {
        JobConfiguration jc = new JobConfiguration();

        Detector d1 = new Detector();
        d1.setFieldName("domain");
        d1.setOverFieldName("client");
        d1.setFunction("info_content");

        Detector d2 = new Detector();
        d2.setFunction("count");

        AnalysisConfig ac = new AnalysisConfig();
        ac.setDetectors(Arrays.asList(new Detector[] {d1, d2}));

        DataDescription dc = new DataDescription();

        jc.setAnalysisConfig(ac);
        jc.setDataDescription(dc);

        return jc;
    }


}
