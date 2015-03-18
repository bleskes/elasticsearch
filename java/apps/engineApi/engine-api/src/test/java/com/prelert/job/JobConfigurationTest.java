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

import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.prelert.rs.data.ErrorCode;

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
    public void dataDescriptionTest()
    throws JobConfigurationException
    {
        String badFormat = "YYY-mm-UU hh:mm:ssY";
        DataDescription dd = new DataDescription();

        dd.setTimeFormat(badFormat);
        try
        {
            dd.verify();
            // shouldn't get here
            Assert.assertTrue("Invalid format should throw", false);
        }
        catch (JobConfigurationException e)
        {
        }

        String goodFormat = "yyyy.MM.dd G 'at' HH:mm:ss z";
        dd.setTimeFormat(goodFormat);
        Assert.assertTrue("Good time format", dd.verify());
    }


    @Test
    public void analysisConfigTest()
    throws JobConfigurationException
    {
        AnalysisConfig ac = new AnalysisConfig();

        // no detector config
        Detector d = new Detector();
        ac.setDetectors(Arrays.asList(new Detector[] {d}));
        try
        {
            ac.verify();
            Assert.assertTrue(false); // shouldn't get here
        }
        catch (JobConfigurationException e)
        {
        }

        // count works with no fields
        d.setFunction("count");
        ac.verify();

        d.setFunction("distinct_count");
        try
        {
            ac.verify();
            Assert.assertTrue(false); // shouldn't get here
        }
        catch (JobConfigurationException e)
        {
        }

        // should work now
        d.setFieldName("somefield");
        d.setOverFieldName("over");
        ac.verify();

        d.setFunction("info_content");
        ac.verify();

        d.setByFieldName("by");
        try
        {
            ac.verify();
            Assert.assertTrue(false); // shouldn't get here
        }
        catch (JobConfigurationException e)
        {
        }

        d.setByFieldName(null);
        d.setFunction("made_up_function");
        try
        {
            ac.verify();
            Assert.assertTrue(false); // shouldn't get here
        }
        catch (JobConfigurationException e)
        {
        }

        ac.setBatchSpan(-1L);
        try
        {
            ac.verify();
            Assert.assertTrue(false); // shouldn't get here
        }
        catch (JobConfigurationException e)
        {
        }

        ac = new AnalysisConfig();
        ac.setBucketSpan(-1L);
        try
        {
            ac.verify();
            Assert.assertTrue(false); // shouldn't get here
        }
        catch (JobConfigurationException e)
        {
        }

        ac = new AnalysisConfig();
        ac.setPeriod(-1L);
        try
        {
            ac.verify();
            Assert.assertTrue(false); // shouldn't get here
        }
        catch (JobConfigurationException e)
        {
        }
    }

    @Test
    public void analysisLimitsTest()
    throws JobConfigurationException
    {
        AnalysisLimits ao = new AnalysisLimits(-1);
        try
        {
            ao.verify();
            Assert.assertTrue(false); // shouldn't get here
        }
        catch (JobConfigurationException e)
        {
        }

        ao = new AnalysisLimits(300);
        try
        {
            ao.verify();
        }
        catch (JobConfigurationException e)
        {
            Assert.assertTrue(false); // shouldn't get here
        }
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

        jc.setAnalysisLimits(new AnalysisLimits(-1));
        try
        {
            jc.verify();
            Assert.assertTrue(false); // shouldn't get here
        }
        catch (JobConfigurationException e)
        {
        }

        jc.setAnalysisLimits(new AnalysisLimits(1000));
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
        ac.setDetectors(Arrays.asList(new Detector[] {d1, d2}));

        List<String> analysisFields = ac.analysisFields();
        Assert.assertTrue(analysisFields.size() == 4);

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
    public void test_checkTransformOutputIsInAnalysisFields() throws JobConfigurationException
    {
        JobConfiguration jc = new JobConfiguration();

        Detector d1 = new Detector();
        d1.setFieldName("domain");
        d1.setOverFieldName("client");
        d1.setFunction("info_content");

        Detector d2 = new Detector();
        d2.setFieldName("metric");

        AnalysisConfig ac = new AnalysisConfig();
        ac.setDetectors(Arrays.asList(new Detector[] {d1, d2}));

        DataDescription dc = new DataDescription();

        TransformConfig tc = new TransformConfig();
        tc.setTransform(TransformType.Names.DOMAIN_LOOKUP_NAME);
        tc.setInputs(Arrays.asList("dns"));

        jc.setTransforms(Arrays.asList(tc));
        jc.setAnalysisConfig(ac);
        jc.setDataDescription(dc);

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

        d1.setFieldName(TransformType.DOMAIN_LOOKUP.defaultOutputNames().get(0));
        assertTrue(jc.verify());
    }

}
