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

package com.prelert.job.process.writer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.prelert.job.SchedulerConfig;
import com.prelert.job.process.exceptions.MalformedJsonException;

/**
 * Reads highly hierarchical JSON structures.  Currently very much geared to the
 * output of Elasticsearch's aggregations.  Could be made more generic in the
 * future if another slightly different hierarchical JSON structure needs to be
 * parsed.
 */
class AggregatedJsonRecordReader extends AbstractJsonRecordReader
{
    private static final String AGG_BUCKETS = "buckets";
    private static final String AGG_KEY = "key";
    private static final String AGG_VALUE = "value";

    private boolean m_IsFirstTime = true;

    private final List<String> m_NestingOrder;
    private List<String> m_NestedValues;
    private String m_LatestDocCount;

    /**
     * Create a reader that simulates simple records given a hierarchical JSON
     * structure where each field is at a progressively deeper level of nesting.
     *
     * @param parser The JSON parser
     * @param fieldMap Map to field name to record array index position
     * @param recordHoldingField
     * @param logger
     * @param nestingOrder
     */
    AggregatedJsonRecordReader(JsonParser parser, Map<String, Integer> fieldMap,
            String recordHoldingField, Logger logger, List<String> nestingOrder)
    {
        super(parser, fieldMap, recordHoldingField, logger);
        m_NestingOrder = Objects.requireNonNull(nestingOrder);
        if (m_NestingOrder.isEmpty())
        {
            throw new IllegalArgumentException(
                    "Expected nesting order for aggregated JSON must not be empty");
        }
        m_NestedValues = new ArrayList<>();
    }

    /**
     * Read forwards in the JSON until enough information has been gathered to
     * write to the record array.
     *
     * @param record Read fields are written to this array. This array is first filled with empty
     * strings and will never contain a <code>null</code>
     * @param gotFields boolean array each element is true if that field
     * was read
     *
     * @return The number of fields in the aggregated hierarchy, or -1 if nothing was read
     * because the end of the stream was reached
     * @throws IOException
     * @throws MalformedJsonException
     */
    @Override
    public long read(String[] record, boolean[] gotFields)
            throws IOException, MalformedJsonException
    {
        initArrays(record, gotFields);
        m_LatestDocCount = null;
        m_FieldCount = 0;
        if (m_IsFirstTime)
        {
            clearNestedLevel();
            consumeToRecordHoldingField();
            m_IsFirstTime = false;
        }

        boolean gotInnerValue = false;
        JsonToken token = tryNextTokenOrReadToEndOnError();
        while (!(token == JsonToken.END_OBJECT && m_NestedLevel == 0))
        {
            if (token == null)
            {
                break;
            }

            if (token == JsonToken.START_OBJECT)
            {
                ++m_NestedLevel;
            }
            else if (token == JsonToken.END_OBJECT)
            {
                if (gotInnerValue)
                {
                    completeRecord(record, gotFields);
                }
                --m_NestedLevel;
                if (m_NestedLevel % 2 == 0 && !m_NestedValues.isEmpty())
                {
                    m_NestedValues.remove(m_NestedValues.size() - 1);
                }
                if (gotInnerValue)
                {
                    break;
                }
            }
            else if (token == JsonToken.FIELD_NAME)
            {
                if (((m_NestedLevel + 1) / 2) == m_NestingOrder.size())
                {
                    gotInnerValue = parseFieldValuePair(record, gotFields) || gotInnerValue;
                }
                // Alternate nesting levels are arbitrary labels that can be
                // ignored.
                else if (m_NestedLevel > 0 && m_NestedLevel % 2 == 0)
                {
                    String fieldName = m_Parser.getCurrentName();
                    if (fieldName.equals(AGG_KEY))
                    {
                        token = tryNextTokenOrReadToEndOnError();
                        if (token == null)
                        {
                            break;
                        }
                        m_NestedValues.add(m_Parser.getText());
                    }
                    else if (fieldName.equals(SchedulerConfig.DOC_COUNT))
                    {
                        token = tryNextTokenOrReadToEndOnError();
                        if (token == null)
                        {
                            break;
                        }
                        m_LatestDocCount = m_Parser.getText();
                    }
                }
            }

            token = tryNextTokenOrReadToEndOnError();
        }

        // null token means EOF; m_NestedLevel 0 means we've reached the end of
        // the aggregations object
        if (token == null || m_NestedLevel == 0)
        {
            return -1;
        }
        return m_FieldCount;
    }

    protected void clearNestedLevel()
    {
        m_NestedLevel = 0;
    }

    private boolean parseFieldValuePair(String[] record, boolean[] gotFields)
            throws IOException, MalformedJsonException
    {
        String fieldName = m_Parser.getCurrentName();
        JsonToken token = tryNextTokenOrReadToEndOnError();

        if (token == null)
        {
            return false;
        }

        if (token == JsonToken.START_OBJECT)
        {
            ++m_NestedLevel;
            return false;
        }

        if (token == JsonToken.START_ARRAY)
        {
            // We don't expect arrays at this level of aggregated input
            // (although we do expect arrays at higher levels).  Consume the
            // whole array but do nothing with it.
            while (token != JsonToken.END_ARRAY)
            {
                token = tryNextTokenOrReadToEndOnError();
            }
            m_Logger.warn("Ignoring array field");
            return false;
        }

        ++m_FieldCount;

        if (AGG_VALUE.equals(fieldName))
        {
            fieldName = m_NestingOrder.get(m_NestingOrder.size() - 1);
        }

        Integer index = m_FieldMap.get(fieldName);
        if (index == null)
        {
            return false;
        }

        String fieldValue = m_Parser.getText();
        record[index] = fieldValue;
        gotFields[index] = true;

        return true;
    }

    private void completeRecord(String[] record, boolean[] gotFields)
            throws IOException, MalformedJsonException
    {
        // This loop should do time plus the by/over/partition/influencer fields
        int numberOfFields = Math.min(m_NestingOrder.size() - 1, m_NestedValues.size());
        if (m_NestingOrder.size() - 1 != m_NestedValues.size())
        {
            m_Logger.warn("Aggregation input does not match expectation: expected field order: "
                    + m_NestingOrder + " actual values: " + m_NestedValues);
        }
        m_FieldCount += numberOfFields;
        for (int i = 0; i < numberOfFields; ++i)
        {
            String fieldName = m_NestingOrder.get(i);
            Integer index = m_FieldMap.get(fieldName);
            if (index == null)
            {
                continue;
            }

            String fieldValue = m_NestedValues.get(i);
            record[index] = fieldValue;
            gotFields[index] = true;
        }

        // This adds the summary count field
        if (m_LatestDocCount != null)
        {
            ++m_FieldCount;
            Integer index = m_FieldMap.get(SchedulerConfig.DOC_COUNT);
            if (index != null)
            {
                record[index] = m_LatestDocCount;
                gotFields[index] = true;
            }
        }
    }
}
