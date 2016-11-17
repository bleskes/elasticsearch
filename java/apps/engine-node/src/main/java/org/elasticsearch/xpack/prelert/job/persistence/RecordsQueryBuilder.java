/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.prelert.job.persistence;

/**
 * One time query builder for records. Sets default values for the following
 * parameters:
 * <ul>
 * <li>From- Skip the first N records. This parameter is for paging if not
 * required set to 0. Default = 0</li>
 * <li>Size- Take only this number of records. Default =
 * {@value DEFAULT_SIZE}</li>
 * <li>IncludeInterim- Include interim results. Default = false</li>
 * <li>SortField- The field to sort results by if <code>null</code> no sort is
 * applied. Default = null</li>
 * <li>SortDescending- Sort in descending order. Default = true</li>
 * <li>anomalyScoreThreshold- Return only buckets with an anomalyScore &gt;=
 * this value. Default = 0.0</li>
 * <li>normalizedProbabilityThreshold. Return only buckets with a
 * maxNormalizedProbability &gt;= this value. Default = 0.0</li>
 * <li>epochStart- The start bucket time. A bucket with this timestamp will be
 * included in the results. If 0 all buckets up to <code>endEpochMs</code> are
 * returned. Default = -1</li>
 * <li>epochEnd- The end bucket timestamp buckets up to but NOT including this
 * timestamp are returned. If 0 all buckets from <code>startEpochMs</code> are
 * returned. Default = -1</li>
 * </ul>
 */
public final class RecordsQueryBuilder
{
    public static final int DEFAULT_SIZE = 100;

    private RecordsQuery recordsQuery = new RecordsQuery();

    public RecordsQueryBuilder from(int from)
    {
        recordsQuery.from = from;
        return this;
    }

    public RecordsQueryBuilder size(int size)
    {
        recordsQuery.size = size;
        return this;
    }

    public RecordsQueryBuilder epochStart(String startTime)
    {
        recordsQuery.epochStart = startTime;
        return this;
    }

    public RecordsQueryBuilder epochEnd(String endTime)
    {
        recordsQuery.epochEnd = endTime;
        return this;
    }

    public RecordsQueryBuilder includeInterim(boolean include)
    {
        recordsQuery.includeInterim = include;
        return this;
    }

    public RecordsQueryBuilder sortField(String fieldname)
    {
        recordsQuery.sortField = fieldname;
        return this;
    }

    public RecordsQueryBuilder sortDescending(boolean sortDescending)
    {
        recordsQuery.sortDescending = sortDescending;
        return this;
    }

    public RecordsQueryBuilder anomalyScoreThreshold(double anomalyScoreFilter)
    {
        recordsQuery.anomalyScoreFilter = anomalyScoreFilter;
        return this;
    }

    public RecordsQueryBuilder normalizedProbability(double normalizedProbability)
    {
        recordsQuery.normalizedProbability = normalizedProbability;
        return this;
    }

    public RecordsQueryBuilder partitionFieldValue(String partitionFieldValue)
    {
        recordsQuery.partitionFieldValue = partitionFieldValue;
        return this;
    }

    public RecordsQuery build()
    {
        return recordsQuery;
    }

    public void clear()
    {
        recordsQuery = new RecordsQuery();
    }

    public class RecordsQuery
    {
        private int from = 0;
        private int size = DEFAULT_SIZE;
        private boolean includeInterim = false;
        private String sortField;
        private boolean sortDescending = true;
        private double anomalyScoreFilter = 0.0d;
        private double normalizedProbability = 0.0d;
        private String partitionFieldValue;
        private String epochStart;
        private String epochEnd;


        public int getSize()
        {
            return size;
        }

        public boolean isIncludeInterim()
        {
            return includeInterim;
        }

        public String getSortField()
        {
            return sortField;
        }

        public boolean isSortDescending()
        {
            return sortDescending;
        }

        public double getAnomalyScoreThreshold()
        {
            return anomalyScoreFilter;
        }

        public double getNormalizedProbabilityThreshold()
        {
            return normalizedProbability;
        }

        public String getPartitionFieldValue()
        {
            return partitionFieldValue;
        }

        public int getFrom()
        {
            return from;
        }

        public String getEpochStart()
        {
            return epochStart;
        }

        public String getEpochEnd()
        {
            return epochEnd;
        }
    }
}


