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
import java.util.Date;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.prelert.job.quantiles.Quantiles;
import com.prelert.utils.json.AutoDetectParseException;
import com.prelert.utils.json.FieldNameParser;

public class QuantilesParser
{
    private static final Logger LOGGER = Logger.getLogger(QuantilesParser.class);

    private QuantilesParser()
    {
    }


    /**
     * Create a new <code>Quantiles</code> and populate it from the JSON parser.
     * The parser must be pointing at the start of the object then all the object's
     * fields are read and if they match the property names the appropriate
     * members are set.
     *
     * Does not validate that all the properties (or any) have been set but if
     * parsing fails an exception will be thrown.
     *
     * @param parser The JSON Parser should be pointing to the start of the object,
     * when the function returns it will be pointing to the end.
     * @return The new quantiles
     * @throws JsonParseException
     * @throws IOException
     * @throws AutoDetectParseException
     */
    public static Quantiles parseJson(JsonParser parser)
    throws JsonParseException, IOException, AutoDetectParseException
    {
        Quantiles quantiles = new Quantiles();
        QuantilesJsonParser quantilesJsonParser = new QuantilesJsonParser(parser, LOGGER);
        quantilesJsonParser.parse(quantiles);
        return quantiles;
    }


    /**
     * Create a new <code>Quantiles</code> and populate it from the JSON parser.
     * The parser must be pointing at the first token inside the object.  It
     * is assumed that prior code has validated that the previous token was
     * the start of an object.  Then all the object's fields are read and if
     * they match the property names the appropriate members are set.
     *
     * Does not validate that all the properties (or any) have been set but if
     * parsing fails an exception will be thrown.
     *
     * @param parser The JSON Parser should be pointing to the start of the object,
     * when the function returns it will be pointing to the end.
     * @return The new quantiles
     * @throws JsonParseException
     * @throws IOException
     * @throws AutoDetectParseException
     */
    public static Quantiles parseJsonAfterStartObject(JsonParser parser)
    throws JsonParseException, IOException, AutoDetectParseException
    {
        Quantiles quantiles = new Quantiles();
        QuantilesJsonParser quantilesJsonParser = new QuantilesJsonParser(parser, LOGGER);
        quantilesJsonParser.parseAfterStartObject(quantiles);
        return quantiles;
    }

    private static class QuantilesJsonParser extends FieldNameParser<Quantiles>
    {

        public QuantilesJsonParser(JsonParser jsonParser, Logger logger)
        {
            super("Quantiles", jsonParser, logger);
        }

        @Override
        protected void handleFieldName(String fieldName, Quantiles quantiles)
                throws AutoDetectParseException, JsonParseException, IOException
        {
            JsonToken token = m_Parser.nextToken();
            switch (fieldName)
            {
            case Quantiles.TIMESTAMP:
                long seconds = parseAsLongOrZero(fieldName);
                // convert seconds to ms
                quantiles.setTimestamp(new Date(seconds * 1000));
                break;
            case Quantiles.QUANTILE_STATE:
                quantiles.setState(parseAsStringOrNull(fieldName));
                break;
            default:
                LOGGER.warn(String.format("Parse error unknown field in Quantiles %s:%s",
                        fieldName, token.asString()));
                break;
            }
        }
    }
}
