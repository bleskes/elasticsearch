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

import java.util.Objects;

/**
 * Job processed record counts
 */
public class DataCounts
{
    public static final String BUCKET_COUNT = "bucketCount";
    public static final String PROCESSED_RECORD_COUNT = "processedRecordCount";
    public static final String PROCESSED_FIELD_COUNT = "processedFieldCount";
    public static final String INPUT_BYTES = "inputBytes";
    public static final String INPUT_RECORD_COUNT = "inputRecordCount";
    public static final String INPUT_FIELD_COUNT = "inputFieldCount";
    public static final String INVALID_DATE_COUNT = "invalidDateCount";
    public static final String MISSING_FIELD_COUNT = "missingFieldCount";
    public static final String OUT_OF_ORDER_TIME_COUNT = "outOfOrderTimeStampCount";
    public static final String FAILED_TRANSFORM_COUNT = "failedTransformCount";


    private long m_BucketCount;
    private long m_ProcessedRecordCount;
    private long m_ProcessedFieldCount;
    private long m_InputRecordCount;
    private long m_InputBytes;
    private long m_InputFieldCount;
    private long m_InvalidDateCount;
    private long m_MissingFieldCount;
    private long m_OutOfOrderTimeStampCount;
    private long m_FailedTransformCount;


    public DataCounts()
    {

    }

    public DataCounts(DataCounts lhs)
    {
        m_BucketCount = lhs.m_BucketCount;
        m_ProcessedRecordCount = lhs.m_ProcessedRecordCount;
        m_ProcessedFieldCount = lhs.m_ProcessedFieldCount;
        m_InputRecordCount = lhs.m_InputRecordCount;
        m_InputBytes = lhs.m_InputBytes;
        m_InputFieldCount = lhs.m_InputFieldCount;
        m_InvalidDateCount = lhs.m_InvalidDateCount;
        m_MissingFieldCount = lhs.m_MissingFieldCount;
        m_OutOfOrderTimeStampCount = lhs.m_OutOfOrderTimeStampCount;
        m_FailedTransformCount = lhs.m_FailedTransformCount;
    }


    /**
     * The number of bucket results
     * @return
     */
    public long getBucketCount()
    {
        return m_BucketCount;
    }

    public void setBucketCount(long count)
    {
        m_BucketCount = count;
    }

    /**
     * Number of records processed by this job.
     * This value is the number of records sent to the job
     * including any records that my be discarded for any
     * reason e.g. because the date cannot be read
     * @return
     */
    public long getProcessedRecordCount()
    {
        return m_ProcessedRecordCount;
    }

    public void setProcessedRecordCount(long count)
    {
        m_ProcessedRecordCount = count;
    }

    public void incrementProcessedRecordCount(long additional)
    {
        m_ProcessedRecordCount += additional;
    }

    /**
     * Number of data points (processed record count * the number
     * of analysed fields) processed by this job. This count does
     * not include the time field.
     * @return
     */
    public long getProcessedFieldCount()
    {
        return m_ProcessedFieldCount;
    }

    public void setProcessedFieldCount(long count)
    {
        m_ProcessedFieldCount = count;
    }

    public void incrementProcessedFieldCount(long additional)
    {
        m_ProcessedFieldCount += additional;
    }



    /**
     * Total number of input records
     * @return
     */
    public long getInputRecordCount()
    {
        return m_InputRecordCount;
    }

    public void setInputRecordCount(long count)
    {
        m_InputRecordCount = count;
    }

    public void incrementInputRecordCount(long additional)
    {
        m_InputRecordCount += additional;
    }


    /**
     * The total number of bytes sent to this job.
     * This value includes the bytes from any  records
     * that have been discarded for any  reason
     * e.g. because the date cannot be read
     * @return Volume in bytes
     */
    public long getInputBytes()
    {
        return m_InputBytes;
    }

    public void setInputBytes(long volume)
    {
        m_InputBytes = volume;
    }

    public void incrementInputBytes(long additional)
    {
        m_InputBytes += additional;
    }

    /**
     * The total number of fields sent to the job
     * including fields that aren't analysed.
     * @return
     */
    public long getInputFieldCount()
    {
        return m_InputFieldCount;
    }

    public void setInputFieldCount(long volume)
    {
        m_InputFieldCount = volume;
    }


    /**
     * The number of records with an invalid date field that could
     * not be parsed or converted to epoch time.
     * @return
     */
    public long getInvalidDateCount()
    {
        return m_InvalidDateCount;
    }

    public void setInvalidDateCount(long count)
    {
        m_InvalidDateCount = count;
    }

    public void incrementInvalidDateCount(long additional)
    {
        m_InvalidDateCount += additional;
    }


    /**
     * The number of records missing a field that had been
     * configured for analysis.
     * @return
     */
    public long getMissingFieldCount()
    {
        return m_MissingFieldCount;
    }

    public void setMissingFieldCount(long count)
    {
        m_MissingFieldCount = count;
    }

    public void incrementMissingFieldCount(long additional)
    {
        m_MissingFieldCount += additional;
    }

    /**
     * The number of records with a timestamp that is
     * before the time of the latest record. Records should
     * be in ascending chronological order
     * @return
     */
    public long getOutOfOrderTimeStampCount()
    {
        return m_OutOfOrderTimeStampCount;
    }

    public void setOutOfOrderTimeStampCount(long count)
    {
        m_OutOfOrderTimeStampCount = count;
    }

    public void incrementOutOfOrderTimeStampCount(long additional)
    {
        m_OutOfOrderTimeStampCount += additional;
    }


    /**
     * The number of transforms that failed.
     * In theory this could be more than the number of records
     * if multiple transforms are applied to each record
     * @return
     */
    public long getFailedTransformCount()
    {
        return m_FailedTransformCount;
    }

    public void setFailedTransformCount(long failedTransformCount)
    {
        this.m_FailedTransformCount = failedTransformCount;
    }


    public void incrementFailedTransformCount(long additional)
    {
        m_FailedTransformCount += additional;
    }

    /**
     * Equality test
     */
    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (other instanceof DataCounts == false)
        {
            return false;
        }

        DataCounts that = (DataCounts)other;

        return this.m_BucketCount == that.m_BucketCount &&
                this.m_ProcessedRecordCount == that.m_ProcessedRecordCount &&
                this.m_ProcessedFieldCount == that.m_ProcessedFieldCount &&
                this.m_InputBytes == that.m_InputBytes &&
                this.m_InputFieldCount == that.m_InputFieldCount &&
                this.m_InputRecordCount == that.m_InputRecordCount &&
                this.m_InvalidDateCount == that.m_InvalidDateCount &&
                this.m_MissingFieldCount == that.m_MissingFieldCount &&
                this.m_OutOfOrderTimeStampCount == that.m_OutOfOrderTimeStampCount &&
                this.m_FailedTransformCount == that.m_FailedTransformCount;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(m_BucketCount, m_ProcessedRecordCount,
                m_ProcessedFieldCount, m_InputBytes, m_InputFieldCount,
                m_InputRecordCount, m_InvalidDateCount,
                m_MissingFieldCount, m_OutOfOrderTimeStampCount, m_FailedTransformCount);
    }
}
