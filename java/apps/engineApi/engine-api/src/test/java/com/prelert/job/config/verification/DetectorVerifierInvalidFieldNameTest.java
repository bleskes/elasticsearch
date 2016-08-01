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

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.prelert.job.Detector;
import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobConfigurationException;

@RunWith(Parameterized.class)
public class DetectorVerifierInvalidFieldNameTest
{
    private static final String SUFFIX = "suffix";

    @Rule public ExpectedException m_ExpectedException = ExpectedException.none();

    private Detector m_Detector;
    private String m_Character;
    private boolean m_IsValid;

    @Parameters
    public static Collection<Object[]> getCharactersAndValidity()
    {
        return Arrays.asList(new Object[][] {
                // char, isValid?
                { "a", true},
                { "[", true },
                { "]", true },
                { "(", true },
                { ")", true },
                { "=", true },
                { "-", true },
                { " ", true },
                { "\"", false },
                { "\\", false },
                { "\t", false },
                { "\n", false },
        });
    }

    public DetectorVerifierInvalidFieldNameTest(String character, boolean isValid)
    {
        m_Character = character;
        m_IsValid = isValid;
    }

    @Before
    public void setUp()
    {
        m_Detector = createDetectorWithValidFieldNames();
    }

    @Test
    public void testVerify_FieldName() throws JobConfigurationException
    {
        expectJobConfigurationExceptionWhenCharIsInvalid(
                ErrorCodes.PROHIBITIED_CHARACTER_IN_FIELD_NAME);

        m_Detector.setFieldName(m_Detector.getFieldName() + getCharacterPlusSuffix());
        DetectorVerifier.verify(m_Detector, false);
    }

    @Test
    public void testVerify_ByFieldName() throws JobConfigurationException
    {
        expectJobConfigurationExceptionWhenCharIsInvalid(
                ErrorCodes.PROHIBITIED_CHARACTER_IN_FIELD_NAME);

        m_Detector.setByFieldName(m_Detector.getByFieldName() + getCharacterPlusSuffix());
        DetectorVerifier.verify(m_Detector, false);
    }

    @Test
    public void testVerify_OverFieldName() throws JobConfigurationException
    {
        expectJobConfigurationExceptionWhenCharIsInvalid(
                ErrorCodes.PROHIBITIED_CHARACTER_IN_FIELD_NAME);

        m_Detector.setOverFieldName(m_Detector.getOverFieldName() + getCharacterPlusSuffix());
        DetectorVerifier.verify(m_Detector, false);
    }

    @Test
    public void testVerify_PartitionFieldName() throws JobConfigurationException
    {
        expectJobConfigurationExceptionWhenCharIsInvalid(
                ErrorCodes.PROHIBITIED_CHARACTER_IN_FIELD_NAME);

        m_Detector.setPartitionFieldName(m_Detector.getPartitionFieldName() + getCharacterPlusSuffix());
        DetectorVerifier.verify(m_Detector, false);
    }

    @Test
    public void testVerify_FieldNameGivenPresummarised() throws JobConfigurationException
    {
        expectJobConfigurationException(ErrorCodes.INVALID_FUNCTION);

        m_Detector.setFieldName(m_Detector.getFieldName() + getCharacterPlusSuffix());
        DetectorVerifier.verify(m_Detector, true);
    }

    @Test
    public void testVerify_ByFieldNameGivenPresummarised() throws JobConfigurationException
    {
        expectJobConfigurationException(ErrorCodes.INVALID_FUNCTION);

        m_Detector.setByFieldName(m_Detector.getByFieldName() + getCharacterPlusSuffix());
        DetectorVerifier.verify(m_Detector, true);
    }

    @Test
    public void testVerify_OverFieldNameGivenPresummarised() throws JobConfigurationException
    {
        expectJobConfigurationException(ErrorCodes.INVALID_FUNCTION);

        m_Detector.setOverFieldName(m_Detector.getOverFieldName() + getCharacterPlusSuffix());
        DetectorVerifier.verify(m_Detector, true);
    }

    @Test
    public void testVerify_PartitionFieldNameGivenPresummarised() throws JobConfigurationException
    {
        expectJobConfigurationException(ErrorCodes.INVALID_FUNCTION);

        m_Detector.setPartitionFieldName(m_Detector.getPartitionFieldName() + getCharacterPlusSuffix());
        DetectorVerifier.verify(m_Detector, true);
    }

    private String getCharacterPlusSuffix()
    {
        return m_Character + SUFFIX;
    }

    private void expectJobConfigurationExceptionWhenCharIsInvalid(ErrorCodes errorCode)
    {
        if (!m_IsValid)
        {
            expectJobConfigurationException(errorCode);
        }
    }

    private void expectJobConfigurationException(ErrorCodes errorCode)
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(errorCode));
    }

    private static Detector createDetectorWithValidFieldNames()
    {
        Detector d = new Detector();
        d.setFieldName("field");
        d.setByFieldName("by");
        d.setOverFieldName("over");
        d.setPartitionFieldName("partition");
        return d;
    }
}
