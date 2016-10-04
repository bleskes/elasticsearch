
package org.elasticsearch.xpack.prelert.job.results;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.elasticsearch.xpack.prelert.job.persistence.serialisation.StorageSerialisable;
import org.elasticsearch.xpack.prelert.job.persistence.serialisation.StorageSerialiser;

import java.io.IOException;
import java.util.Objects;


@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(value={"rawAnomalyScore","initialAnomalyScore"}, allowSetters=true)
public class BucketInfluencer implements StorageSerialisable
{
    /**
     * Elasticsearch type
     */
    public static final String TYPE = "bucketInfluencer";

    /**
     * This is the field name of the time bucket influencer.
     */
    public static final String BUCKET_TIME = "bucketTime";

    /*
     * Field names
     */
    public static final String INFLUENCER_FIELD_NAME = "influencerFieldName";
    public static final String INITIAL_ANOMALY_SCORE = "initialAnomalyScore";
    public static final String ANOMALY_SCORE = "anomalyScore";
    public static final String RAW_ANOMALY_SCORE = "rawAnomalyScore";
    public static final String PROBABILITY = "probability";

    private String influenceField;
    private double initialAnomalyScore;
    private double anomalyScore;
    private double rawAnomalyScore;
    private double probability;

    public BucketInfluencer()
    {
        // Default constructor
    }

    public double getProbability()
    {
        return probability;
    }

    public void setProbability(double probability)
    {
        this.probability = probability;
    }

    public String getInfluencerFieldName()
    {
        return influenceField;
    }

    public void setInfluencerFieldName(String fieldName)
    {
        this.influenceField = fieldName;
    }

    public double getInitialAnomalyScore()
    {
        return initialAnomalyScore;
    }

    public void setInitialAnomalyScore(double influenceScore)
    {
        this.initialAnomalyScore = influenceScore;
    }

    public double getAnomalyScore()
    {
        return anomalyScore;
    }

    public void setAnomalyScore(double score)
    {
        anomalyScore = score;
    }

    public double getRawAnomalyScore()
    {
        return rawAnomalyScore;
    }

    public void setRawAnomalyScore(double score)
    {
        rawAnomalyScore = score;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(influenceField, initialAnomalyScore, anomalyScore,
                rawAnomalyScore, probability);
    }

    @Override
    public boolean equals(Object obj)
    {
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

        BucketInfluencer other = (BucketInfluencer) obj;

        return  Objects.equals(influenceField, other.influenceField) &&
                Double.compare(initialAnomalyScore, other.initialAnomalyScore) == 0 &&
                Double.compare(anomalyScore, other.anomalyScore) == 0 &&
                Double.compare(rawAnomalyScore, other.rawAnomalyScore) == 0 &&
                Double.compare(probability, other.probability) == 0;
    }

    @Override
    public void serialise(StorageSerialiser serialiser) throws IOException
    {
        serialiser.add(PROBABILITY, probability)
                  .add(INFLUENCER_FIELD_NAME, influenceField)
                  .add(INITIAL_ANOMALY_SCORE, initialAnomalyScore)
                  .add(ANOMALY_SCORE, anomalyScore)
                  .add(RAW_ANOMALY_SCORE, rawAnomalyScore);
    }
}
