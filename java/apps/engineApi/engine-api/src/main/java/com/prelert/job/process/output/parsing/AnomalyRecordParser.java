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
import com.prelert.job.results.AnomalyRecord;
import com.prelert.utils.json.AutoDetectParseException;
import com.prelert.utils.json.FieldNameParser;

public class AnomalyRecordParser
{
    private static final Logger LOGGER = Logger.getLogger(AnomalyRecordParser.class);

    private AnomalyRecordParser()
    {

    }

    /**
     * Create a new <code>AnomalyRecord</code> and populate it from the JSON parser.
     * The parser must be pointing at the start of the object then all the object's
     * fields are read and if they match the property names then the appropriate
     * members are set.
     *
     * Does not validate that all the properties (or any) have been set but if
     * parsing fails an exception will be thrown.
     *
     * @param parser The JSON Parser should be pointing to the start of the object,
     * when the function returns it will be pointing to the end.
     * @return The new AnomalyRecord
     * @throws JsonParseException
     * @throws IOException
     * @throws AutoDetectParseException
     */
    public static AnomalyRecord parseJson(JsonParser parser)
    throws JsonParseException, IOException, AutoDetectParseException
    {
        AnomalyRecord record = new AnomalyRecord();
        AnomalyRecordJsonParser anomalyRecordJsonParser = new AnomalyRecordJsonParser(parser,
                LOGGER);
        anomalyRecordJsonParser.parse(record);
        return record;
    }

    private static class AnomalyRecordJsonParser extends FieldNameParser<AnomalyRecord> {

        public AnomalyRecordJsonParser(JsonParser jsonParser, Logger logger)
        {
            super("Anomaly Record", jsonParser, logger);
        }

        @Override
        protected void handleFieldName(String fieldName, AnomalyRecord record)
                throws AutoDetectParseException, JsonParseException, IOException
        {
            JsonToken token = m_Parser.nextToken();
            switch (fieldName)
            {
            case AnomalyRecord.PROBABILITY:
                record.setProbability(parseAsDoubleOrZero(token, fieldName));
                break;
            case AnomalyRecord.ANOMALY_SCORE:
                record.setAnomalyScore(parseAsDoubleOrZero(token, fieldName));
                break;
            case AnomalyRecord.NORMALIZED_PROBABILITY:
                record.setNormalizedProbability(parseAsDoubleOrZero(token, fieldName));
                break;
            case AnomalyRecord.BY_FIELD_NAME:
                record.setByFieldName(parseAsStringOrNull(token, fieldName));
                break;
            case AnomalyRecord.BY_FIELD_VALUE:
                record.setByFieldValue(parseAsStringOrNull(token, fieldName));
                break;
            case AnomalyRecord.PARTITION_FIELD_NAME:
                record.setPartitionFieldName(parseAsStringOrNull(token, fieldName));
                break;
            case AnomalyRecord.PARTITION_FIELD_VALUE:
                record.setPartitionFieldValue(parseAsStringOrNull(token, fieldName));
                break;
            case AnomalyRecord.FUNCTION:
                record.setFunction(parseAsStringOrNull(token, fieldName));
                break;
            case AnomalyRecord.FUNCTION_DESCRIPTION:
                record.setFunctionDescription(parseAsStringOrNull(token, fieldName));
                break;
            case AnomalyRecord.TYPICAL:
                record.setTypical(parseAsDoubleOrZero(token, fieldName));
                break;
            case AnomalyRecord.ACTUAL:
                record.setActual(parseAsDoubleOrZero(token, fieldName));
                break;
            case AnomalyRecord.FIELD_NAME:
                record.setFieldName(parseAsStringOrNull(token, fieldName));
                break;
            case AnomalyRecord.OVER_FIELD_NAME:
                record.setOverFieldName(parseAsStringOrNull(token, fieldName));
                break;
            case AnomalyRecord.OVER_FIELD_VALUE:
                record.setOverFieldValue(parseAsStringOrNull(token, fieldName));
                break;
            case AnomalyRecord.IS_INTERIM:
                record.setInterim(parseAsBooleanOrNull(token, fieldName));
                break;
            case AnomalyRecord.INFLUENCES:
                record.setInfluences(InfluenceParser.parseJson(m_Parser));
                break;
            case AnomalyRecord.CAUSES:
                if (token != JsonToken.START_ARRAY)
                {
                    String msg = "Invalid value Expecting an array of causes";
                    LOGGER.warn(msg);
                    throw new AutoDetectParseException(msg);
                }

                token = m_Parser.nextToken();
                while (token != JsonToken.END_ARRAY)
                {
                    AnomalyCause cause = AnomalyCauseParser.parseJson(m_Parser);
                    record.addCause(cause);

                    token = m_Parser.nextToken();
                }
                break;
            default:
                LOGGER.warn(String.format("Parse error unknown field in Anomaly Record %s:%s",
                        fieldName, token.asString()));
                break;
            }
        }
    }


}
