
package org.elasticsearch.xpack.prelert.job.persistence;

import org.elasticsearch.xpack.prelert.utils.Strings;

import java.util.Objects;

/**
 * One time query builder for a single buckets.
 * <ul>
 *     <li>Timestamp (Required) - Timestamp of the bucket</li>
 *     <li>Expand- Include anomaly records. Default= false</li>
 *     <li>IncludeInterim- Include interim results. Default = false</li>
 *     <li>partitionValue Set the bucket's max normalised probabiltiy to this
 * partiton field value's max normalised probability. Default = null</li>
 * </ul>
 */
public final class BucketQueryBuilder
{
    public static int DEFAULT_TAKE_SIZE = 100;

    private BucketQuery bucketQuery;

    public BucketQueryBuilder(long timestampMillis)
    {
        bucketQuery = new BucketQuery(timestampMillis);
    }

    public BucketQueryBuilder expand(boolean expand)
    {
        bucketQuery.expand = expand;
        return this;
    }

    public BucketQueryBuilder includeInterim(boolean include)
    {
        bucketQuery.includeInterim = include;
        return this;
    }

    /**
     * partitionValue must be non null and not empty else it
     * is not set
     * @param partitionValue
     * @return
     */
    public BucketQueryBuilder partitionValue(String partitionValue)
    {
        if (!Strings.isNullOrEmpty(partitionValue))
        {
            bucketQuery.partitionValue = partitionValue;
        }
        return this;
    }

    public BucketQueryBuilder.BucketQuery build()
    {
        return bucketQuery;
    }

    public class BucketQuery
    {
        private long timestampMillis;
        private boolean expand =  false;
        private boolean includeInterim = false;
        private String partitionValue = null;

        public BucketQuery(long timestamp)
        {
            this.timestampMillis = timestamp;
        }

        public long getTimestamp()
        {
            return timestampMillis;
        }

        public boolean isIncludeInterim()
        {
            return includeInterim;
        }

        public boolean isExpand()
        {
            return expand;
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
            return Objects.hash(timestampMillis, expand, includeInterim,
                    partitionValue);
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

            BucketQuery other = (BucketQuery) obj;
            return this.timestampMillis == other.timestampMillis &&
                   this.expand == other.expand &&
                   this.includeInterim == other.includeInterim &&
                   this.partitionValue == other.partitionValue;
        }

    }
}
