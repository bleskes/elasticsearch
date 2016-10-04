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

import org.elasticsearch.xpack.prelert.utils.Strings;

import java.util.Objects;

/**
 * One time query builder for a single buckets.
 * <ul>
 * <li>Timestamp (Required) - Timestamp of the bucket</li>
 * <li>Expand- Include anomaly records. Default= false</li>
 * <li>IncludeInterim- Include interim results. Default = false</li>
 * <li>partitionValue Set the bucket's max normalised probabiltiy to this
 * partiton field value's max normalised probability. Default = null</li>
 * </ul>
 */
public final class BucketQueryBuilder {
    public static int DEFAULT_TAKE_SIZE = 100;

    private BucketQuery bucketQuery;

    public BucketQueryBuilder(String timestamp) {
        bucketQuery = new BucketQuery(timestamp);
    }

    public BucketQueryBuilder expand(boolean expand) {
        bucketQuery.expand = expand;
        return this;
    }

    public BucketQueryBuilder includeInterim(boolean include) {
        bucketQuery.includeInterim = include;
        return this;
    }

    /**
     * partitionValue must be non null and not empty else it
     * is not set
     *
     * @param partitionValue
     * @return
     */
    public BucketQueryBuilder partitionValue(String partitionValue) {
        if (!Strings.isNullOrEmpty(partitionValue)) {
            bucketQuery.partitionValue = partitionValue;
        }
        return this;
    }

    public BucketQueryBuilder.BucketQuery build() {
        return bucketQuery;
    }

    public class BucketQuery {
        private String timestamp;
        private boolean expand = false;
        private boolean includeInterim = false;
        private String partitionValue = null;

        public BucketQuery(String timestamp) {
            this.timestamp = timestamp;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public boolean isIncludeInterim() {
            return includeInterim;
        }

        public boolean isExpand() {
            return expand;
        }

        /**
         * @return Null if not set
         */
        public String getPartitionValue() {
            return partitionValue;
        }

        @Override
        public int hashCode() {
            return Objects.hash(timestamp, expand, includeInterim,
                    partitionValue);
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

            BucketQuery other = (BucketQuery) obj;
            return Objects.equals(timestamp, other.timestamp) &&
                    Objects.equals(expand, other.expand) &&
                    Objects.equals(includeInterim, other.includeInterim) &&
                    Objects.equals(partitionValue, other.partitionValue);
        }

    }
}
