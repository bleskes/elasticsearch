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

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.prelert.job.results.AnomalyCause;
import com.prelert.utils.json.FieldNameParser;

final class AnomalyCauseParser extends FieldNameParser<AnomalyCause>
{
    private static final Logger LOGGER = Logger.getLogger(AnomalyCauseParser.class);

    public AnomalyCauseParser(JsonParser jsonParser)
    {
        super("AnomalyCause", jsonParser, LOGGER);
    }

    @Override
    protected AnomalyCause supply()
    {
        return new AnomalyCause();
    }

    @Override
    protected void handleFieldName(String fieldName, AnomalyCause cause) throws IOException
    {
        JsonToken token = m_Parser.nextToken();
        switch (fieldName)
        {
        case AnomalyCause.PROBABILITY:
            cause.setProbability(parseAsDoubleOrZero(fieldName));
            break;
        case AnomalyCause.BY_FIELD_NAME:
            cause.setByFieldName(parseAsStringOrNull(fieldName));
            break;
        case AnomalyCause.BY_FIELD_VALUE:
            cause.setByFieldValue(parseAsStringOrNull(fieldName));
            break;
        case AnomalyCause.PARTITION_FIELD_NAME:
            cause.setPartitionFieldName(parseAsStringOrNull(fieldName));
            break;
        case AnomalyCause.PARTITION_FIELD_VALUE:
            cause.setPartitionFieldValue(parseAsStringOrNull(fieldName));
            break;
        case AnomalyCause.FUNCTION:
            cause.setFunction(parseAsStringOrNull(fieldName));
            break;
        case AnomalyCause.FUNCTION_DESCRIPTION:
            cause.setFunctionDescription(parseAsStringOrNull(fieldName));
            break;
        case AnomalyCause.TYPICAL:
            cause.setTypical(parsePrimitiveDoubleArray(fieldName));
            break;
        case AnomalyCause.ACTUAL:
            cause.setActual(parsePrimitiveDoubleArray(fieldName));
            break;
        case AnomalyCause.FIELD_NAME:
            cause.setFieldName(parseAsStringOrNull(fieldName));
            break;
        case AnomalyCause.OVER_FIELD_NAME:
            cause.setOverFieldName(parseAsStringOrNull(fieldName));
            break;
        case AnomalyCause.OVER_FIELD_VALUE:
            cause.setOverFieldValue(parseAsStringOrNull(fieldName));
            break;
        case AnomalyCause.INFLUENCERS:
            cause.setInfluencers(new InfluenceParser(m_Parser).parseJson());
            break;
        default:
            LOGGER.warn(String.format("Parse error unknown field in Anomaly Cause %s:%s",
                    fieldName, token.asString()));
            break;
        }
    }
}
