
package org.elasticsearch.xpack.prelert.job.persistence;

/**
 * One time query builder for records. Sets default values for the following
 * parameters:
 * <ul>
 * <li>Skip- Skip the first N records. This parameter is for paging if not
 * required set to 0. Default = 0</li>
 * <li>Take- Take only this number of records. Default =
 * {@value DEFAULT_TAKE_SIZE}</li>
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
    public static final int DEFAULT_TAKE_SIZE = 100;

    private RecordsQuery recordsQuery = new RecordsQuery();

    public RecordsQueryBuilder skip(int skip)
    {
        recordsQuery.skip = skip;
        return this;
    }

    public RecordsQueryBuilder take(int take)
    {
        recordsQuery.take = take;
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
        private int skip = 0;
        private int take = DEFAULT_TAKE_SIZE;
        private boolean includeInterim = false;
        private String sortField;
        private boolean sortDescending = true;
        private double anomalyScoreFilter = 0.0d;
        private double normalizedProbability = 0.0d;
        private String partitionFieldValue;
        private String epochStart;
        private String epochEnd;


        public int getTake()
        {
            return take;
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

        public int getSkip()
        {
            return skip;
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


