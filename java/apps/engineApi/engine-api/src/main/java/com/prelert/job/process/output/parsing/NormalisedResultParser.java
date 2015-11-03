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

import java.io.IOException;
import java.util.Objects;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.prelert.job.normalisation.NormalisedResult;

public class NormalisedResultParser
{
    private final JsonParser m_JsonParser;
    private final Logger m_Logger;

    public NormalisedResultParser(JsonParser jsonParser, Logger logger)
    {
        m_JsonParser = Objects.requireNonNull(jsonParser);
        m_Logger = Objects.requireNonNull(logger);
    }

    /**
     * Read a NormalisedResult from the JSON parser
     * @return the {@link NormalisedResult} that was read
     * @throws JsonParseException
     * @throws IOException
     */
    public NormalisedResult parseJson() throws JsonParseException, IOException
    {
        NormalisedResult result = new NormalisedResult();

        JsonToken token = m_JsonParser.nextToken();
        while (token != null && token != JsonToken.END_OBJECT)
        {
            switch (token)
            {
                case START_OBJECT:
                    break;
                case FIELD_NAME:
                    handleFieldName(result);
                    break;
                    //$CASES-OMITTED$
                default:
                    m_Logger.warn(
                            "Parsing error: Only simple fields expected in NormalisedResult not "
                            + token);
                    break;
            }

            token = m_JsonParser.nextToken();
        }

        return result;
    }

    private void handleFieldName(NormalisedResult result) throws JsonParseException, IOException
    {
        String fieldName = m_JsonParser.getCurrentName();
        JsonToken token = m_JsonParser.nextToken();
        switch (fieldName)
        {
            case NormalisedResult.RAW_SCORE:
                result.setRawScore(
                        parseStringValueAsDoubleOrZero(token, NormalisedResult.RAW_SCORE));
                break;
            case NormalisedResult.NORMALIZED_SCORE:
                result.setNormalizedScore(
                        parseStringValueAsDoubleOrZero(token, NormalisedResult.NORMALIZED_SCORE));
                break;
            default:
                m_Logger.trace(String.format(
                        "Parsed unknown field in NormalisedResult %s:%s", fieldName,
                        m_JsonParser.getValueAsString()));
                break;
        }
    }

    private double parseStringValueAsDoubleOrZero(JsonToken token, String key)
            throws JsonParseException, IOException
    {
        if (token == JsonToken.VALUE_STRING)
        {
            String val = m_JsonParser.getValueAsString();
            if (val.isEmpty() == false)
            {
                try
                {
                    return Double.parseDouble(val);
                }
                catch (NumberFormatException nfe)
                {
                    m_Logger.warn("Cannot parse " + key + " : " + m_JsonParser.getText()
                            + " as a double");
                }
            }
        }
        else
        {
            m_Logger.warn("Cannot parse " + key + " : " + m_JsonParser.getText() + " as a double");
        }
        return 0.0;
    }
}
