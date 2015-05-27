/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2014     *
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

package com.prelert.job.normalisation;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;

public class NormalisedResultTest
{
    private static final double ERROR = 0.001;

    @Mock private Logger m_Logger;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown()
    {
        Mockito.verifyNoMoreInteractions(m_Logger);
    }

    @Test
    public void testParseJson_GivenEmptyInput() throws JsonParseException, IOException
    {
        String input = "";
        JsonParser parser = createJsonParser(input);

        NormalisedResult normalisedResult = NormalisedResult.parseJson(parser, m_Logger);

        assertEquals(0.0, normalisedResult.getRawScore(), ERROR);
        assertEquals(0.0, normalisedResult.getNormalizedScore(), ERROR);
    }

    @Test
    public void testParseJson_GivenEmptyJson() throws JsonParseException, IOException
    {
        String input = "{}";
        JsonParser parser = createJsonParser(input);

        NormalisedResult normalisedResult = NormalisedResult.parseJson(parser, m_Logger);

        assertEquals(0.0, normalisedResult.getRawScore(), ERROR);
        assertEquals(0.0, normalisedResult.getNormalizedScore(), ERROR);
    }

    @Test
    public void testParseJson_GivenEmptyUnknownField() throws JsonParseException, IOException
    {
        String input = "{\"foo\":\"0.0\"}";
        JsonParser parser = createJsonParser(input);

        NormalisedResult normalisedResult = NormalisedResult.parseJson(parser, m_Logger);

        assertEquals(0.0, normalisedResult.getRawScore(), ERROR);
        assertEquals(0.0, normalisedResult.getNormalizedScore(), ERROR);
        verify(m_Logger).trace("Parsed unknown field in NormalisedResult foo:0.0");
    }

    @Test
    public void testParseJson_GivenEmptyList() throws JsonParseException, IOException
    {
        String input = "[]";
        JsonParser parser = createJsonParser(input);

        NormalisedResult normalisedResult = NormalisedResult.parseJson(parser, m_Logger);

        assertEquals(0.0, normalisedResult.getRawScore(), ERROR);
        assertEquals(0.0, normalisedResult.getNormalizedScore(), ERROR);
        verify(m_Logger).warn(
                "Parsing error: Only simple fields expected in NormalisedResult not START_ARRAY");
        verify(m_Logger).warn(
                "Parsing error: Only simple fields expected in NormalisedResult not END_ARRAY");
    }

    @Test
    public void testParseJson_GivenValidNormalisationResult() throws JsonParseException, IOException
    {
        String input = "{\"rawScore\":\"42.0\", \"normalizedScore\":\"0.01\"}";
        JsonParser parser = createJsonParser(input);

        NormalisedResult normalisedResult = NormalisedResult.parseJson(parser, m_Logger);

        assertEquals(42.0, normalisedResult.getRawScore(), ERROR);
        assertEquals(0.01, normalisedResult.getNormalizedScore(), ERROR);
    }

    @Test
    public void testParseJson_GivenRawScoreIsNotNumber() throws JsonParseException,
            IOException
    {
        String input = "{\"rawScore\":\"invalid\"}";
        JsonParser parser = createJsonParser(input);

        NormalisedResult normalisedResult = NormalisedResult.parseJson(parser, m_Logger);

        assertEquals(0.0, normalisedResult.getRawScore(), ERROR);
        assertEquals(0.0, normalisedResult.getNormalizedScore(), ERROR);
        verify(m_Logger).warn("Cannot parse rawScore : invalid as a double");
    }

    @Test
    public void testParseJson_GivenRawScoreIsNotFollowedByValueString()
            throws JsonParseException, IOException
    {
        String input = "{\"rawScore\":[]}";
        JsonParser parser = createJsonParser(input);

        NormalisedResult normalisedResult = NormalisedResult.parseJson(parser, m_Logger);

        assertEquals(0.0, normalisedResult.getRawScore(), ERROR);
        assertEquals(0.0, normalisedResult.getNormalizedScore(), ERROR);
        verify(m_Logger).warn("Cannot parse rawScore : [ as a double");
        verify(m_Logger).warn(
                "Parsing error: Only simple fields expected in NormalisedResult not END_ARRAY");
    }

    @Test
    public void testParseJson_GivenRawScoreIsFollowedEmptyValue() throws JsonParseException,
            IOException
    {
        String input = "{\"rawScore\":\"\"}";
        JsonParser parser = createJsonParser(input);

        NormalisedResult normalisedResult = NormalisedResult.parseJson(parser, m_Logger);

        assertEquals(0.0, normalisedResult.getRawScore(), ERROR);
        assertEquals(0.0, normalisedResult.getNormalizedScore(), ERROR);
    }

    @Test
    public void testParseJson_GivenNormalisedScoreIsNotNumber() throws JsonParseException,
            IOException
    {
        String input = "{\"normalizedScore\":\"invalid\"}";
        JsonParser parser = createJsonParser(input);

        NormalisedResult normalisedResult = NormalisedResult.parseJson(parser, m_Logger);

        assertEquals(0.0, normalisedResult.getRawScore(), ERROR);
        assertEquals(0.0, normalisedResult.getNormalizedScore(), ERROR);
        verify(m_Logger).warn("Cannot parse normalizedScore : invalid as a double");
    }

    @Test
    public void testParseJson_GivenNormalisedScoreIsNotFollowedByValueString()
            throws JsonParseException, IOException
    {
        String input = "{\"normalizedScore\":[]}";
        JsonParser parser = createJsonParser(input);

        NormalisedResult normalisedResult = NormalisedResult.parseJson(parser, m_Logger);

        assertEquals(0.0, normalisedResult.getRawScore(), ERROR);
        assertEquals(0.0, normalisedResult.getNormalizedScore(), ERROR);
        verify(m_Logger).warn("Cannot parse normalizedScore : [ as a double");
        verify(m_Logger).warn(
                "Parsing error: Only simple fields expected in NormalisedResult not END_ARRAY");
    }

    @Test
    public void testParseJson_GivenNormalisedScoreIsFollowedEmptyValue()
            throws JsonParseException, IOException
    {
        String input = "{\"normalizedScore\":\"\"}";
        JsonParser parser = createJsonParser(input);

        NormalisedResult normalisedResult = NormalisedResult.parseJson(parser, m_Logger);

        assertEquals(0.0, normalisedResult.getRawScore(), ERROR);
        assertEquals(0.0, normalisedResult.getNormalizedScore(), ERROR);
    }

    private static final JsonParser createJsonParser(String input) throws JsonParseException,
            IOException
    {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
        return new JsonFactory().createParser(inputStream);
    }
}
