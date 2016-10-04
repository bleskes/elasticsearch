
package org.elasticsearch.xpack.prelert.job.persistence;

import org.elasticsearch.xpack.prelert.utils.Strings;

import java.util.Objects;

/**
 * One time query builder for buckets.
 * <ul>
 *     <li>Skip- Skip the first N Buckets. This parameter is for paging
 * if not required set to 0. Default = 0</li>
 *     <li>Take- Take only this number of Buckets. Default = {@value DEFAULT_TAKE_SIZE}</li>
 *     <li>Expand- Include anomaly records. Default= false</li>
 *     <li>IncludeInterim- Include interim results. Default = false</li>
 *     <li>anomalyScoreThreshold- Return only buckets with an anomalyScore >=
 * this value. Default = 0.0</li>
 *     <li>normalizedProbabilityThreshold- Return only buckets with a maxNormalizedProbability >=
 * this value. Default = 0.0</li>
 *     <li>epochStart- The start bucket time. A bucket with this timestamp will be
 * included in the results. If 0 all buckets up to <code>endEpochMs</code>
 * are returned. Default = -1</li>
 *     <li>epochEnd- The end bucket timestamp buckets up to but NOT including this
 * timestamp are returned. If 0 all buckets from <code>startEpochMs</code>
 * are returned. Default = -1</li>
 *     <li>partitionValue Set the bucket's max normalised probability to this
 * partition field value's max normalised probability. Default = null</li>
 * </ul>
 */
public final class BucketsQueryBuilder
{
    public static int DEFAULT_TAKE_SIZE = 100;

    private BucketsQuery bucketsQuery = new BucketsQuery();

    public BucketsQueryBuilder skip(int skip)
    {
        bucketsQuery.skip = skip;
        return this;
    }

    public BucketsQueryBuilder take(int take)
    {
        bucketsQuery.take = take;
        return this;
    }

    public BucketsQueryBuilder expand(boolean expand)
    {
        bucketsQuery.expand = expand;
        return this;
    }

    public BucketsQueryBuilder includeInterim(boolean include)
    {
        bucketsQuery.includeInterim = include;
        return this;
    }

    public BucketsQueryBuilder anomalyScoreThreshold(Double anomalyScoreFilter)
    {
        bucketsQuery.anomalyScoreFilter = anomalyScoreFilter;
        return this;
    }

    public BucketsQueryBuilder normalizedProbabilityThreshold(Double normalizedProbability)
    {
        bucketsQuery.normalizedProbability = normalizedProbability;
        return this;
    }

    /**
     * @param partitionValue Not set if null or empty
     * @return
     */
    public BucketsQueryBuilder partitionValue(String partitionValue)
    {
        if (!Strings.isNullOrEmpty(partitionValue))
        {
            bucketsQuery.partitionValue = partitionValue;
        }
        return this;
    }


    /**
     * If startTime <= 0 the parameter is not set
     * @param startTime
     * @return
     */
    public BucketsQueryBuilder epochStart(long startTime)
    {
        if (startTime > 0)
        {
            bucketsQuery.epochStart = startTime;
        }
        return this;
    }

    /**
     * If endTime <= 0 the parameter is not set
     * @param endTime
     * @return
     */
    public BucketsQueryBuilder epochEnd(long endTime)
    {
        if (endTime > 0)
        {
            bucketsQuery.epochEnd = endTime;
        }
        return this;
    }

    public BucketsQueryBuilder.BucketsQuery build()
    {
        return bucketsQuery;
    }

    public void clear()
    {
        bucketsQuery = new BucketsQueryBuilder.BucketsQuery();
    }


    public class BucketsQuery
    {
        private int skip = 0;
        private int take = DEFAULT_TAKE_SIZE;
        private boolean expand =  false;
        private boolean includeInterim = false;
        private double anomalyScoreFilter = 0.0d;
        private double normalizedProbability = 0.0d;
        private long epochStart = -1;
        private long epochEnd = -1;
        private String partitionValue = null;

        public int getSkip()
        {
            return skip;
        }

        public int getTake()
        {
            return take;
        }

        public boolean isExpand()
        {
            return expand;
        }

        public boolean isIncludeInterim()
        {
            return includeInterim;
        }

        public double getAnomalyScoreFilter()
        {
            return anomalyScoreFilter;
        }

        public double getNormalizedProbability()
        {
            return normalizedProbability;
        }

        public long getEpochStart()
        {
            return epochStart;
        }

        public long getEpochEnd()
        {
            return epochEnd;
        }

        /**
         * @return Null if not set
         */
        public String getPartitionValue()
        {
            return partitionValue;
        }

        @Override
        public int hashCode() {
            return Objects.hash(skip, take, expand, includeInterim,
                    anomalyScoreFilter, normalizedProbability,
                    epochStart, epochEnd, partitionValue);
        }


        @Override
        public boolean equals(Object obj) {
            if (this == obj)
            {
                return true;
            }
            if (obj == null)
            {
                return false;
            }
            if (getClass() != obj.getClass())
            {
                return false;
            }

            BucketsQuery other = (BucketsQuery) obj;
            return this.skip == other.skip &&
                   this.take == other.take &&
                   this.expand == other.expand &&
                   this.includeInterim == other.includeInterim &&
                   this.epochStart == other.epochStart &&
                   this.epochStart == other.epochStart &&
                   this.anomalyScoreFilter == other.anomalyScoreFilter &&
                   this.normalizedProbability == other.normalizedProbability &&
                   this.partitionValue == other.partitionValue;
        }

    }
}
