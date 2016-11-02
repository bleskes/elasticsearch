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

import org.elasticsearch.xpack.prelert.job.results.Bucket;
import org.elasticsearch.common.Strings;

import java.util.Objects;

/**
 * One time query builder for buckets.
 * <ul>
 * <li>Skip- Skip the first N Buckets. This parameter is for paging if not
 * required set to 0. Default = 0</li>
 * <li>Take- Take only this number of Buckets. Default =
 * {@value DEFAULT_TAKE_SIZE}</li>
 * <li>Expand- Include anomaly records. Default= false</li>
 * <li>IncludeInterim- Include interim results. Default = false</li>
 * <li>anomalyScoreThreshold- Return only buckets with an anomalyScore &gt;=
 * this value. Default = 0.0</li>
 * <li>normalizedProbabilityThreshold- Return only buckets with a
 * maxNormalizedProbability &gt;= this value. Default = 0.0</li>
 * <li>epochStart- The start bucket time. A bucket with this timestamp will be
 * included in the results. If 0 all buckets up to <code>endEpochMs</code> are
 * returned. Default = -1</li>
 * <li>epochEnd- The end bucket timestamp buckets up to but NOT including this
 * timestamp are returned. If 0 all buckets from <code>startEpochMs</code> are
 * returned. Default = -1</li>
 * <li>partitionValue Set the bucket's max normalised probability to this
 * partition field value's max normalised probability. Default = null</li>
 * </ul>
 */
public final class BucketsQueryBuilder {
    public static final int DEFAULT_TAKE_SIZE = 100;

    private BucketsQuery bucketsQuery = new BucketsQuery();

    public BucketsQueryBuilder skip(int skip) {
        bucketsQuery.skip = skip;
        return this;
    }

    public BucketsQueryBuilder take(int take) {
        bucketsQuery.take = take;
        return this;
    }

    public BucketsQueryBuilder expand(boolean expand) {
        bucketsQuery.expand = expand;
        return this;
    }

    public BucketsQueryBuilder includeInterim(boolean include) {
        bucketsQuery.includeInterim = include;
        return this;
    }

    public BucketsQueryBuilder anomalyScoreThreshold(Double anomalyScoreFilter) {
        bucketsQuery.anomalyScoreFilter = anomalyScoreFilter;
        return this;
    }

    public BucketsQueryBuilder normalizedProbabilityThreshold(Double normalizedProbability) {
        bucketsQuery.normalizedProbability = normalizedProbability;
        return this;
    }

    /**
     * @param partitionValue Not set if null or empty
     */
    public BucketsQueryBuilder partitionValue(String partitionValue) {
        if (!Strings.isNullOrEmpty(partitionValue)) {
            bucketsQuery.partitionValue = partitionValue;
        }
        return this;
    }

    public BucketsQueryBuilder sortField(String sortField) {
        bucketsQuery.sortField = sortField;
        return this;
    }

    public BucketsQueryBuilder sortDescending(boolean sortDescending) {
        bucketsQuery.sortDescending = sortDescending;
        return this;
    }

    /**
     * If startTime &lt;= 0 the parameter is not set
     */
    public BucketsQueryBuilder epochStart(String startTime) {
        bucketsQuery.epochStart = startTime;
        return this;
    }

    /**
     * If endTime &lt;= 0 the parameter is not set
     */
    public BucketsQueryBuilder epochEnd(String endTime) {
        bucketsQuery.epochEnd = endTime;
        return this;
    }

    public BucketsQueryBuilder.BucketsQuery build() {
        return bucketsQuery;
    }

    public void clear() {
        bucketsQuery = new BucketsQueryBuilder.BucketsQuery();
    }


    public class BucketsQuery {
        private int skip = 0;
        private int take = DEFAULT_TAKE_SIZE;
        private boolean expand = false;
        private boolean includeInterim = false;
        private double anomalyScoreFilter = 0.0d;
        private double normalizedProbability = 0.0d;
        private String epochStart;
        private String epochEnd;
        private String partitionValue = null;
        private String sortField = Bucket.TIMESTAMP.getPreferredName();
        private boolean sortDescending = false;

        public int getSkip() {
            return skip;
        }

        public int getTake() {
            return take;
        }

        public boolean isExpand() {
            return expand;
        }

        public boolean isIncludeInterim() {
            return includeInterim;
        }

        public double getAnomalyScoreFilter() {
            return anomalyScoreFilter;
        }

        public double getNormalizedProbability() {
            return normalizedProbability;
        }

        public String getEpochStart() {
            return epochStart;
        }

        public String getEpochEnd() {
            return epochEnd;
        }

        /**
         * @return Null if not set
         */
        public String getPartitionValue() {
            return partitionValue;
        }

        public String getSortField() {
            return sortField;
        }

        public boolean isSortDescending() {
            return sortDescending;
        }

        @Override
        public int hashCode() {
            return Objects.hash(skip, take, expand, includeInterim, anomalyScoreFilter, normalizedProbability, epochStart, epochEnd,
                    partitionValue, sortField, sortDescending);
        }


        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }

            BucketsQuery other = (BucketsQuery) obj;
            return Objects.equals(skip, other.skip) &&
                    Objects.equals(take, other.take) &&
                    Objects.equals(expand, other.expand) &&
                    Objects.equals(includeInterim, other.includeInterim) &&
                    Objects.equals(epochStart, other.epochStart) &&
                    Objects.equals(epochStart, other.epochStart) &&
                    Objects.equals(anomalyScoreFilter, other.anomalyScoreFilter) &&
                    Objects.equals(normalizedProbability, other.normalizedProbability) &&
                    Objects.equals(partitionValue, other.partitionValue) &&
                    Objects.equals(sortField, other.sortField) &&
                    this.sortDescending == other.sortDescending;
        }

    }
}
