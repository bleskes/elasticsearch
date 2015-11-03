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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.prelert.job.normalisation.NormalisedResult;

/**
 * Parse the JSON output of the Normaliser.
 */
public class NormalisedResultsParser implements Runnable
{
    private final List<NormalisedResult> m_Results;
    private final InputStream m_InputStream;
    private final Logger m_Logger;

    public NormalisedResultsParser(InputStream inputStream, Logger logger)
    {
        m_InputStream = inputStream;
        m_Logger = logger;
        m_Results = new ArrayList<>();
    }

    @Override
    public void run()
    {
        try
        {
            //printResults();
            parseResults();
        }
        catch (IOException e)
        {
            m_Logger.warn("Error reading normalise output", e);
        }
    }

    public List<NormalisedResult> getNormalisedResults()
    {
        return m_Results;
    }

    private void parseResults() throws JsonParseException, IOException
    {
        JsonParser parser = new JsonFactory().createParser(m_InputStream);
        parser.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);

        JsonToken token = parser.nextToken();
        if (token == null)
        {
            m_Logger.info("Zero results read from the normalizer");
            return;
        }

        // Parse the results from the stream
        int resultCount = 0;
        NormalisedResultParser resultParser = new NormalisedResultParser(parser, m_Logger);
        while (token != null)
        {
            NormalisedResult result = resultParser.parseJson();
            m_Results.add(result);
            resultCount++;

            token = parser.nextToken();
        }

        m_Logger.info(resultCount + " records parsed from output");
    }

    /**
     * Debugging print normalise output
     * @throws IOException
     */
    @SuppressWarnings("unused")
    private void printResults() throws IOException
    {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(m_InputStream, StandardCharsets.UTF_8)))
        {
            in.lines().forEach(line -> System.out.println(line));
        }
    }
}