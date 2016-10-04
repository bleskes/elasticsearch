
package org.elasticsearch.xpack.prelert.job.results;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.elasticsearch.xpack.prelert.job.persistence.serialisation.StorageSerialisable;
import org.elasticsearch.xpack.prelert.job.persistence.serialisation.StorageSerialiser;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Bucket Result POJO
 */
@JsonIgnoreProperties({"epoch", "normalisable", "id", "perPartitionMaxProbability"
                    /*, "partitionScores"*/})
@JsonInclude(Include.NON_NULL)
public class Bucket implements StorageSerialisable
{
    /*
     * Field Names
     */
    public static final String TIMESTAMP = "timestamp";
    public static final String ANOMALY_SCORE = "anomalyScore";
    public static final String INITIAL_ANOMALY_SCORE = "initialAnomalyScore";
    public static final String MAX_NORMALIZED_PROBABILITY = "maxNormalizedProbability";
    public static final String IS_INTERIM = "isInterim";
    public static final String RECORD_COUNT = "recordCount";
    public static final String EVENT_COUNT = "eventCount";
    public static final String RECORDS = "records";
    public static final String BUCKET_INFLUENCERS = "bucketInfluencers";
    public static final String INFLUENCERS = "influencers";
    public static final String BUCKET_SPAN = "bucketSpan";
    public static final String PROCESSING_TIME_MS = "processingTimeMs";
    public static final String PARTITION_SCORES = "partitionScores";

    /**
     * Elasticsearch type
     */
    public static final String TYPE = "bucket";

    private String id;
    private Date timestamp;
    private double anomalyScore;
    private long bucketSpan;

    private double initialAnomalyScore;

    private double maxNormalizedProbability;
    private int recordCount;
    private List<AnomalyRecord> records;
    private long eventCount;
    private boolean isInterim;
    private boolean hadBigNormalisedUpdate;
    private List<BucketInfluencer> bucketInfluencers;
    private List<Influencer> influencers;
    private long processingTimeMs;
    private Map<String, Double> perPartitionMaxProbability;
    private List<PartitionScore> partitionScores;

