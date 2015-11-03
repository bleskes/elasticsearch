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

package com.prelert.job.process.output.parsing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.prelert.job.normalisation.NormalisedResult;

public class NormalisedResultsParserTest
{
    private static final double ERROR = 0.001;

    @Mock private Logger m_Logger;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testRun_GivenEmptyInput()
    {
        String input = "";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        NormalisedResultsParser resultsParser = new NormalisedResultsParser(inputStream, m_Logger);

        resultsParser.run();

        List<NormalisedResult> normalisedResults = resultsParser.getNormalisedResults();
        assertTrue(normalisedResults.isEmpty());
    }

    @Test
    public void testRun_GivenValidNormalisedResults()
    {
        String input = "{\"rawScore\":\"42.0\", \"normalizedScore\":\"0.01\"}\n" +
                "{\"rawScore\":\"43.0\", \"normalizedScore\":\"0.02\"}";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        NormalisedResultsParser resultsParser = new NormalisedResultsParser(inputStream, m_Logger);

        resultsParser.run();

        List<NormalisedResult> normalisedResults = resultsParser.getNormalisedResults();
        assertEquals(2, normalisedResults.size());
        assertEquals(42.0, normalisedResults.get(0).getRawScore(), ERROR);
        assertEquals(0.01, normalisedResults.get(0).getNormalizedScore(), ERROR);
        assertEquals(43.0, normalisedResults.get(1).getRawScore(), ERROR);
        assertEquals(0.02, normalisedResults.get(1).getNormalizedScore(), ERROR);
    }
}
