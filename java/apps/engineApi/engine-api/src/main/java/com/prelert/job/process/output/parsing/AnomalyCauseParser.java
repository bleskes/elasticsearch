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

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.prelert.job.results.AnomalyCause;
import com.prelert.utils.json.AutoDetectParseException;
import com.prelert.utils.json.FieldNameParser;

public class AnomalyCauseParser
{
    private static final Logger LOGGER = Logger.getLogger(AnomalyCauseParser.class);

    private AnomalyCauseParser()
    {
    }

    /**
     * Create a new <code>AnomalyCause</code> and populate it from the JSON parser.
     * The parser must be pointing at the start of the object then all the object's
     * fields are read and if they match the property names then the appropriate
     * members are set.
     *
     * Does not validate that all the properties (or any) have been set but if
     * parsing fails an exception will be thrown.
     *
     * @param parser The JSON Parser should be pointing to the start of the object,
     * when the function returns it will be pointing to the end.
     * @return The new AnomalyCause
     * @throws JsonParseException
     * @throws IOException
     * @throws AutoDetectParseException
     */
    public static AnomalyCause parseJson(JsonParser parser)
    throws JsonParseException, IOException, AutoDetectParseException
    {
        AnomalyCause cause = new AnomalyCause();
        AnomalyCauseJsonParser anomalyCauseJsonParser = new AnomalyCauseJsonParser(parser, LOGGER);
        anomalyCauseJsonParser.parse(cause);
        return cause;
    }

    private static class AnomalyCauseJsonParser extends FieldNameParser<AnomalyCause>
    {

        public AnomalyCauseJsonParser(JsonParser jsonParser, Logger logger)
        {
            super("AnomalyCause", jsonParser, logger);
        }

        @Override
        protected void handleFieldName(String fieldName, AnomalyCause cause)
                throws AutoDetectParseException, JsonParseException, IOException
        {
            JsonToken token = m_Parser.nextToken();
            switch (fieldName)
            {
            case AnomalyCause.PROBABILITY:
                cause.setProbability(parseAsDoubleOrZero(token, fieldName));
                break;
            case AnomalyCause.BY_FIELD_NAME:
                cause.setByFieldName(parseAsStringOrNull(token, fieldName));
                break;
            case AnomalyCause.BY_FIELD_VALUE:
                cause.setByFieldValue(parseAsStringOrNull(token, fieldName));
                break;
            case AnomalyCause.PARTITION_FIELD_NAME:
                cause.setPartitionFieldName(parseAsStringOrNull(token, fieldName));
                break;
            case AnomalyCause.PARTITION_FIELD_VALUE:
                cause.setPartitionFieldValue(parseAsStringOrNull(token, fieldName));
                break;
            case AnomalyCause.FUNCTION:
                cause.setFunction(parseAsStringOrNull(token, fieldName));
                break;
            case AnomalyCause.FUNCTION_DESCRIPTION:
                cause.setFunctionDescription(parseAsStringOrNull(token, fieldName));
                break;
            case AnomalyCause.TYPICAL:
                cause.setTypical(parseAsDoubleOrZero(token, fieldName));
                break;
            case AnomalyCause.ACTUAL:
                cause.setActual(parseAsDoubleOrZero(token, fieldName));
                break;
            case AnomalyCause.FIELD_NAME:
                cause.setFieldName(parseAsStringOrNull(token, fieldName));
                break;
            case AnomalyCause.OVER_FIELD_NAME:
                cause.setOverFieldName(parseAsStringOrNull(token, fieldName));
                break;
            case AnomalyCause.OVER_FIELD_VALUE:
                cause.setOverFieldValue(parseAsStringOrNull(token, fieldName));
                break;
            case AnomalyCause.INFLUENCERS:
                cause.setInfluences(InfluenceParser.parseJson(m_Parser));
                break;
            default:
                LOGGER.warn(String.format("Parse error unknown field in Anomaly Cause %s:%s",
                        fieldName, token.asString()));
                break;
            }
        }
    }

}
