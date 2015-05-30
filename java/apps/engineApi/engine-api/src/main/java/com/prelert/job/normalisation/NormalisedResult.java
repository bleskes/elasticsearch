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

import java.io.IOException;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * Store the output of the normaliser process
 *
 * {"rawScore":"0.0","normalizedScore":"0"}
 *
 */
public class NormalisedResult
{
    public static final String RAW_SCORE = "rawScore";
    public static final String NORMALIZED_SCORE = "normalizedScore";

    private double m_RawScore;
    private double m_NormalizedScore;

    public double getRawScore()
    {
        return m_RawScore;
    }

    public void setRawScore(double rawScore)
    {
        this.m_RawScore = rawScore;
    }

    public double getNormalizedScore()
    {
        return m_NormalizedScore;
    }

    public void setNormalizedScore(double normalizedScore)
    {
        this.m_NormalizedScore = normalizedScore;
    }

    public static NormalisedResult parseJson(JsonParser parser, Logger logger)
            throws JsonParseException, IOException
    {
        NormalisedResult result = new NormalisedResult();

        JsonToken token = parser.nextToken();
        while (token != null && token != JsonToken.END_OBJECT)
        {
            switch (token)
            {
                case START_OBJECT:
                    break;
                case FIELD_NAME:
                    String fieldName = parser.getCurrentName();
                    token = parser.nextToken();
                    handleFieldName(result, token, parser, fieldName, logger);
                    break;
                default:
                    logger.warn("Parsing error: Only simple fields expected in NormalisedResult not "
                            + token);
                    break;
            }

            token = parser.nextToken();
        }

        return result;
    }

    private static void handleFieldName(NormalisedResult result, JsonToken token, JsonParser parser,
            String fieldName, Logger logger) throws JsonParseException, IOException
    {
        switch (fieldName)
        {
            case RAW_SCORE:
                result.setRawScore(parseStringValueAsDoubleOrZero(token, parser,
                        RAW_SCORE, logger));
                break;
            case NORMALIZED_SCORE:
                result.setNormalizedScore(parseStringValueAsDoubleOrZero(token, parser,
                        NORMALIZED_SCORE, logger));
                break;
            default:
                logger.trace(String.format(
                        "Parsed unknown field in NormalisedResult %s:%s", fieldName,
                        parser.getValueAsString()));
                break;
        }
    }

    private static double parseStringValueAsDoubleOrZero(JsonToken token, JsonParser parser,
            String key, Logger logger) throws JsonParseException, IOException
    {
        // TODO this is string should be output as a double
        // if (token == JsonToken.VALUE_NUMBER_FLOAT ||
        //     token == JsonToken.VALUE_NUMBER_INT)
        // {
        //     return parser.getDoubleValue();
        // }

        if (token == JsonToken.VALUE_STRING)
        {
            String val = parser.getValueAsString();
            if (val.isEmpty() == false)
            {
                try
                {
                    return Double.parseDouble(val);
                }
                catch (NumberFormatException nfe)
                {
                    logger.warn("Cannot parse " + key + " : " + parser.getText() + " as a double");
                }
            }
        }
        else
        {
            logger.warn("Cannot parse " + key + " : " + parser.getText() + " as a double");
        }
        return 0.0;
    }
}
