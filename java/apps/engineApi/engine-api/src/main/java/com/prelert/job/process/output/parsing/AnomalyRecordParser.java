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
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.prelert.job.results.AnomalyCause;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.utils.json.FieldNameParser;

final class AnomalyRecordParser extends FieldNameParser<AnomalyRecord>
{
    private static final Logger LOGGER = Logger.getLogger(AnomalyRecordParser.class);

    public AnomalyRecordParser(JsonParser jsonParser)
    {
        super("Anomaly Record", jsonParser, LOGGER);
    }

    @Override
    protected AnomalyRecord supply()
    {
        return new AnomalyRecord();
    }

    @Override
    protected void handleFieldName(String fieldName, AnomalyRecord record) throws IOException
    {
        JsonToken token = m_Parser.nextToken();
        switch (fieldName)
        {
        case AnomalyRecord.DETECTOR_INDEX:
            record.setDetectorIndex(parseAsIntOrZero(fieldName));
            break;
        case AnomalyRecord.PROBABILITY:
            record.setProbability(parseAsDoubleOrZero(fieldName));
            break;
        case AnomalyRecord.ANOMALY_SCORE:
            record.setAnomalyScore(parseAsDoubleOrZero(fieldName));
            break;
        case AnomalyRecord.NORMALIZED_PROBABILITY:
            record.setNormalizedProbability(parseAsDoubleOrZero(fieldName));
            record.setInitialNormalizedProbability(record.getNormalizedProbability());
            break;
        case AnomalyRecord.BY_FIELD_NAME:
            record.setByFieldName(parseAsStringOrNull(fieldName));
            break;
        case AnomalyRecord.BY_FIELD_VALUE:
            record.setByFieldValue(parseAsStringOrNull(fieldName));
            break;
        case AnomalyRecord.PARTITION_FIELD_NAME:
            record.setPartitionFieldName(parseAsStringOrNull(fieldName));
            break;
        case AnomalyRecord.PARTITION_FIELD_VALUE:
            record.setPartitionFieldValue(parseAsStringOrNull(fieldName));
            break;
        case AnomalyRecord.FUNCTION:
            record.setFunction(parseAsStringOrNull(fieldName));
            break;
        case AnomalyRecord.FUNCTION_DESCRIPTION:
            record.setFunctionDescription(parseAsStringOrNull(fieldName));
            break;
        case AnomalyRecord.TYPICAL:
            record.setTypical(parsePrimitiveDoubleArray(fieldName));
            break;
        case AnomalyRecord.ACTUAL:
            record.setActual(parsePrimitiveDoubleArray(fieldName));
            break;
        case AnomalyRecord.FIELD_NAME:
            record.setFieldName(parseAsStringOrNull(fieldName));
            break;
        case AnomalyRecord.OVER_FIELD_NAME:
            record.setOverFieldName(parseAsStringOrNull(fieldName));
            break;
        case AnomalyRecord.OVER_FIELD_VALUE:
            record.setOverFieldValue(parseAsStringOrNull(fieldName));
            break;
        case AnomalyRecord.IS_INTERIM:
            record.setInterim(parseAsBooleanOrNull(fieldName));
            break;
        case AnomalyRecord.INFLUENCERS:
            record.setInfluencers(new InfluenceParser(m_Parser).parseJson());
            break;
        case AnomalyRecord.CAUSES:
            record.setCauses(parseCauses(fieldName));
            break;
        case AnomalyRecord.BUCKET_SPAN:
            record.setBucketSpan(parseAsLongOrZero(fieldName));
            break;
        default:
            LOGGER.warn(String.format("Parse error unknown field in Anomaly Record %s:%s",
                    fieldName, token.asString()));
            break;
        }
    }

    private List<AnomalyCause> parseCauses(String fieldName) throws IOException
    {
        List<AnomalyCause> causes = new ArrayList<>();
        parseArray(fieldName, () -> new AnomalyCauseParser(m_Parser).parseJson(), causes);
        return causes;
    }
}
