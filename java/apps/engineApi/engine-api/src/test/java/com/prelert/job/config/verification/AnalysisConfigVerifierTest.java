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
package com.prelert.job.config.verification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.Detector;
import com.prelert.job.errorcodes.ErrorCodes;

import junit.framework.Assert;

/**
 * Tests the configured fields in the analysis are correct
 * {@linkplain AnalysisConfig#analysisFields()}
 * {@linkplain AnalysisConfig#fields()}
 * {@linkplain AnalysisConfig#byFields()}
 * {@linkplain AnalysisConfig#overFields()}
 * {@linkplain AnalysisConfig#partitionFields()}
 */
public class AnalysisConfigVerifierTest
{
    @Rule
    public ExpectedException m_ExpectedException = ExpectedException.none();


    @Test
    public void testVerify_throws()
    throws JobConfigurationException
    {
        AnalysisConfig ac = new AnalysisConfig();

        // no detector config
        Detector d = new Detector();
        ac.setDetectors(Arrays.asList(new Detector[] {d}));
        try
        {
            AnalysisConfigVerifier.verify(ac);
            Assert.assertTrue(false); // shouldn't get here
        }
        catch (JobConfigurationException e)
        {
            assertEquals(ErrorCodes.INVALID_FIELD_SELECTION, e.getErrorCode());
        }

        // count works with no fields
        d.setFunction("count");
        AnalysisConfigVerifier.verify(ac);

        d.setFunction("distinct_count");
        try
        {
            AnalysisConfigVerifier.verify(ac);
            Assert.assertTrue(false); // shouldn't get here
        }
        catch (JobConfigurationException e)
        {
            assertEquals(ErrorCodes.INVALID_FIELD_SELECTION, e.getErrorCode());
        }

        // should work now
        d.setFieldName("somefield");
        d.setOverFieldName("over");
        AnalysisConfigVerifier.verify(ac);

        d.setFunction("info_content");
        AnalysisConfigVerifier.verify(ac);

        d.setByFieldName("by");
        AnalysisConfigVerifier.verify(ac);

        d.setByFieldName(null);
        d.setFunction("made_up_function");
        try
        {
            AnalysisConfigVerifier.verify(ac);
            Assert.assertTrue(false); // shouldn't get here
        }
        catch (JobConfigurationException e)
        {
            assertEquals(ErrorCodes.UNKNOWN_FUNCTION, e.getErrorCode());
        }

        ac.setBatchSpan(-1L);
        try
        {
            AnalysisConfigVerifier.verify(ac);
            Assert.assertTrue(false); // shouldn't get here
        }
        catch (JobConfigurationException e)
        {
            assertEquals(ErrorCodes.INVALID_VALUE, e.getErrorCode());
        }

        ac = new AnalysisConfig();
        ac.setBucketSpan(-1L);
        try
        {
            AnalysisConfigVerifier.verify(ac);
            Assert.assertTrue(false); // shouldn't get here
        }
        catch (JobConfigurationException e)
        {
            assertEquals(ErrorCodes.INVALID_VALUE, e.getErrorCode());
        }

        ac = new AnalysisConfig();
        ac.setPeriod(-1L);
        try
        {
            AnalysisConfigVerifier.verify(ac);
            Assert.assertTrue(false); // shouldn't get here
        }
        catch (JobConfigurationException e)
        {
            assertEquals(ErrorCodes.INVALID_VALUE, e.getErrorCode());
        }

        ac = new AnalysisConfig();
        ac.setLatency(-1L);
        try
        {
            AnalysisConfigVerifier.verify(ac);
            Assert.assertTrue(false); // shouldn't get here
        }
        catch (JobConfigurationException e)
        {
            assertEquals(ErrorCodes.INVALID_VALUE, e.getErrorCode());
        }
    }

    @Test
    public void testVerify_GivenNegativeBucketSpan() throws JobConfigurationException
    {
        AnalysisConfig analysisConfig = new AnalysisConfig();
        analysisConfig.setBucketSpan(-1L);
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("bucketSpan cannot be less than 0. Value = -1");

        AnalysisConfigVerifier.verify(analysisConfig);
    }

    @Test
    public void testVerify_GivenNegativeBatchSpan() throws JobConfigurationException
    {
        AnalysisConfig analysisConfig = new AnalysisConfig();
        analysisConfig.setBatchSpan(-1L);
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("batchSpan cannot be less than 0. Value = -1");

        AnalysisConfigVerifier.verify(analysisConfig);
    }

    @Test
    public void testVerify_GivenNegativeLatency() throws JobConfigurationException
    {
        AnalysisConfig analysisConfig = new AnalysisConfig();
        analysisConfig.setLatency(-1L);
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("latency cannot be less than 0. Value = -1");

        AnalysisConfigVerifier.verify(analysisConfig);
    }

    @Test
    public void testVerify_GivenNegativePeriod() throws JobConfigurationException
    {
        AnalysisConfig analysisConfig = new AnalysisConfig();
        analysisConfig.setPeriod(-1L);
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("period cannot be less than 0. Value = -1");

        AnalysisConfigVerifier.verify(analysisConfig);
    }

    @Test
    public void testVerify_GivenDefaultConfig_ShouldBeInvalidDueToNoDetectors() throws JobConfigurationException
    {
        AnalysisConfig analysisConfig = new AnalysisConfig();
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("No detectors configured");

        AnalysisConfigVerifier.verify(analysisConfig);
    }

