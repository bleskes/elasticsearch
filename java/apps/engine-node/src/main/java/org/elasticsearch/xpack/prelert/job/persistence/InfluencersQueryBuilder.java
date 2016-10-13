/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated. All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elasticsearch Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Elasticsearch Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elasticsearch Incorporated.
 */
package org.elasticsearch.xpack.prelert.job.persistence;

import org.elasticsearch.xpack.prelert.job.results.Influencer;

import java.util.Objects;

/**
 * One time query builder for buckets.
 * <ul>
 * <li>Skip- Skip the first N Buckets. This parameter is for paging
 * if not required set to 0. Default = 0</li>
 * <li>Take- Take only this number of Buckets. Default = {@value DEFAULT_TAKE_SIZE}</li>
 * <li>Expand- Include anomaly records. Default= false</li>
 * <li>IncludeInterim- Include interim results. Default = false</li>
 * <li>anomalyScoreThreshold- Return only buckets with an anomalyScore >=
 * this value. Default = 0.0</li>
 * <li>normalizedProbabilityThreshold- Return only buckets with a maxNormalizedProbability >=
 * this value. Default = 0.0</li>
 * <li>epochStart- The start bucket time. A bucket with this timestamp will be
 * included in the results. If 0 all buckets up to <code>endEpochMs</code>
 * are returned. Default = -1</li>
 * <li>epochEnd- The end bucket timestamp buckets up to but NOT including this
 * timestamp are returned. If 0 all buckets from <code>startEpochMs</code>
 * are returned. Default = -1</li>
 * <li>partitionValue Set the bucket's max normalised probability to this
 * partition field value's max normalised probability. Default = null</li>
 * </ul>
 */
public final class InfluencersQueryBuilder {
    public static int DEFAULT_TAKE_SIZE = 100;

    private InfluencersQuery influencersQuery = new InfluencersQuery();

    public InfluencersQueryBuilder skip(int skip) {
        influencersQuery.skip = skip;
        return this;
    }

    public InfluencersQueryBuilder take(int take) {
        influencersQuery.take = take;
        return this;
    }

    public InfluencersQueryBuilder includeInterim(boolean include) {
        influencersQuery.includeInterim = include;
        return this;
    }

    public InfluencersQueryBuilder anomalyScoreThreshold(Double anomalyScoreFilter) {
        influencersQuery.anomalyScoreFilter = anomalyScoreFilter;
        return this;
    }

    public InfluencersQueryBuilder sortField(String sortField) {
        influencersQuery.sortField = sortField;
        return this;
    }

    public InfluencersQueryBuilder sortDescending(boolean sortDescending) {
        influencersQuery.sortDescending = sortDescending;
        return this;
    }

    /**
     * If startTime <= 0 the parameter is not set
     *
     * @param startTime
     * @return
     */
    public InfluencersQueryBuilder epochStart(String startTime) {
        influencersQuery.epochStart = startTime;
        return this;
    }

    /**
     * If endTime <= 0 the parameter is not set
     *
     * @param endTime
     * @return
     */
    public InfluencersQueryBuilder epochEnd(String endTime) {
        influencersQuery.epochEnd = endTime;
        return this;
    }

    public InfluencersQueryBuilder.InfluencersQuery build() {
        return influencersQuery;
    }

    public void clear() {
        influencersQuery = new InfluencersQueryBuilder.InfluencersQuery();
    }


    public class InfluencersQuery {
        private int skip = 0;
        private int take = DEFAULT_TAKE_SIZE;
        private boolean includeInterim = false;
        private double anomalyScoreFilter = 0.0d;
        private String epochStart;
        private String epochEnd;
        private String sortField = Influencer.ANOMALY_SCORE;
        private boolean sortDescending = false;

        public int getSkip() {
            return skip;
        }

        public int getTake() {
            return take;
        }

        public boolean isIncludeInterim() {
            return includeInterim;
        }

        public double getAnomalyScoreFilter() {
            return anomalyScoreFilter;
        }

        public String getEpochStart() {
            return epochStart;
        }

        public String getEpochEnd() {
            return epochEnd;
        }

        public String getSortField() {
            return sortField;
        }

        public boolean isSortDescending() {
            return sortDescending;
        }

        @Override
        public int hashCode() {
            return Objects.hash(skip, take, includeInterim, anomalyScoreFilter, epochStart, epochEnd,
                    sortField, sortDescending);
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

            InfluencersQuery other = (InfluencersQuery) obj;
            return Objects.equals(skip, other.skip) &&
                    Objects.equals(take, other.take) &&
                    Objects.equals(includeInterim, other.includeInterim) &&
                    Objects.equals(epochStart, other.epochStart) &&
                    Objects.equals(epochStart, other.epochStart) &&
                    Objects.equals(anomalyScoreFilter, other.anomalyScoreFilter) &&
                    Objects.equals(sortField, other.sortField) &&
                    this.sortDescending == other.sortDescending;
        }

    }
}
