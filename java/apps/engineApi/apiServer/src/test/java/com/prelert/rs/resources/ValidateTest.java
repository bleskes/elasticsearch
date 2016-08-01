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

package com.prelert.rs.resources;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.prelert.job.Detector;
import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobConfigurationException;
import com.prelert.job.transform.TransformConfig;
import com.prelert.rs.data.Acknowledgement;


public class ValidateTest extends ServiceTest
{
    @Rule public ExpectedException m_ExpectedException = ExpectedException.none();

    private Validate m_Validate;

    @Before
    public void setUp()
    {
        m_Validate = new Validate();
        configureService(m_Validate);
    }

    @Test
    public void testValidDetector() throws JobConfigurationException
    {
        Detector detector = new Detector();
        detector.setFunction("count");
        detector.setByFieldName("airline");
        Response response = m_Validate.validateDetector(detector);

        Acknowledgement acknowledgement = (Acknowledgement) response.getEntity();
        assertTrue(acknowledgement.getAcknowledgement());
    }

    @Test
    public void testInvalidDetector() throws JobConfigurationException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("fieldName must be set when the 'mean' function is used");
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_FIELD_SELECTION));

        Detector detector = new Detector();
        detector.setFunction("mean");
        detector.setByFieldName("airline");
        m_Validate.validateDetector(detector);
    }

    @Test
    public void testValidTransformConfig() throws JobConfigurationException
    {
        TransformConfig transformConfig = new TransformConfig();
        transformConfig.setTransform("concat");
        transformConfig.setInputs(Arrays.asList("one", "two"));
        transformConfig.setOutputs(Arrays.asList("oneplustwo"));
        Response response = m_Validate.validateTransform(transformConfig);

        Acknowledgement acknowledgement = (Acknowledgement) response.getEntity();
        assertTrue(acknowledgement.getAcknowledgement());
    }

    @Test
    public void testInvalidTransformConfig() throws JobConfigurationException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Transform type concat expected [2‥+∞) input(s), got 1");
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(ErrorCodes.TRANSFORM_INVALID_INPUT_COUNT));

        TransformConfig transformConfig = new TransformConfig();
        transformConfig.setTransform("concat");
        transformConfig.setInputs(Arrays.asList("justone"));
        transformConfig.setOutputs(Arrays.asList("stilljustone"));
        m_Validate.validateTransform(transformConfig);
    }

    @Test
    public void testValidTransformConfigArray() throws JobConfigurationException
    {
        TransformConfig transformConfig1 = new TransformConfig();
        transformConfig1.setTransform("concat");
        transformConfig1.setInputs(Arrays.asList("one", "two"));
        transformConfig1.setOutputs(Arrays.asList("oneplustwo"));
        TransformConfig transformConfig2 = new TransformConfig();
        transformConfig2.setTransform("domain_split");
        transformConfig2.setInputs(Arrays.asList("domain"));
        transformConfig2.setOutputs(Arrays.asList("sub_domain", "highest_registered_domain"));
        Response response = m_Validate.validateTransforms(new TransformConfig[] { transformConfig1, transformConfig2 });

        Acknowledgement acknowledgement = (Acknowledgement) response.getEntity();
        assertTrue(acknowledgement.getAcknowledgement());
    }

    @Test
    public void testInvalidTransformConfigArray() throws JobConfigurationException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Transform type concat with inputs [one, two] has a circular dependency");
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(ErrorCodes.TRANSFORM_HAS_CIRCULAR_DEPENDENCY));

        TransformConfig transformConfig1 = new TransformConfig();
        transformConfig1.setTransform("concat");
        transformConfig1.setInputs(Arrays.asList("one", "two"));
        transformConfig1.setOutputs(Arrays.asList("three"));
        TransformConfig transformConfig2 = new TransformConfig();
        transformConfig2.setTransform("concat");
        transformConfig2.setInputs(Arrays.asList("two", "three"));
        transformConfig2.setOutputs(Arrays.asList("one"));
        m_Validate.validateTransforms(new TransformConfig[] { transformConfig1, transformConfig2 });
        m_Validate.validateTransforms(new TransformConfig[] { transformConfig1, transformConfig2 });
    }
}