    @Test
    public void testVerify_GivenValidConfig() throws JobConfigurationException
    {
        AnalysisConfig analysisConfig = new AnalysisConfig();
        analysisConfig.setBucketSpan(3600L);
        analysisConfig.setBatchSpan(0L);
        analysisConfig.setLatency(0L);
        analysisConfig.setPeriod(0L);
        List<Detector> detectors = new ArrayList<>();
        Detector detector = new Detector();
        detector.setFieldName("count");
        detectors.add(detector);
        analysisConfig.setDetectors(detectors);

        assertTrue(AnalysisConfigVerifier.verify(analysisConfig));
    }

    @Test
    public void testVerify_OverlappingBuckets() throws JobConfigurationException
    {
        AnalysisConfig analysisConfig;
        List<Detector> detectors;
        Detector detector;

        /* Uncomment this when overlappingBuckets turned on by default
         *
        // Test overlappingBuckets unset
        analysisConfig = new AnalysisConfig();
        analysisConfig.setBucketSpan(5000L);
        analysisConfig.setBatchSpan(0L);
        detectors = new ArrayList<>();
        detector = new Detector();
        detector.setFunction("count");
        detectors.add(detector);
        detector = new Detector();
        detector.setFunction("mean");
        detector.setFieldName("value");
        detectors.add(detector);
        analysisConfig.setDetectors(detectors);
        assertTrue(AnalysisConfigVerifier.verify(analysisConfig));
        assertTrue(analysisConfig.getOverlappingBuckets());

        // Test overlappingBuckets unset
        analysisConfig = new AnalysisConfig();
        analysisConfig.setBucketSpan(5000L);
        analysisConfig.setBatchSpan(0L);
        detectors = new ArrayList<>();
        detector = new Detector();
        detector.setFunction("count");
        detectors.add(detector);
        detector = new Detector();
        detector.setFunction("rare");
        detector.setByFieldName("value");
        detectors.add(detector);
        analysisConfig.setDetectors(detectors);
        assertTrue(AnalysisConfigVerifier.verify(analysisConfig));
        assertFalse(analysisConfig.getOverlappingBuckets());

        // Test overlappingBuckets unset
        analysisConfig = new AnalysisConfig();
        analysisConfig.setBucketSpan(5000L);
        analysisConfig.setBatchSpan(0L);
        detectors = new ArrayList<>();
        detector = new Detector();
        detector.setFunction("count");
        detectors.add(detector);
        detector = new Detector();
        detector.setFunction("min");
        detector.setFieldName("value");
        detectors.add(detector);
        detector = new Detector();
        detector.setFunction("max");
        detector.setFieldName("value");
        detectors.add(detector);
        analysisConfig.setDetectors(detectors);
        assertTrue(AnalysisConfigVerifier.verify(analysisConfig));
        assertFalse(analysisConfig.getOverlappingBuckets());
        */

        // Test overlappingBuckets set
        analysisConfig = new AnalysisConfig();
        analysisConfig.setBucketSpan(5000L);
        analysisConfig.setBatchSpan(0L);
        detectors = new ArrayList<>();
        detector = new Detector();
        detector.setFunction("count");
        detectors.add(detector);
        detector = new Detector();
        detector.setFunction("rare");
        detector.setByFieldName("value");
        detectors.add(detector);
        analysisConfig.setOverlappingBuckets(false);
        analysisConfig.setDetectors(detectors);
        assertTrue(AnalysisConfigVerifier.verify(analysisConfig));
        assertFalse(analysisConfig.getOverlappingBuckets());

        // Test overlappingBuckets set
        analysisConfig = new AnalysisConfig();
        analysisConfig.setBucketSpan(5000L);
        analysisConfig.setBatchSpan(0L);
        analysisConfig.setOverlappingBuckets(true);
        detectors = new ArrayList<>();
        detector = new Detector();
        detector.setFunction("count");
        detectors.add(detector);
        detector = new Detector();
        detector.setFunction("rare");
        detector.setByFieldName("value");
        detectors.add(detector);
        analysisConfig.setDetectors(detectors);
        try
        {
            AnalysisConfigVerifier.verify(analysisConfig);
            Assert.assertTrue(false); // shouldn't get here
        }
        catch (JobConfigurationException e)
        {
            assertEquals(ErrorCodes.INVALID_FUNCTION, e.getErrorCode());
        }

        // Test overlappingBuckets set
        analysisConfig = new AnalysisConfig();
        analysisConfig.setBucketSpan(5000L);
        analysisConfig.setBatchSpan(0L);
        analysisConfig.setOverlappingBuckets(false);
        detectors = new ArrayList<>();
        detector = new Detector();
        detector.setFunction("count");
        detectors.add(detector);
        detector = new Detector();
        detector.setFunction("mean");
        detector.setFieldName("value");
        detectors.add(detector);
        analysisConfig.setDetectors(detectors);
        assertTrue(AnalysisConfigVerifier.verify(analysisConfig));
        assertFalse(analysisConfig.getOverlappingBuckets());
    }

    @Test
    public void testHashCode_GivenEqual()
    {
        AnalysisConfig analysisConfig1 = new AnalysisConfig();
        AnalysisConfig analysisConfig2 = new AnalysisConfig();

        assertEquals(analysisConfig1.hashCode(), analysisConfig2.hashCode());
    }
}
