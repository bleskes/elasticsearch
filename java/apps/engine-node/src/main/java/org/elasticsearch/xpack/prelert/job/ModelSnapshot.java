
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
public class ModelSnapshot implements StorageSerialisable {
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

    private Date timestamp;
    private String description;
    private long restorePriority;
    private String snapshotId;
    private int snapshotDocCount;
    private ModelSizeStats modelSizeStats;
    private Date latestRecordTimeStamp;
    private Date latestResultTimeStamp;
    private Quantiles quantiles;

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getRestorePriority() {
        return restorePriority;
    }

    public void setRestorePriority(long restorePriority) {
        this.restorePriority = restorePriority;
    }

    public String getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(String snapshotId) {
        this.snapshotId = snapshotId;
    }

    public int getSnapshotDocCount() {
        return snapshotDocCount;
    }

    public void setSnapshotDocCount(int snapshotDocCount) {
        this.snapshotDocCount = snapshotDocCount;
    }

    public ModelSizeStats getModelSizeStats() {
        return modelSizeStats;
    }

    public void setModelSizeStats(ModelSizeStats modelSizeStats) {
        this.modelSizeStats = modelSizeStats;
    }

    public Quantiles getQuantiles() {
        return quantiles;
    }

    public void setQuantiles(Quantiles q) {
        quantiles = q;
    }

    public Date getLatestRecordTimeStamp() {
        return latestRecordTimeStamp;
    }

    public void setLatestRecordTimeStamp(Date latestRecordTimeStamp) {
        this.latestRecordTimeStamp = latestRecordTimeStamp;
    }

    public Date getLatestResultTimeStamp() {
        return latestResultTimeStamp;
    }

    public void setLatestResultTimeStamp(Date latestResultTimeStamp) {
        this.latestResultTimeStamp = latestResultTimeStamp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, description, restorePriority, snapshotId, quantiles,
                snapshotDocCount, modelSizeStats, latestRecordTimeStamp, latestResultTimeStamp);
    }

    /**
     * Compare all the fields.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other instanceof ModelSnapshot == false) {
            return false;
        }

        ModelSnapshot that = (ModelSnapshot) other;

        return Objects.equals(this.timestamp, that.timestamp)
                && Objects.equals(this.description, that.description)
                && this.restorePriority == that.restorePriority
                && Objects.equals(this.snapshotId, that.snapshotId)
                && this.snapshotDocCount == that.snapshotDocCount
                && Objects.equals(this.modelSizeStats, that.modelSizeStats)
                && Objects.equals(this.quantiles, that.quantiles)
                && Objects.equals(this.latestRecordTimeStamp, that.latestRecordTimeStamp)
                && Objects.equals(this.latestResultTimeStamp, that.latestResultTimeStamp);
    }

    @Override
    public void serialise(StorageSerialiser serialiser) throws IOException {
        serialiser.addTimestamp(timestamp)
                .add(DESCRIPTION, description)
                .add(RESTORE_PRIORITY, restorePriority)
                .add(SNAPSHOT_ID, snapshotId)
                .add(SNAPSHOT_DOC_COUNT, snapshotDocCount);

        if (modelSizeStats != null) {
            serialiser.startObject(ModelSizeStats.TYPE);
            modelSizeStats.serialise(serialiser);
            serialiser.endObject();
        }
        if (quantiles != null) {
            serialiser.startObject(Quantiles.TYPE);
            quantiles.serialise(serialiser);
            serialiser.endObject();
        }
        if (latestRecordTimeStamp != null) {
            serialiser.add(LATEST_RECORD_TIME, latestRecordTimeStamp);
        }
        if (latestResultTimeStamp != null) {
            serialiser.add(LATEST_RESULT_TIME, latestResultTimeStamp);
        }
    }
}
