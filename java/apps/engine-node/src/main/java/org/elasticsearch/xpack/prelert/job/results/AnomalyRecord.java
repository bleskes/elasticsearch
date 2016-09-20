
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

    private String m_Id;
    private int m_DetectorIndex;
    private double m_Probability;
    private String m_ByFieldName;
    private String m_ByFieldValue;
    private String m_CorrelatedByFieldValue;
    private String m_PartitionFieldName;
    private String m_PartitionFieldValue;
    private String m_Function;
    private String m_FunctionDescription;
    private double[] m_Typical;
    private double[] m_Actual;
    private boolean m_IsInterim;

    private String m_FieldName;

    private String m_OverFieldName;
    private String m_OverFieldValue;
    private List<AnomalyCause> m_Causes;

    private double m_AnomalyScore;
    private double m_NormalizedProbability;

    private double m_InitialNormalizedProbability;

    private Date m_Timestamp;
    private long m_BucketSpan;

    private List<Influence> m_Influencers;

    private boolean m_HadBigNormalisedUpdate;

    private String m_Parent;

    /**
     * Data store ID of this record.  May be null for records that have not been
     * read from the data store.
     */
    public String getId()
    {
        return m_Id;
    }

    public void setId(String id)
    {
        m_Id = id;
    }

    public int getDetectorIndex()
    {
        return m_DetectorIndex;
    }

    public void setDetectorIndex(int detectorIndex)
    {
        m_DetectorIndex = detectorIndex;
    }

    public double getAnomalyScore()
    {
        return m_AnomalyScore;
    }

    public void setAnomalyScore(double anomalyScore)
    {
        m_AnomalyScore = anomalyScore;
    }

    public double getNormalizedProbability()
    {
        return m_NormalizedProbability;
    }

    public void setNormalizedProbability(double normalizedProbability)
    {
        m_NormalizedProbability = normalizedProbability;
    }

    public double getInitialNormalizedProbability()
    {
        return m_InitialNormalizedProbability;
    }

    public void setInitialNormalizedProbability(double initialNormalizedProbability)
    {
        m_InitialNormalizedProbability = initialNormalizedProbability;
    }

    public Date getTimestamp()
    {
        return m_Timestamp;
    }

    public void setTimestamp(Date timestamp)
    {
        m_Timestamp = timestamp;
    }

    /**
     * Bucketspan expressed in seconds
     */
    public long getBucketSpan()
    {
        return m_BucketSpan;
    }

    /**
     * Bucketspan expressed in seconds
     */
    public void setBucketSpan(long bucketSpan)
    {
        m_BucketSpan = bucketSpan;
    }

    public double getProbability()
    {
        return m_Probability;
    }

    public void setProbability(double value)
    {
        m_Probability = value;
    }


    public String getByFieldName()
    {
        return m_ByFieldName;
    }

    public void setByFieldName(String value)
    {
        m_ByFieldName = value.intern();
    }

    public String getByFieldValue()
    {
        return m_ByFieldValue;
    }

    public void setByFieldValue(String value)
    {
        m_ByFieldValue = value.intern();
    }

    public String getCorrelatedByFieldValue()
    {
        return m_CorrelatedByFieldValue;
    }

    public void setCorrelatedByFieldValue(String value)
    {
        m_CorrelatedByFieldValue = value.intern();
    }

    public String getPartitionFieldName()
    {
        return m_PartitionFieldName;
    }

    public void setPartitionFieldName(String field)
    {
        m_PartitionFieldName = field.intern();
    }

    public String getPartitionFieldValue()
    {
        return m_PartitionFieldValue;
    }

    public void setPartitionFieldValue(String value)
    {
        m_PartitionFieldValue = value.intern();
    }

    public String getFunction()
    {
        return m_Function;
    }

    public void setFunction(String name)
    {
        m_Function = name.intern();
    }

    public String getFunctionDescription()
    {
        return m_FunctionDescription;
    }

    public void setFunctionDescription(String functionDescription)
    {
        m_FunctionDescription = functionDescription.intern();
    }

    @JsonFormat(with = Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
    public double[] getTypical()
    {
        return m_Typical;
    }

    public void setTypical(double[] typical)
    {
        m_Typical = typical;
    }

    @JsonFormat(with = Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
    public double[] getActual()
    {
        return m_Actual;
    }

    public void setActual(double[] actual)
    {
        m_Actual = actual;
    }

    @JsonProperty("isInterim")
    public boolean isInterim()
    {
        return m_IsInterim;
    }

    @JsonProperty("isInterim")
    public void setInterim(boolean isInterim)
    {
        m_IsInterim = isInterim;
    }

    public String getFieldName()
    {
        return m_FieldName;
    }

    public void setFieldName(String field)
    {
        m_FieldName = field.intern();
    }

    public String getOverFieldName()
    {
        return m_OverFieldName;
    }

    public void setOverFieldName(String name)
    {
        m_OverFieldName = name.intern();
    }

    public String getOverFieldValue()
    {
        return m_OverFieldValue;
    }

    public void setOverFieldValue(String value)
    {
        m_OverFieldValue = value.intern();
    }

    public List<AnomalyCause> getCauses()
    {
        return m_Causes;
    }

    public void setCauses(List<AnomalyCause> causes)
    {
        m_Causes = causes;
    }

    public void addCause(AnomalyCause cause)
    {
        if (m_Causes == null)
        {
            m_Causes = new ArrayList<>();
        }
        m_Causes.add(cause);
    }

    public String getParent()
    {
        return m_Parent;
    }

    public void setParent(String parent)
    {
        m_Parent = parent.intern();
    }

    public List<Influence> getInfluencers()
    {
        return m_Influencers;
    }

    public void setInfluencers(List<Influence> influencers)
    {
        m_Influencers = influencers;
    }


    @Override
    public int hashCode()
    {
        // ID is NOT included in the hash, so that a record from the data store
        // will hash the same as a record representing the same anomaly that did
        // not come from the data store

        // m_HadBigNormalisedUpdate is also deliberately excluded from the hash

        return Objects.hash(m_DetectorIndex, m_Probability, m_AnomalyScore, m_InitialNormalizedProbability,
                m_NormalizedProbability, Arrays.hashCode(m_Typical), Arrays.hashCode(m_Actual),
                m_Function, m_FunctionDescription, m_FieldName, m_ByFieldName, m_ByFieldValue, m_CorrelatedByFieldValue,
                m_PartitionFieldName, m_PartitionFieldValue, m_OverFieldName, m_OverFieldValue,
                m_Timestamp, m_Parent, m_IsInterim, m_Causes, m_Influencers);
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
        return this.m_DetectorIndex == that.m_DetectorIndex
                && this.m_Probability == that.m_Probability
                && this.m_AnomalyScore == that.m_AnomalyScore
                && this.m_NormalizedProbability == that.m_NormalizedProbability
                && this.m_InitialNormalizedProbability == that.m_InitialNormalizedProbability
                && Objects.deepEquals(this.m_Typical, that.m_Typical)
                && Objects.deepEquals(this.m_Actual, that.m_Actual)
                && Objects.equals(this.m_Function, that.m_Function)
                && Objects.equals(this.m_FunctionDescription, that.m_FunctionDescription)
                && Objects.equals(this.m_FieldName, that.m_FieldName)
                && Objects.equals(this.m_ByFieldName, that.m_ByFieldName)
                && Objects.equals(this.m_ByFieldValue, that.m_ByFieldValue)
                && Objects.equals(this.m_CorrelatedByFieldValue, that.m_CorrelatedByFieldValue)
                && Objects.equals(this.m_PartitionFieldName, that.m_PartitionFieldName)
                && Objects.equals(this.m_PartitionFieldValue, that.m_PartitionFieldValue)
                && Objects.equals(this.m_OverFieldName, that.m_OverFieldName)
                && Objects.equals(this.m_OverFieldValue, that.m_OverFieldValue)
                && Objects.equals(this.m_Timestamp, that.m_Timestamp)
                && Objects.equals(this.m_Parent, that.m_Parent)
                && Objects.equals(this.m_IsInterim, that.m_IsInterim)
                && Objects.equals(this.m_Causes, that.m_Causes)
                && Objects.equals(this.m_Influencers, that.m_Influencers);
    }

    public boolean hadBigNormalisedUpdate()
    {
        return m_HadBigNormalisedUpdate;
    }

    public void resetBigNormalisedUpdateFlag()
    {
        m_HadBigNormalisedUpdate = false;
    }

    public void raiseBigNormalisedUpdateFlag()
    {
        m_HadBigNormalisedUpdate = true;
    }

    @Override
    public void serialise(StorageSerialiser serialiser) throws IOException
    {
        serialiser.addTimestamp(m_Timestamp)
                .add(DETECTOR_INDEX, m_DetectorIndex)
                .add(PROBABILITY, m_Probability)
                .add(ANOMALY_SCORE, m_AnomalyScore)
                .add(NORMALIZED_PROBABILITY, m_NormalizedProbability)
                .add(INITIAL_NORMALIZED_PROBABILITY, m_InitialNormalizedProbability)
                .add(BUCKET_SPAN, m_BucketSpan);

        DotNotationReverser reverser = serialiser.newDotNotationReverser();
        List<String> topLevelExcludes = new ArrayList<>();

        if (m_ByFieldName != null)
        {
            serialiser.add(BY_FIELD_NAME, m_ByFieldName);
            if (m_ByFieldValue != null)
            {
                reverser.add(m_ByFieldName, m_ByFieldValue);
                topLevelExcludes.add(m_ByFieldName);
            }
        }
        if (m_ByFieldValue != null)
        {
            serialiser.add(BY_FIELD_VALUE, m_ByFieldValue);
        }
        if (m_CorrelatedByFieldValue != null)
        {
            serialiser.add(CORRELATED_BY_FIELD_VALUE, m_CorrelatedByFieldValue);
        }
        if (m_Typical != null)
        {
            if (m_Typical.length == 1)
            {
                serialiser.add(TYPICAL, m_Typical[0]);
            }
            else
            {
                serialiser.add(TYPICAL, m_Typical);
            }
        }
        if (m_Actual != null)
        {
            if (m_Actual.length == 1)
            {
                serialiser.add(ACTUAL, m_Actual[0]);
            }
            else
            {
                serialiser.add(ACTUAL, m_Actual);
            }
        }
        if (m_IsInterim)
        {
            serialiser.add(IS_INTERIM, m_IsInterim);
        }
        if (m_FieldName != null)
        {
            serialiser.add(FIELD_NAME, m_FieldName);
        }
        if (m_Function != null)
        {
            serialiser.add(FUNCTION, m_Function);
        }
        if (m_FunctionDescription != null)
        {
            serialiser.add(FUNCTION_DESCRIPTION, m_FunctionDescription);
        }
        if (m_PartitionFieldName != null)
        {
            serialiser.add(PARTITION_FIELD_NAME, m_PartitionFieldName);
            if (m_PartitionFieldValue != null)
            {
                reverser.add(m_PartitionFieldName, m_PartitionFieldValue);
                topLevelExcludes.add(m_PartitionFieldName);
            }
        }
        if (m_PartitionFieldValue != null)
        {
            serialiser.add(PARTITION_FIELD_VALUE, m_PartitionFieldValue);
        }
        if (m_OverFieldName != null)
        {
            serialiser.add(AnomalyRecord.OVER_FIELD_NAME, m_OverFieldName);
            if (m_OverFieldValue != null)
            {
                reverser.add(m_OverFieldName, m_OverFieldValue);
                topLevelExcludes.add(m_OverFieldName);
            }
        }
        if (m_OverFieldValue != null)
        {
            serialiser.add(OVER_FIELD_VALUE, m_OverFieldValue);
        }
        if (m_Causes != null)
        {
            serialiser.add(CAUSES, m_Causes);
        }
        if (m_Influencers != null && m_Influencers.isEmpty() == false)
        {
            // First add the influencers array
            serialiser.add(INFLUENCERS, m_Influencers);

            // Then, where possible without creating duplicates, add top level
            // raw data fields
            for (Influence influence: m_Influencers)
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
