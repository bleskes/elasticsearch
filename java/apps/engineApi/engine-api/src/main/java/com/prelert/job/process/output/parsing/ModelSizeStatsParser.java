/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2016     *
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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.prelert.job.ModelSizeStats;
import com.prelert.job.ModelSizeStats.MemoryStatus;
import com.prelert.utils.json.FieldNameParser;

final class ModelSizeStatsParser extends FieldNameParser<ModelSizeStats>
{
    private static final Logger LOGGER = Logger.getLogger(ModelSizeStats.class);

    public ModelSizeStatsParser(JsonParser jsonParser)
    {
        super("ModelSizeStats", jsonParser, LOGGER);
    }

    @Override
    protected ModelSizeStats supply()
    {
        return new ModelSizeStats();
    }

    @Override
    protected void handleFieldName(String fieldName, ModelSizeStats modelSizeStats)
            throws IOException
    {
        JsonToken token = m_Parser.nextToken();
        switch (fieldName)
        {
        case ModelSizeStats.MODEL_BYTES:
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
        case ModelSizeStats.BUCKET_TIME:
            modelSizeStats.setTimestamp(parseTimestamp(token));
            break;
        default:
            LOGGER.warn(String.format("Parse error unknown field in ModelSizeStats %s:%s",
                    fieldName, token.asString()));
            break;
        }
    }

    private Date parseTimestamp(JsonToken token) throws IOException
    {
        long val = 0;
        if (token == JsonToken.VALUE_NUMBER_INT)
        {
            // convert seconds to ms
            val = m_Parser.getLongValue() * 1000;
        }
        else
        {
            LOGGER.warn("Cannot parse " + token.asString() + " as a long");
        }
        return new Date(val);
    }

}
