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

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.prelert.job.Detector;
import com.prelert.job.config.verification.JobConfigurationException;
import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;
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
    public void testValid() throws JobConfigurationException
    {
        Detector detector = new Detector();
        detector.setFunction("count");
        detector.setByFieldName("airline");
        Response response = m_Validate.validateDetector(detector);

        Acknowledgement acknowledgement = (Acknowledgement) response.getEntity();
        assertTrue(acknowledgement.getAcknowledgement());
    }

    @Test
    public void testInvalid() throws JobConfigurationException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("fieldName must be set when the 'mean' function is used");
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_FIELD_SELECTION));

        Detector detector = new Detector();
        detector.setFunction("mean");
        detector.setByFieldName("airline");
        m_Validate.validateDetector(detector);
    }
}
