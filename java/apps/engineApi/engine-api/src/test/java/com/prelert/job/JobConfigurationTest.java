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

package com.prelert.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.prelert.job.DataDescription.DataFormat;
import com.prelert.job.exceptions.JobConfigurationException;
import com.prelert.job.transform.TransformConfig;
import com.prelert.job.transform.TransformType;
import com.prelert.job.transform.condition.Condition;
import com.prelert.job.transform.condition.Operator;
import com.prelert.job.transform.exceptions.TransformConfigurationException;
import com.prelert.rs.data.ErrorCode;
import com.prelert.rs.data.ErrorCodeMatcher;

/**
 * Test the {@link JobConfiguration#verify()} function for
 * basic errors in the configuration
 */
public class JobConfigurationTest
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
                ErrorCodeMatcher.hasErrorCode(ErrorCode.JOB_ID_TOO_LONG));

        conf.verify();
    }

    @Test
    public void testCheckValidId_ProhibitedChars() throws JobConfigurationException
    {
        JobConfiguration conf = new JobConfiguration();
        conf.setReferenceJobId("ref");
        conf.setId("?");

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCode.PROHIBITIED_CHARACTER_IN_JOB_ID));

        conf.verify();
    }

    @Test
    public void testCheckValidId_UpperCaseChars() throws JobConfigurationException
    {
        JobConfiguration conf = new JobConfiguration();
        conf.setReferenceJobId("ref");

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCode.PROHIBITIED_CHARACTER_IN_JOB_ID));

        conf.setId("UPPERCASE");
        conf.verify();
    }

    @Test
    public void testCheckValidId_ControlChars() throws JobConfigurationException
    {
        JobConfiguration conf = new JobConfiguration();
        conf.setReferenceJobId("ref");

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCode.PROHIBITIED_CHARACTER_IN_JOB_ID));

        conf.setId("two\nlines");
        conf.verify();
    }

    @Test
    public void jobConfigurationTest()
    throws JobConfigurationException
    {
        JobConfiguration jc = new JobConfiguration();
        try
        {
            jc.verify();
            Assert.assertTrue(false); // shouldn't get here
        }
        catch (JobConfigurationException e)
        {
            Assert.assertTrue(e.getErrorCode() == ErrorCode.INCOMPLETE_CONFIGURATION);
        }

        jc.setReferenceJobId("ref_id");
        jc.verify();

        jc.setId("bad id with spaces");
        try
        {
            jc.verify();
            Assert.assertTrue(false); // shouldn't get here
        }
        catch (JobConfigurationException e)
        {
            Assert.assertTrue(e.getErrorCode() == ErrorCode.PROHIBITIED_CHARACTER_IN_JOB_ID);
        }


        jc.setId("bad_id_with_UPPERCASE_chars");
        try
        {
            jc.verify();
            Assert.assertTrue(false); // shouldn't get here
        }
        catch (JobConfigurationException e)
        {
            Assert.assertTrue(e.getErrorCode() == ErrorCode.PROHIBITIED_CHARACTER_IN_JOB_ID);
        }

        jc.setId("a very  very very very very very very very very very very very very very very very very very very very long id");
        try
        {
            jc.verify();
            Assert.assertTrue(false); // shouldn't get here
        }
        catch (JobConfigurationException e)
        {
            Assert.assertTrue(e.getErrorCode() == ErrorCode.JOB_ID_TOO_LONG);
        }


        jc.setId(null);
        jc.setReferenceJobId(null);
        jc.setAnalysisConfig(new AnalysisConfig());
        try
        {
            jc.verify();
            Assert.assertTrue(false); // shouldn't get here
        }
        catch (JobConfigurationException e)
        {
            Assert.assertTrue(e.getErrorCode() == ErrorCode.INCOMPLETE_CONFIGURATION);
        }



        AnalysisConfig ac = new AnalysisConfig();
        Detector d = new Detector();
        d.setFieldName("a");
        d.setByFieldName("b");
        d.setFunction("max");
        ac.setDetectors(Arrays.asList(new Detector[] {d}));

        jc.setAnalysisConfig(ac);
        jc.verify(); // ok

        jc.setAnalysisLimits(new AnalysisLimits(-1, null));
        try
        {
            jc.verify();
            Assert.assertTrue(false); // shouldn't get here
        }
        catch (JobConfigurationException e)
        {
            Assert.assertEquals(ErrorCode.INVALID_VALUE, e.getErrorCode());
        }

        jc.setAnalysisLimits(new AnalysisLimits(1000, 4L));
        jc.verify();

        DataDescription dc = new DataDescription();
        dc.setTimeFormat("YYY_KKKKajsatp*");

        jc.setDataDescription(dc);
        try
        {
            jc.verify();
            Assert.assertTrue(false); // shouldn't get here
        }
        catch (JobConfigurationException e)
        {
            Assert.assertEquals(ErrorCode.INVALID_DATE_FORMAT, e.getErrorCode());
        }


        dc = new DataDescription();
        jc.setDataDescription(dc);

        jc.setTimeout(-1L);
        try
        {
            jc.verify();
            Assert.assertTrue(false); // shouldn't get here
        }
        catch (JobConfigurationException e)
        {
            Assert.assertEquals(ErrorCode.INVALID_VALUE, e.getErrorCode());
        }

        jc.setTimeout(300L);
        jc.verify();

    }


    /**
     * Test the {@link AnalysisConfig#analysisFields()} method which produces
     * a list of analysis fields from the detectors
     */
    @Test
    public void testAnalysisConfigRequiredFields()
    {
        Detector d1 = new Detector();
        d1.setFieldName("field");
        d1.setByFieldName("by");
        d1.setFunction("max");

        Detector d2 = new Detector();
        d2.setFieldName("field2");
        d2.setOverFieldName("over");

        AnalysisConfig ac = new AnalysisConfig();
        ac.setSummaryCountFieldName("agg");
        ac.setDetectors(Arrays.asList(new Detector[] {d1, d2}));

        List<String> analysisFields = ac.analysisFields();
        Assert.assertTrue(analysisFields.size() == 5);

        Assert.assertTrue(analysisFields.contains("agg"));
        Assert.assertTrue(analysisFields.contains("field"));
        Assert.assertTrue(analysisFields.contains("by"));
        Assert.assertTrue(analysisFields.contains("field2"));
        Assert.assertTrue(analysisFields.contains("over"));

        Assert.assertFalse(analysisFields.contains("max"));
        Assert.assertFalse(analysisFields.contains(""));
        Assert.assertFalse(analysisFields.contains(null));

        Detector d3 = new Detector();
        d3.setFunction("count");
        d3.setByFieldName("by2");
        d3.setPartitionFieldName("partition");

        ac = new AnalysisConfig();
        ac.setDetectors(Arrays.asList(new Detector[] {d1, d2, d3}));

        analysisFields = ac.analysisFields();
        Assert.assertTrue(analysisFields.size() == 6);

        Assert.assertTrue(analysisFields.contains("partition"));
        Assert.assertTrue(analysisFields.contains("field"));
        Assert.assertTrue(analysisFields.contains("by"));
        Assert.assertTrue(analysisFields.contains("by2"));
        Assert.assertTrue(analysisFields.contains("field2"));
        Assert.assertTrue(analysisFields.contains("over"));

        Assert.assertFalse(analysisFields.contains("count"));
        Assert.assertFalse(analysisFields.contains("max"));
        Assert.assertFalse(analysisFields.contains(""));
        Assert.assertFalse(analysisFields.contains(null));
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
            jc.verify();
            fail("verify should throw"); // shouldn't get here
        }
        catch (JobConfigurationException e)
        {
            assertTrue(e instanceof TransformConfigurationException);
            assertEquals(e.getErrorCode(), ErrorCode.TRANSFORM_OUTPUTS_UNUSED);
        }


        jc.getAnalysisConfig().getDetectors().get(0).setFieldName(TransformType.DOMAIN_SPLIT.defaultOutputNames().get(0));
        assertTrue(jc.verify());
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
            jc.verify();
            fail("verify should throw"); // shouldn't get here
        }
        catch (JobConfigurationException e)
        {
            assertTrue(e instanceof TransformConfigurationException);
            assertEquals(e.getErrorCode(), ErrorCode.DUPLICATED_TRANSFORM_OUTPUT_NAME);
        }

        jc.getAnalysisConfig().getDetectors().get(0).setFieldName(TransformType.DOMAIN_SPLIT.defaultOutputNames().get(0));
        tc.setOutputs(TransformType.DOMAIN_SPLIT.defaultOutputNames());
        assertTrue(jc.verify());
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

        jc.verify();
    }

    @Test
    public void testVerify_GivenDataFormatIsSingleLineAndNullTransforms()
            throws JobConfigurationException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage(
                "When the data format is SINGLE_LINE, transforms are required.");
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(
                ErrorCode.DATA_FORMAT_IS_SINGLE_LINE_BUT_NO_TRANSFORMS));

        JobConfiguration config = buildJobConfigurationNoTransforms();
        config.getDataDescription().setFormat(DataFormat.SINGLE_LINE);

        config.verify();
    }

    @Test
    public void testVerify_GivenDataFormatIsSingleLineAndEmptyTransforms()
            throws JobConfigurationException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage(
                "When the data format is SINGLE_LINE, transforms are required.");
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(
                ErrorCode.DATA_FORMAT_IS_SINGLE_LINE_BUT_NO_TRANSFORMS));

        JobConfiguration config = buildJobConfigurationNoTransforms();
        config.setTransforms(new ArrayList<>());
        config.getDataDescription().setFormat(DataFormat.SINGLE_LINE);

        config.verify();
    }

    @Test
    public void testVerify_GivenDataFormatIsSingleLineAndNonEmptyTransforms()
            throws JobConfigurationException
    {
        ArrayList<TransformConfig> transforms = new ArrayList<>();
        TransformConfig transform = new TransformConfig();
        transform.setTransform("extract");
        transform.setArguments(Arrays.asList(""));
        transform.setInputs(Arrays.asList("raw"));
        transform.setOutputs(Arrays.asList("time"));
        transforms.add(transform);
        JobConfiguration config = buildJobConfigurationNoTransforms();
        config.setTransforms(transforms);
        config.getDataDescription().setFormat(DataFormat.SINGLE_LINE);

        config.verify();
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

        TransformConfig tc = new TransformConfig();
        tc.setTransform(TransformType.Names.DOMAIN_SPLIT_NAME);
        tc.setInputs(Arrays.asList("dns"));

        jc.setAnalysisConfig(ac);
        jc.setDataDescription(dc);

        return jc;
    }


}