    public Bucket()
    {
        records = Collections.emptyList();
        influencers = Collections.emptyList();
        partitionScores = Collections.emptyList();
        bucketInfluencers = new ArrayList<>();
        setPerPartitionMaxProbability(Collections.emptyMap());
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * Timestamp expressed in seconds since the epoch (rather than Java's
     * convention of milliseconds).
     */
    public long getEpoch()
    {
        return timestamp.getTime() / 1000;
    }

    public Date getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp(Date timestamp)
    {
        this.timestamp = timestamp;
    }

    /**
     * Bucketspan expressed in seconds
     */
    public long getBucketSpan()
    {
        return bucketSpan;
    }

    /**
     * Bucketspan expressed in seconds
     */
    public void setBucketSpan(long bucketSpan)
    {
        this.bucketSpan = bucketSpan;
    }

    public double getAnomalyScore()
    {
        return anomalyScore;
    }

    public void setAnomalyScore(double anomalyScore)
    {
        this.anomalyScore = anomalyScore;
    }

    public double getInitialAnomalyScore()
    {
        return initialAnomalyScore;
    }

    public void setInitialAnomalyScore(double influenceScore)
    {
        this.initialAnomalyScore = influenceScore;
    }

    public double getMaxNormalizedProbability()
    {
        return maxNormalizedProbability;
    }

    public void setMaxNormalizedProbability(double maxNormalizedProbability)
    {
        this.maxNormalizedProbability = maxNormalizedProbability;
    }

    public int getRecordCount()
    {
        return recordCount;
    }

    public void setRecordCount(int recordCount)
    {
        this.recordCount = recordCount;
    }

    /**
     * Get all the anomaly records associated with this bucket
     * @return All the anomaly records
     */
    public List<AnomalyRecord> getRecords()
    {
        return records;
    }

    public void setRecords(List<AnomalyRecord> records)
    {
        this.records = records;
    }

    /**
     * The number of records (events) actually processed
     * in this bucket.
     * @return
     */
    public long getEventCount()
    {
        return eventCount;
    }

    public void setEventCount(long value)
    {
        eventCount = value;
    }

    @JsonProperty("isInterim")
    public boolean isInterim()
    {
        return isInterim;
    }

    @JsonProperty("isInterim")
    public void setInterim(boolean isInterim)
    {
        this.isInterim = isInterim;
    }

    public long getProcessingTimeMs()
    {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(long timeMs)
    {
        processingTimeMs = timeMs;
    }

    public List<Influencer> getInfluencers()
    {
        return influencers;
    }

    public void setInfluencers(List<Influencer> influences)
    {
        this.influencers = influences;
    }

    public List<BucketInfluencer> getBucketInfluencers()
    {
        return bucketInfluencers;
    }

    public void setBucketInfluencers(List<BucketInfluencer> bucketInfluencers)
    {
        this.bucketInfluencers = bucketInfluencers;
    }

    public void addBucketInfluencer(BucketInfluencer bucketInfluencer)
    {
        if (bucketInfluencers == null)
        {
            bucketInfluencers = new ArrayList<>();
        }
        bucketInfluencers.add(bucketInfluencer);
    }


    public List<PartitionScore> getPartitionScores()
    {
        return partitionScores;
    }

    public void setPartitionScores(List<PartitionScore> scores)
    {
        partitionScores = scores;
    }

    /**
     * Box class for the stream collector function below
     */
    private final class DoubleMaxBox
    {
        private double value = 0.0;

        public DoubleMaxBox()
        {
        }

        public void accept(double d)
        {
            if (d > value)
            {
                value = d;
            }
        }

        public DoubleMaxBox combine(DoubleMaxBox other)
        {
            return (this.value > other.value) ? this : other;
        }

        public Double value()
        {
            return this.value;
        }
    }

    public Map<String, Double> calcMaxNormalizedProbabilityPerPartition()
    {
        perPartitionMaxProbability = records.stream().collect(
                Collectors.groupingBy(AnomalyRecord::getPartitionFieldValue,
                        Collector.of(DoubleMaxBox::new,
                                (m, ar) -> m.accept(ar.getNormalizedProbability()),
                                DoubleMaxBox::combine, DoubleMaxBox::value)));

        return perPartitionMaxProbability;
    }

    public Map<String, Double> getPerPartitionMaxProbability() {
        return perPartitionMaxProbability;
    }

    public void setPerPartitionMaxProbability(
            Map<String, Double> perPartitionMaxProbability) {
        this.perPartitionMaxProbability = perPartitionMaxProbability;
    }

    public double partitionAnomalyScore(String partitionValue)
    {
        Optional<PartitionScore> first = partitionScores.stream()
                .filter(s -> partitionValue.equals(s.getPartitionFieldValue()))
                .findFirst();

        return first.isPresent() ? first.get().getAnomalyScore() : 0.0;
    }

    @Override
    public int hashCode()
    {
        // hadBigNormalisedUpdate is deliberately excluded from the hash
        // as is id, which is generated by the datastore
        return Objects.hash(timestamp, eventCount, initialAnomalyScore, anomalyScore,
                maxNormalizedProbability, recordCount, records, isInterim, bucketSpan,
                bucketInfluencers, influencers);
    }

    /**
     * Compare all the fields and embedded anomaly records (if any)
     */
    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (other instanceof Bucket == false)
        {
            return false;
        }

        Bucket that = (Bucket)other;

        // hadBigNormalisedUpdate is deliberately excluded from the test
        // as is id, which is generated by the datastore
        return Objects.equals(this.timestamp, that.timestamp)
                && (this.eventCount == that.eventCount)
                && (this.bucketSpan == that.bucketSpan)
                && (this.anomalyScore == that.anomalyScore)
                && (this.initialAnomalyScore == that.initialAnomalyScore)
                && (this.maxNormalizedProbability == that.maxNormalizedProbability)
                && (this.recordCount == that.recordCount)
                && Objects.equals(this.records, that.records)
                && Objects.equals(this.isInterim, that.isInterim)
                && Objects.equals(this.bucketInfluencers, that.bucketInfluencers)
                && Objects.equals(this.influencers, that.influencers);
    }

    public boolean hadBigNormalisedUpdate()
    {
        return hadBigNormalisedUpdate;
    }

    public void resetBigNormalisedUpdateFlag()
    {
        hadBigNormalisedUpdate = false;
    }

    public void raiseBigNormalisedUpdateFlag()
    {
        hadBigNormalisedUpdate = true;
    }

    /**
     * This method encapsulated the logic for whether a bucket should
     * be normalised. The decision depends on two factors.
     *
     * The first is whether the bucket has bucket influencers.
     * Since bucket influencers were introduced, every bucket must have
     * at least one bucket influencer. If it does not, it means it is
     * a bucket persisted with an older version and should not be
     * normalised.
     *
     * The second factor has to do with minimising the number of buckets
     * that are sent for normalisation. Buckets that have no records
     * and a score of zero should not be normalised as their score
     * will not change and they will just add overhead.
     *
     * @return true if the bucket should be normalised or false otherwise
     */
    public boolean isNormalisable()
    {
        if (bucketInfluencers == null || bucketInfluencers.isEmpty())
        {
            return false;
        }
        return anomalyScore > 0.0 || recordCount > 0;
    }

    @Override
    public void serialise(StorageSerialiser serialiser) throws IOException
    {
        serialiser.addTimestamp(timestamp)
                  .add(ANOMALY_SCORE, anomalyScore)
                  .add(INITIAL_ANOMALY_SCORE, initialAnomalyScore)
                  .add(MAX_NORMALIZED_PROBABILITY, maxNormalizedProbability)
                  .add(RECORD_COUNT, recordCount)
                  .add(EVENT_COUNT, eventCount)
                  .add(BUCKET_SPAN, bucketSpan)
                  .add(PROCESSING_TIME_MS, processingTimeMs);

        if (isInterim)
        {
            serialiser.add(IS_INTERIM, isInterim);
        }

        if (bucketInfluencers != null)
        {
            serialiser.add(BUCKET_INFLUENCERS, bucketInfluencers);
        }

        if (partitionScores != null && partitionScores.isEmpty() == false)
        {
            serialiser.add(PARTITION_SCORES, partitionScores);
        }
    }
}
