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
import com.prelert.job.results.ModelDebugOutput;
import com.prelert.utils.json.FieldNameParser;

final class ModelDebugOutputParser extends FieldNameParser<ModelDebugOutput>
{
    private static final Logger LOGGER = Logger.getLogger(ModelDebugOutputParser.class);

    public ModelDebugOutputParser(JsonParser jsonParser)
    {
        super("ModelDebugOutput", jsonParser, LOGGER);
    }

    @Override
    protected ModelDebugOutput supply()
    {
        return new ModelDebugOutput();
    }

    @Override
    protected void handleFieldName(String fieldName, ModelDebugOutput modelDebugOutput)
            throws IOException
    {
        JsonToken token = m_Parser.nextToken();
        switch (fieldName)
        {
        case ModelDebugOutput.TIMESTAMP:
            modelDebugOutput.setTimestamp(new Date(parseAsLongOrZero(fieldName)));
            break;
        case ModelDebugOutput.PARTITION_FIELD_NAME:
            modelDebugOutput.setPartitionFieldName(parseAsStringOrNull(fieldName));
            break;
        case ModelDebugOutput.PARTITION_FIELD_VALUE:
            modelDebugOutput.setPartitionFieldValue(parseAsStringOrNull(fieldName));
            break;
        case ModelDebugOutput.OVER_FIELD_NAME:
            modelDebugOutput.setOverFieldName(parseAsStringOrNull(fieldName));
            break;
        case ModelDebugOutput.OVER_FIELD_VALUE:
            modelDebugOutput.setOverFieldValue(parseAsStringOrNull(fieldName));
            break;
        case ModelDebugOutput.BY_FIELD_NAME:
            modelDebugOutput.setByFieldName(parseAsStringOrNull(fieldName));
            break;
        case ModelDebugOutput.BY_FIELD_VALUE:
            modelDebugOutput.setByFieldValue(parseAsStringOrNull(fieldName));
            break;
        case ModelDebugOutput.DEBUG_FEATURE:
            modelDebugOutput.setDebugFeature(parseAsStringOrNull(fieldName));
            break;
        case ModelDebugOutput.DEBUG_LOWER:
            modelDebugOutput.setDebugLower(parseAsDoubleOrZero(fieldName));
            break;
        case ModelDebugOutput.DEBUG_UPPER:
            modelDebugOutput.setDebugUpper(parseAsDoubleOrZero(fieldName));
            break;
        case ModelDebugOutput.DEBUG_MEDIAN:
            modelDebugOutput.setDebugMedian(parseAsDoubleOrZero(fieldName));
            break;
        case ModelDebugOutput.ACTUAL:
            modelDebugOutput.setActual(parseAsDoubleOrZero(fieldName));
            break;
        default:
            LOGGER.warn(String.format("Parse error unknown field in ModelDebugOutput %s:%s",
                    fieldName, token.asString()));
            break;
        }
    }
}
