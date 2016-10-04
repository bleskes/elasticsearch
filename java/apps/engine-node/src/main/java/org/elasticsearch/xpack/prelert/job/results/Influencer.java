
package org.elasticsearch.xpack.prelert.job.results;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.elasticsearch.xpack.prelert.job.persistence.serialisation.DotNotationReverser;
import org.elasticsearch.xpack.prelert.job.persistence.serialisation.StorageSerialisable;
import org.elasticsearch.xpack.prelert.job.persistence.serialisation.StorageSerialiser;

import java.io.IOException;
import java.util.Date;
import java.util.Objects;

public class Influencer implements StorageSerialisable
{
    /**
     * Elasticsearch type
     */
    public static final String TYPE = "influencer";

    /*
     * Field names
     */
    public static final String PROBABILITY = "probability";
    public static final String TIMESTAMP = "timestamp";
    public static final String INFLUENCER_FIELD_NAME = "influencerFieldName";
    public static final String INFLUENCER_FIELD_VALUE = "influencerFieldValue";
    public static final String INITIAL_ANOMALY_SCORE = "initialAnomalyScore";
    public static final String ANOMALY_SCORE = "anomalyScore";

    private String id;

    private Date timestamp;
    private String influenceField;
    private String influenceValue;
    private double probability;
    private double initialAnomalyScore;
    private double anomalyScore;
    private boolean hadBigNormalisedUpdate;
    private boolean isInterim;

    public Influencer()
    {
    }

    public Influencer(String fieldName, String fieldValue)
    {
        influenceField = fieldName;
        influenceValue = fieldValue;
    }

    /**
     * Data store ID of this record.  May be null for records that have not been
     * read from the data store.
     */
    @JsonIgnore
    public String getId()
    {
        return id;
    }

    @JsonIgnore
    public void setId(String id)
    {
        this.id = id;
    }

    public double getProbability()
    {
        return probability;
    }

    public void setProbability(double probability)
    {
        this.probability = probability;
    }


    public Date getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp(Date date)
    {
        timestamp = date;
    }


    public String getInfluencerFieldName()
    {
        return influenceField;
    }

    public void setInfluencerFieldName(String fieldName)
    {
        influenceField = fieldName;
    }


    public String getInfluencerFieldValue()
    {
        return influenceValue;
    }

    public void setInfluencerFieldValue(String fieldValue)
    {
        influenceValue = fieldValue;
    }

    public double getInitialAnomalyScore()
    {
        return initialAnomalyScore;
    }

    public void setInitialAnomalyScore(double influenceScore)
    {
        initialAnomalyScore = influenceScore;
    }

    public double getAnomalyScore()
    {
        return anomalyScore;
    }

    public void setAnomalyScore(double score)
    {
        anomalyScore = score;
    }

    public boolean isInterim()
    {
        return isInterim;
    }

    @JsonProperty("isInterim")
    public void setInterim(boolean value)
    {
        isInterim = value;
    }

    public boolean hadBigNormalisedUpdate()
    {
        return this.hadBigNormalisedUpdate;
    }

    public void resetBigNormalisedUpdateFlag()
    {
        hadBigNormalisedUpdate = false;
    }

    public void raiseBigNormalisedUpdateFlag()
    {
        hadBigNormalisedUpdate = true;
    }

    @Override
    public int hashCode()
    {
        // ID is NOT included in the hash, so that a record from the data store
        // will hash the same as a record representing the same anomaly that did
        // not come from the data store

        // hadBigNormalisedUpdate is also deliberately excluded from the hash

        return Objects.hash(timestamp, influenceField, influenceValue, initialAnomalyScore,
                anomalyScore, probability, isInterim);
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

        Influencer other = (Influencer) obj;

        // ID is NOT compared, so that a record from the data store will compare
        // equal to a record representing the same anomaly that did not come
        // from the data store

        // hadBigNormalisedUpdate is also deliberately excluded from the test
        return Objects.equals(timestamp, other.timestamp) &&
                Objects.equals(influenceField, other.influenceField) &&
                Objects.equals(influenceValue, other.influenceValue) &&
                Double.compare(initialAnomalyScore, other.initialAnomalyScore) == 0 &&
                Double.compare(anomalyScore, other.anomalyScore) == 0 &&
                Double.compare(probability, other.probability) == 0 &&
                (isInterim == other.isInterim);
    }

    @Override
    public void serialise(StorageSerialiser serialiser) throws IOException
    {
        serialiser.addTimestamp(timestamp)
                  .add(PROBABILITY, probability)
                  .add(INFLUENCER_FIELD_NAME, influenceField)
                  .add(INFLUENCER_FIELD_VALUE, influenceValue)
                  .add(INITIAL_ANOMALY_SCORE, initialAnomalyScore)
                  .add(ANOMALY_SCORE, anomalyScore);

        if (isInterim)
        {
            serialiser.add(Bucket.IS_INTERIM, true);
        }

        DotNotationReverser reverser = serialiser.newDotNotationReverser();
        reverser.add(influenceField, influenceValue);
        serialiser.addReverserResults(reverser);
    }
}
