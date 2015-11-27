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
import com.prelert.job.ModelSizeStats;
import com.prelert.job.ModelSizeStats.MemoryStatus;
import com.prelert.utils.json.AutoDetectParseException;
import com.prelert.utils.json.FieldNameParser;

public class ModelSizeStatsParser
{
    private static final Logger LOGGER = Logger.getLogger(ModelSizeStats.class);

    private ModelSizeStatsParser()
    {

    }

    /**
     * Create a new <code>ModelSizeStats</code> and populate it from the JSON parser.
     * The parser must be pointing at the start of the object then all the object's
     * fields are read and if they match the property names the appropriate
     * members are set.
     *
     * Does not validate that all the properties (or any) have been set but if
     * parsing fails an exception will be thrown.
     *
     * @param parser The JSON Parser should be pointing to the start of the object,
     * when the function returns it will be pointing to the end.
     * @return The new ModelSizeStats
     * @throws JsonParseException
     * @throws IOException
     * @throws AutoDetectParseException
     */
    public static ModelSizeStats parseJson(JsonParser parser)
    throws JsonParseException, IOException, AutoDetectParseException
    {
        ModelSizeStats modelSizeStats = new ModelSizeStats();
        ModelSizeStatsJsonParser modelSizeStatsJsonParser = new ModelSizeStatsJsonParser(parser,
                LOGGER);
        modelSizeStatsJsonParser.parse(modelSizeStats);
        return modelSizeStats;
    }


    /**
     * Create a new <code>ModelSizeStats</code> and populate it from the JSON parser.
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
     * @return The new ModelSizeStats
     * @throws JsonParseException
     * @throws IOException
     * @throws AutoDetectParseException
     */
    public static ModelSizeStats parseJsonAfterStartObject(JsonParser parser)
    throws JsonParseException, IOException, AutoDetectParseException
    {
        ModelSizeStats modelSizeStats = new ModelSizeStats();
        ModelSizeStatsJsonParser modelSizeStatsJsonParser = new ModelSizeStatsJsonParser(parser,
                LOGGER);
        modelSizeStatsJsonParser.parseAfterStartObject(modelSizeStats);
        return modelSizeStats;
    }

    private static class ModelSizeStatsJsonParser extends FieldNameParser<ModelSizeStats>
    {

        public ModelSizeStatsJsonParser(JsonParser jsonParser, Logger logger)
        {
            super("ModelSizeStats", jsonParser, logger);
        }

        @Override
        protected void handleFieldName(String fieldName, ModelSizeStats modelSizeStats)
                throws AutoDetectParseException, JsonParseException, IOException
        {
            JsonToken token = m_Parser.nextToken();
            switch (fieldName)
            {
            case ModelSizeStats.TYPE:
                modelSizeStats.setModelBytes(parseAsLongOrZero(fieldName));
                break;
            case ModelSizeStats.TOTAL_BY_FIELD_COUNT:
                modelSizeStats.setTotalByFieldCount(parseAsLongOrZero(fieldName));
                break;
            case ModelSizeStats.TOTAL_OVER_FIELD_COUNT:
                modelSizeStats.setTotalOverFieldCount(parseAsLongOrZero(fieldName));
                break;
            case ModelSizeStats.TOTAL_PARTITION_FIELD_COUNT:
                modelSizeStats.setTotalPartitionFieldCount(parseAsLongOrZero(fieldName));
                break;
            case ModelSizeStats.BUCKET_ALLOCATION_FAILURES_COUNT:
                modelSizeStats.setBucketAllocationFailuresCount(parseAsLongOrZero(fieldName));
                break;
            case ModelSizeStats.MEMORY_STATUS:
                int status = parseAsIntOrZero(fieldName);
                modelSizeStats.setMemoryStatus(MemoryStatus.values()[status].name());
                break;
            default:
                LOGGER.warn(String.format("Parse error unknown field in ModelSizeStats %s:%s",
                        fieldName, token.asString()));
                break;
            }
        }
    }
}
