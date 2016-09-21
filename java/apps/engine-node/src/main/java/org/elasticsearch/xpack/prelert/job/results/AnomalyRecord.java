
package org.elasticsearch.xpack.prelert.job.results;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Feature;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.elasticsearch.xpack.prelert.job.persistence.serialisation.DotNotationReverser;
import org.elasticsearch.xpack.prelert.job.persistence.serialisation.StorageSerialisable;
import org.elasticsearch.xpack.prelert.job.persistence.serialisation.StorageSerialiser;

import java.io.IOException;
import java.util.*;

/**
 * Anomaly Record POJO.
 * Uses the object wrappers Boolean and Double so <code>null</code> values
 * can be returned if the members have not been set.
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties({"id", "parent"})
public class AnomalyRecord implements StorageSerialisable
{
    /**
     * Serialisation fields
     */
    public static final String TYPE = "record";

    /**
     * Result fields (all detector types)
     */
    public static final String DETECTOR_INDEX = "detectorIndex";
    public static final String PROBABILITY = "probability";
    public static final String BY_FIELD_NAME = "byFieldName";
    public static final String BY_FIELD_VALUE = "byFieldValue";
    public static final String CORRELATED_BY_FIELD_VALUE = "correlatedByFieldValue";
    public static final String PARTITION_FIELD_NAME = "partitionFieldName";
    public static final String PARTITION_FIELD_VALUE = "partitionFieldValue";
    public static final String FUNCTION = "function";
    public static final String FUNCTION_DESCRIPTION = "functionDescription";
    public static final String TYPICAL = "typical";
    public static final String ACTUAL = "actual";
    public static final String IS_INTERIM = "isInterim";
    public static final String INFLUENCERS = "influencers";
    public static final String BUCKET_SPAN = "bucketSpan";
    public static final String TIMESTAMP = "timestamp";

    /**
     * Metric Results (including population metrics)
     */
    public static final String FIELD_NAME = "fieldName";

    /**
     * Population results
     */
    public static final String OVER_FIELD_NAME = "overFieldName";
    public static final String OVER_FIELD_VALUE = "overFieldValue";
    public static final String CAUSES = "causes";

    /**
     * Normalisation
     */
    public static final String ANOMALY_SCORE = "anomalyScore";
    public static final String NORMALIZED_PROBABILITY = "normalizedProbability";
    public static final String INITIAL_NORMALIZED_PROBABILITY = "initialNormalizedProbability";

    private String id;
    private int detectorIndex;
    private double probability;
    private String byFieldName;
    private String byFieldValue;
    private String correlatedByFieldValue;
    private String partitionFieldName;
    private String partitionFieldValue;
    private String function;
    private String functionDescription;
    private double[] typical;
    private double[] actual;
    private boolean isInterim;

    private String fieldName;

    private String overFieldName;
    private String overFieldValue;
    private List<AnomalyCause> causes;

    private double anomalyScore;
    private double normalizedProbability;

    private double initialNormalizedProbability;

    private Date timestamp;
    private long bucketSpan;

    private List<Influence> influencers;

    private boolean hadBigNormalisedUpdate;

    private String parent;

    /**
     * Data store ID of this record.  May be null for records that have not been
     * read from the data store.
     */
    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public int getDetectorIndex()
    {
        return detectorIndex;
    }

    public void setDetectorIndex(int detectorIndex)
    {
        this.detectorIndex = detectorIndex;
    }

    public double getAnomalyScore()
    {
        return anomalyScore;
    }

    public void setAnomalyScore(double anomalyScore)
    {
        this.anomalyScore = anomalyScore;
    }

    public double getNormalizedProbability()
    {
        return normalizedProbability;
    }

    public void setNormalizedProbability(double normalizedProbability)
    {
        this.normalizedProbability = normalizedProbability;
    }

    public double getInitialNormalizedProbability()
    {
        return initialNormalizedProbability;
    }

    public void setInitialNormalizedProbability(double initialNormalizedProbability)
    {
        this.initialNormalizedProbability = initialNormalizedProbability;
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

    public double getProbability()
    {
        return probability;
    }

    public void setProbability(double value)
    {
        probability = value;
    }


    public String getByFieldName()
    {
        return byFieldName;
    }

    public void setByFieldName(String value)
    {
        byFieldName = value.intern();
    }

    public String getByFieldValue()
    {
        return byFieldValue;
    }

    public void setByFieldValue(String value)
    {
        byFieldValue = value.intern();
    }

    public String getCorrelatedByFieldValue()
    {
        return correlatedByFieldValue;
    }

    public void setCorrelatedByFieldValue(String value)
    {
        correlatedByFieldValue = value.intern();
    }

    public String getPartitionFieldName()
    {
        return partitionFieldName;
    }

    public void setPartitionFieldName(String field)
    {
        partitionFieldName = field.intern();
    }

    public String getPartitionFieldValue()
    {
        return partitionFieldValue;
    }

    public void setPartitionFieldValue(String value)
    {
        partitionFieldValue = value.intern();
    }

    public String getFunction()
    {
        return function;
    }

    public void setFunction(String name)
    {
        function = name.intern();
    }

    public String getFunctionDescription()
    {
        return functionDescription;
    }

    public void setFunctionDescription(String functionDescription)
    {
        this.functionDescription = functionDescription.intern();
    }

    @JsonFormat(with = Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
    public double[] getTypical()
    {
        return typical;
    }

    public void setTypical(double[] typical)
    {
        this.typical = typical;
    }

    @JsonFormat(with = Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
    public double[] getActual()
    {
        return actual;
    }

    public void setActual(double[] actual)
    {
        this.actual = actual;
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

    public String getFieldName()
    {
        return fieldName;
    }

    public void setFieldName(String field)
    {
        fieldName = field.intern();
    }

    public String getOverFieldName()
    {
        return overFieldName;
    }

    public void setOverFieldName(String name)
    {
        overFieldName = name.intern();
    }

    public String getOverFieldValue()
    {
        return overFieldValue;
    }

    public void setOverFieldValue(String value)
    {
        overFieldValue = value.intern();
    }

    public List<AnomalyCause> getCauses()
    {
        return causes;
    }

    public void setCauses(List<AnomalyCause> causes)
    {
        this.causes = causes;
    }

    public void addCause(AnomalyCause cause)
    {
        if (causes == null)
        {
            causes = new ArrayList<>();
        }
        causes.add(cause);
    }

    public String getParent()
    {
        return parent;
    }

    public void setParent(String parent)
    {
        this.parent = parent.intern();
    }

    public List<Influence> getInfluencers()
    {
        return influencers;
    }

    public void setInfluencers(List<Influence> influencers)
    {
        this.influencers = influencers;
    }


    @Override
    public int hashCode()
    {
        // ID is NOT included in the hash, so that a record from the data store
        // will hash the same as a record representing the same anomaly that did
        // not come from the data store

        // m_HadBigNormalisedUpdate is also deliberately excluded from the hash

        return Objects.hash(detectorIndex, probability, anomalyScore, initialNormalizedProbability,
                normalizedProbability, Arrays.hashCode(typical), Arrays.hashCode(actual),
                function, functionDescription, fieldName, byFieldName, byFieldValue, correlatedByFieldValue,
                partitionFieldName, partitionFieldValue, overFieldName, overFieldValue,
                timestamp, parent, isInterim, causes, influencers);
    }


    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (other instanceof AnomalyRecord == false)
        {
            return false;
        }

        AnomalyRecord that = (AnomalyRecord)other;

        // ID is NOT compared, so that a record from the data store will compare
        // equal to a record representing the same anomaly that did not come
        // from the data store

        // m_HadBigNormalisedUpdate is also deliberately excluded from the test
        return this.detectorIndex == that.detectorIndex
                && this.probability == that.probability
                && this.anomalyScore == that.anomalyScore
                && this.normalizedProbability == that.normalizedProbability
                && this.initialNormalizedProbability == that.initialNormalizedProbability
                && Objects.deepEquals(this.typical, that.typical)
                && Objects.deepEquals(this.actual, that.actual)
                && Objects.equals(this.function, that.function)
                && Objects.equals(this.functionDescription, that.functionDescription)
                && Objects.equals(this.fieldName, that.fieldName)
                && Objects.equals(this.byFieldName, that.byFieldName)
                && Objects.equals(this.byFieldValue, that.byFieldValue)
                && Objects.equals(this.correlatedByFieldValue, that.correlatedByFieldValue)
                && Objects.equals(this.partitionFieldName, that.partitionFieldName)
                && Objects.equals(this.partitionFieldValue, that.partitionFieldValue)
                && Objects.equals(this.overFieldName, that.overFieldName)
                && Objects.equals(this.overFieldValue, that.overFieldValue)
                && Objects.equals(this.timestamp, that.timestamp)
                && Objects.equals(this.parent, that.parent)
                && Objects.equals(this.isInterim, that.isInterim)
                && Objects.equals(this.causes, that.causes)
                && Objects.equals(this.influencers, that.influencers);
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
    public void serialise(StorageSerialiser serialiser) throws IOException
    {
        serialiser.addTimestamp(timestamp)
                .add(DETECTOR_INDEX, detectorIndex)
                .add(PROBABILITY, probability)
                .add(ANOMALY_SCORE, anomalyScore)
                .add(NORMALIZED_PROBABILITY, normalizedProbability)
                .add(INITIAL_NORMALIZED_PROBABILITY, initialNormalizedProbability)
                .add(BUCKET_SPAN, bucketSpan);

        DotNotationReverser reverser = serialiser.newDotNotationReverser();
        List<String> topLevelExcludes = new ArrayList<>();

        if (byFieldName != null)
        {
            serialiser.add(BY_FIELD_NAME, byFieldName);
            if (byFieldValue != null)
            {
                reverser.add(byFieldName, byFieldValue);
                topLevelExcludes.add(byFieldName);
            }
        }
        if (byFieldValue != null)
        {
            serialiser.add(BY_FIELD_VALUE, byFieldValue);
        }
        if (correlatedByFieldValue != null)
        {
            serialiser.add(CORRELATED_BY_FIELD_VALUE, correlatedByFieldValue);
        }
        if (typical != null)
        {
            if (typical.length == 1)
            {
                serialiser.add(TYPICAL, typical[0]);
            }
            else
            {
                serialiser.add(TYPICAL, typical);
            }
        }
        if (actual != null)
        {
            if (actual.length == 1)
            {
                serialiser.add(ACTUAL, actual[0]);
            }
            else
            {
                serialiser.add(ACTUAL, actual);
            }
        }
        if (isInterim)
        {
            serialiser.add(IS_INTERIM, isInterim);
        }
        if (fieldName != null)
        {
            serialiser.add(FIELD_NAME, fieldName);
        }
        if (function != null)
        {
            serialiser.add(FUNCTION, function);
        }
        if (functionDescription != null)
        {
            serialiser.add(FUNCTION_DESCRIPTION, functionDescription);
        }
        if (partitionFieldName != null)
        {
            serialiser.add(PARTITION_FIELD_NAME, partitionFieldName);
            if (partitionFieldValue != null)
            {
                reverser.add(partitionFieldName, partitionFieldValue);
                topLevelExcludes.add(partitionFieldName);
            }
        }
        if (partitionFieldValue != null)
        {
            serialiser.add(PARTITION_FIELD_VALUE, partitionFieldValue);
        }
        if (overFieldName != null)
        {
            serialiser.add(AnomalyRecord.OVER_FIELD_NAME, overFieldName);
            if (overFieldValue != null)
            {
                reverser.add(overFieldName, overFieldValue);
                topLevelExcludes.add(overFieldName);
            }
        }
        if (overFieldValue != null)
        {
            serialiser.add(OVER_FIELD_VALUE, overFieldValue);
        }
        if (causes != null)
        {
            serialiser.add(CAUSES, causes);
        }
        if (influencers != null && influencers.isEmpty() == false)
        {
            // First add the influencers array
            serialiser.add(INFLUENCERS, influencers);

            // Then, where possible without creating duplicates, add top level
            // raw data fields
            for (Influence influence: influencers)
            {
                if (influence.getInfluencerFieldName() != null &&
                    !influence.getInfluencerFieldValues().isEmpty() &&
                    !topLevelExcludes.contains(influence.getInfluencerFieldName()))
                {
                    reverser.add(influence.getInfluencerFieldName(), influence.getInfluencerFieldValues().get(0));
                }
            }
        }

        serialiser.addReverserResults(reverser);
    }
}
