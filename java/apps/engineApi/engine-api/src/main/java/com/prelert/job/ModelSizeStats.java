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
package com.prelert.job;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.prelert.utils.json.AutoDetectParseException;
import com.prelert.utils.json.FieldNameParser;

/**
 * Provide access to the C++ model memory usage numbers
 * for the Java process.
 */
@JsonIgnoreProperties({"id"})
public class ModelSizeStats
{
    /**
     * Field Names
     */
    public static final String ID = "id";
    public static final String MODEL_BYTES = "modelBytes";
    public static final String TOTAL_BY_FIELD_COUNT = "totalByFieldCount";
    public static final String TOTAL_OVER_FIELD_COUNT = "totalOverFieldCount";
    public static final String TOTAL_PARTITION_FIELD_COUNT = "totalPartitionFieldCount";
    public static final String BUCKET_ALLOCATION_FAILURES_COUNT = "bucketAllocationFailuresCount";
    public static final String MEMORY_STATUS = "memoryStatus";

    /**
     * Elasticsearch type
     */
    public static final String TYPE = "modelSizeStats";

    /**
     * The status of the memory monitored by the ResourceMonitor.
     * OK is default, SOFT_LIMIT means that the models have done
     * some aggressive pruning to keep the memory below the limit,
     * and HARD_LIMIT means that samples have been dropped
     */
    public enum MemoryStatus { OK, SOFT_LIMIT, HARD_LIMIT };

    private static final Logger LOGGER = Logger.getLogger(ModelSizeStats.class);

    private long m_ModelBytes;
    private long m_TotalByFieldCount;
    private long m_TotalOverFieldCount;
    private long m_TotalPartitionFieldCount;
    private long m_BucketAllocationFailuresCount;
    private MemoryStatus m_MemoryStatus;

    public ModelSizeStats()
    {
        m_ModelBytes = 0;
        m_ModelBytes = 0;
        m_TotalByFieldCount = 0;
        m_TotalOverFieldCount = 0;
        m_TotalPartitionFieldCount = 0;
        m_BucketAllocationFailuresCount = 0;
        m_MemoryStatus = MemoryStatus.OK;
    }

    public String getId()
    {
        return TYPE;
    }

    public void setId(String id)
    {
    }

    public void setModelBytes(long m)
    {
        m_ModelBytes = m;
    }

    public long getModelBytes()
    {
        return m_ModelBytes;
    }

    public void setTotalByFieldCount(long m)
    {
        m_TotalByFieldCount = m;
    }

    public long getTotalByFieldCount()
    {
        return m_TotalByFieldCount;
    }

    public void setTotalPartitionFieldCount(long m)
    {
        m_TotalPartitionFieldCount = m;
    }

    public long getTotalPartitionFieldCount()
    {
        return m_TotalPartitionFieldCount;
    }

    public void setTotalOverFieldCount(long m)
    {
        m_TotalOverFieldCount = m;
    }

    public long getTotalOverFieldCount()
    {
        return m_TotalOverFieldCount;
    }

    public void setBucketAllocationFailuresCount(long m)
    {
        m_BucketAllocationFailuresCount = m;
    }

    public long getBucketAllocationFailuresCount()
    {
        return m_BucketAllocationFailuresCount;
    }

    public void setMemoryStatus(String m)
    {
        if (m == null || m == "")
        {
            m_MemoryStatus = MemoryStatus.OK;
        }
        else
        {
            m_MemoryStatus = MemoryStatus.valueOf(m);
        }
    }

    public String getMemoryStatus()
    {
        return m_MemoryStatus.name();
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
            case TYPE:
                modelSizeStats.setModelBytes(parseAsLongOrZero(token, fieldName));
                break;
            case TOTAL_BY_FIELD_COUNT:
                modelSizeStats.setTotalByFieldCount(parseAsLongOrZero(token, fieldName));
                break;
            case TOTAL_OVER_FIELD_COUNT:
                modelSizeStats.setTotalOverFieldCount(parseAsLongOrZero(token, fieldName));
                break;
            case TOTAL_PARTITION_FIELD_COUNT:
                modelSizeStats.setTotalPartitionFieldCount(parseAsLongOrZero(token, fieldName));
                break;
            case BUCKET_ALLOCATION_FAILURES_COUNT:
                modelSizeStats.setBucketAllocationFailuresCount(parseAsLongOrZero(token, fieldName));
                break;
            case MEMORY_STATUS:
                int status = parseAsIntOrZero(token, fieldName);
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

