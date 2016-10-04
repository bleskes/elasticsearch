
package org.elasticsearch.xpack.prelert.job;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.elasticsearch.xpack.prelert.job.persistence.serialisation.StorageSerialisable;
import org.elasticsearch.xpack.prelert.job.persistence.serialisation.StorageSerialiser;
import org.elasticsearch.xpack.prelert.job.quantiles.Quantiles;

import java.io.IOException;
import java.util.Date;
import java.util.Objects;


/**
 * ModelSnapshot Result POJO
 */
@JsonInclude(Include.NON_NULL)
public class ModelSnapshot implements StorageSerialisable
{
    /**
     * Field Names
     */
    public static final String TIMESTAMP = "timestamp";
    public static final String DESCRIPTION = "description";
    public static final String RESTORE_PRIORITY = "restorePriority";
    public static final String SNAPSHOT_ID = "snapshotId";
    public static final String SNAPSHOT_DOC_COUNT = "snapshotDocCount";
    public static final String LATEST_RECORD_TIME = "latestRecordTimeStamp";
    public static final String LATEST_RESULT_TIME = "latestResultTimeStamp";

    /**
     * Elasticsearch type
     */
    public static final String TYPE = "modelSnapshot";

    private Date m_Timestamp;
    private String m_Description;
    private long m_RestorePriority;
    private String m_SnapshotId;
    private int m_SnapshotDocCount;
    private ModelSizeStats m_ModelSizeStats;
    private Date m_LatestRecordTimeStamp;
    private Date m_LatestResultTimeStamp;
    private Quantiles m_Quantiles;

    public Date getTimestamp()
    {
        return m_Timestamp;
    }

    public void setTimestamp(Date timestamp)
    {
        m_Timestamp = timestamp;
    }

    public String getDescription()
    {
        return m_Description;
    }

    public void setDescription(String description)
    {
        m_Description = description;
    }

    public long getRestorePriority()
    {
        return m_RestorePriority;
    }

    public void setRestorePriority(long restorePriority)
    {
        m_RestorePriority = restorePriority;
    }

    public String getSnapshotId()
    {
        return m_SnapshotId;
    }

    public void setSnapshotId(String snapshotId)
    {
        m_SnapshotId = snapshotId;
    }

    public int getSnapshotDocCount()
    {
        return m_SnapshotDocCount;
    }

    public void setSnapshotDocCount(int snapshotDocCount)
    {
        m_SnapshotDocCount = snapshotDocCount;
    }

    public ModelSizeStats getModelSizeStats()
    {
        return m_ModelSizeStats;
    }

    public void setModelSizeStats(ModelSizeStats modelSizeStats)
    {
        m_ModelSizeStats = modelSizeStats;
    }

    public Quantiles getQuantiles()
    {
        return m_Quantiles;
    }

    public void setQuantiles(Quantiles q)
    {
        m_Quantiles = q;
    }

    public Date getLatestRecordTimeStamp()
    {
        return m_LatestRecordTimeStamp;
    }

    public void setLatestRecordTimeStamp(Date latestRecordTimeStamp)
    {
        m_LatestRecordTimeStamp = latestRecordTimeStamp;
    }

    public Date getLatestResultTimeStamp()
    {
        return m_LatestResultTimeStamp;
    }

    public void setLatestResultTimeStamp(Date latestResultTimeStamp)
    {
        m_LatestResultTimeStamp = latestResultTimeStamp;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(m_Timestamp, m_Description, m_RestorePriority, m_SnapshotId, m_Quantiles,
                m_SnapshotDocCount, m_ModelSizeStats, m_LatestRecordTimeStamp, m_LatestResultTimeStamp);
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

        if (other instanceof ModelSnapshot == false)
        {
            return false;
        }

        ModelSnapshot that = (ModelSnapshot) other;

        return Objects.equals(this.m_Timestamp, that.m_Timestamp)
                && Objects.equals(this.m_Description, that.m_Description)
                && this.m_RestorePriority == that.m_RestorePriority
                && Objects.equals(this.m_SnapshotId, that.m_SnapshotId)
                && this.m_SnapshotDocCount == that.m_SnapshotDocCount
                && Objects.equals(this.m_ModelSizeStats, that.m_ModelSizeStats)
                && Objects.equals(this.m_Quantiles,  that.m_Quantiles)
                && Objects.equals(this.m_LatestRecordTimeStamp, that.m_LatestRecordTimeStamp)
                && Objects.equals(this.m_LatestResultTimeStamp, that.m_LatestResultTimeStamp);
    }

    @Override
    public void serialise(StorageSerialiser serialiser) throws IOException
    {
        serialiser.addTimestamp(m_Timestamp)
                  .add(DESCRIPTION, m_Description)
                  .add(RESTORE_PRIORITY, m_RestorePriority)
                  .add(SNAPSHOT_ID, m_SnapshotId)
                  .add(SNAPSHOT_DOC_COUNT, m_SnapshotDocCount);

        if (m_ModelSizeStats != null)
        {
            serialiser.startObject(ModelSizeStats.TYPE);
            m_ModelSizeStats.serialise(serialiser);
            serialiser.endObject();
        }
        if (m_Quantiles != null)
        {
            serialiser.startObject(Quantiles.TYPE);
            m_Quantiles.serialise(serialiser);
            serialiser.endObject();
        }
        if (m_LatestRecordTimeStamp != null)
        {
            serialiser.add(LATEST_RECORD_TIME, m_LatestRecordTimeStamp);
        }
        if (m_LatestResultTimeStamp != null)
        {
            serialiser.add(LATEST_RESULT_TIME, m_LatestResultTimeStamp);
        }
    }
}
