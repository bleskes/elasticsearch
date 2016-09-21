
package org.elasticsearch.xpack.prelert.job;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.elasticsearch.xpack.prelert.job.persistence.serialisation.StorageSerialisable;
import org.elasticsearch.xpack.prelert.job.persistence.serialisation.StorageSerialiser;

import java.io.IOException;
import java.util.Date;
import java.util.Objects;

/**
 * Provide access to the C++ model memory usage numbers
 * for the Java process.
 */
@JsonIgnoreProperties({"modelSizeStatsId"})
public class ModelSizeStats implements StorageSerialisable
{
    /**
     * Field Names
     */
    public static final String MODEL_BYTES = "modelBytes";
    public static final String TOTAL_BY_FIELD_COUNT = "totalByFieldCount";
    public static final String TOTAL_OVER_FIELD_COUNT = "totalOverFieldCount";
    public static final String TOTAL_PARTITION_FIELD_COUNT = "totalPartitionFieldCount";
    public static final String BUCKET_ALLOCATION_FAILURES_COUNT = "bucketAllocationFailuresCount";
    public static final String MEMORY_STATUS = "memoryStatus";
    public static final String LOG_TIME = "logTime";
    public static final String BUCKET_TIME = "bucketTime";
    public static final String TIMESTAMP = "timestamp";

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
    public enum MemoryStatus { OK, SOFT_LIMIT, HARD_LIMIT }

    private long modelBytes;
    private long totalByFieldCount;
    private long totalOverFieldCount;
    private long totalPartitionFieldCount;
    private long bucketAllocationFailuresCount;
    private MemoryStatus memoryStatus;
    private Date timestamp;
    private Date logTime;
    private String id = TYPE;

    public ModelSizeStats()
    {
        this.modelBytes = 0;
        this.totalByFieldCount = 0;
        this.totalOverFieldCount = 0;
        this.totalPartitionFieldCount = 0;
        this.bucketAllocationFailuresCount = 0;
        this.memoryStatus = MemoryStatus.OK;
        this.logTime = new Date();
    }

    public String getModelSizeStatsId()
    {
        return this.id;
    }

    public void setModelSizeStatsId(String id)
    {
        this.id = id;
    }

    public void setModelBytes(long m)
    {
        this.modelBytes = m;
    }

    public long getModelBytes()
    {
        return this.modelBytes;
    }

    public void setTotalByFieldCount(long m)
    {
        this.totalByFieldCount = m;
    }

    public long getTotalByFieldCount()
    {
        return this.totalByFieldCount;
    }

    public void setTotalPartitionFieldCount(long m)
    {
        this.totalPartitionFieldCount = m;
    }

    public long getTotalPartitionFieldCount()
    {
        return this.totalPartitionFieldCount;
    }

    public void setTotalOverFieldCount(long m)
    {
        this.totalOverFieldCount = m;
    }

    public long getTotalOverFieldCount()
    {
        return this.totalOverFieldCount;
    }

    public void setBucketAllocationFailuresCount(long m)
    {
        this.bucketAllocationFailuresCount = m;
    }

    public long getBucketAllocationFailuresCount()
    {
        return this.bucketAllocationFailuresCount;
    }

    public void setMemoryStatus(String m)
    {
        if (m == null || m.isEmpty())
        {
            this.memoryStatus = MemoryStatus.OK;
        }
        else
        {
            this.memoryStatus = MemoryStatus.valueOf(m);
        }
    }

    public String getMemoryStatus()
    {
        return this.memoryStatus.name();
    }

    public Date getTimestamp()
    {
        return this.timestamp;
    }

    public void setTimestamp(Date d)
    {
        this.timestamp = d;
    }

    public Date getLogTime()
    {
        return this.logTime;
    }

    public void setLogTime(Date d)
    {
        this.logTime = d;
    }

    @Override
    public void serialise(StorageSerialiser serialiser) throws IOException
    {
        serialiser.addTimestamp(timestamp)
                .add(MODEL_BYTES, modelBytes)
                .add(TOTAL_BY_FIELD_COUNT, totalByFieldCount)
                .add(TOTAL_OVER_FIELD_COUNT, totalOverFieldCount)
                .add(TOTAL_PARTITION_FIELD_COUNT, totalPartitionFieldCount)
                .add(BUCKET_ALLOCATION_FAILURES_COUNT, bucketAllocationFailuresCount)
                .add(MEMORY_STATUS, memoryStatus)
                .add(LOG_TIME, logTime);
    }

    @Override
    public int hashCode()
    {
        // this.id excluded here as it is generated by the datastore
        return Objects.hash(this.modelBytes, totalByFieldCount, totalOverFieldCount, totalPartitionFieldCount,
                this.bucketAllocationFailuresCount, memoryStatus, timestamp, logTime);
    }

    /**
     * Compare all the fields.
     */
    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (other instanceof ModelSizeStats == false)
        {
            return false;
        }

        ModelSizeStats that = (ModelSizeStats) other;

        // this.id excluded here as it is generated by the datastore
        return this.modelBytes == that.modelBytes
                && this.totalByFieldCount == that.totalByFieldCount
                && this.totalOverFieldCount == that.totalOverFieldCount
                && this.totalPartitionFieldCount == that.totalPartitionFieldCount
                && this.bucketAllocationFailuresCount == that.bucketAllocationFailuresCount
                && Objects.equals(this.memoryStatus, that.memoryStatus)
                && Objects.equals(this.timestamp, that.timestamp)
                && Objects.equals(this.logTime, that.logTime);
    }
}
