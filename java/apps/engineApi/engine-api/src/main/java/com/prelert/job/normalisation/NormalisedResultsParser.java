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

/**
 * Parse the JSON output of the Normaliser.
 */
public class NormalisedResultsParser implements Runnable
{
    private List<NormalisedResult> m_Results;
    private InputStream m_InputStream;
    private Logger m_Logger;

    public NormalisedResultsParser(InputStream inputStream, Logger logger)
    {
        m_InputStream = inputStream;
        m_Logger = logger;
        m_Results = new ArrayList<NormalisedResult>();
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

    /**
     * Debugging print normalise output
     * @throws IOException
     */
    @SuppressWarnings("unused")
    private void printResults() throws IOException
    {
        BufferedReader in = new BufferedReader(
                new InputStreamReader(m_InputStream,
                StandardCharsets.UTF_8));

        String line = null;
        while((line = in.readLine()) != null)
        {
            System.out.println(line);
        }
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
        while (token != null)
        {
            NormalisedResult result = NormalisedResult.parseJson(parser, m_Logger);
            m_Results.add(result);
            resultCount++;

            token = parser.nextToken();
        }

        m_Logger.info(resultCount + " records parsed from output");
    }

}