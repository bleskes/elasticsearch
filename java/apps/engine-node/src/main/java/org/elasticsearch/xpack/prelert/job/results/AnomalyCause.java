package org.elasticsearch.xpack.prelert.job.results;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Feature;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.elasticsearch.xpack.prelert.job.persistence.serialisation.DotNotationReverser;
import org.elasticsearch.xpack.prelert.job.persistence.serialisation.StorageSerialisable;
import org.elasticsearch.xpack.prelert.job.persistence.serialisation.StorageSerialiser;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Anomaly Cause POJO.
 * Used as a nested level inside population anomaly records.
 */
@JsonInclude(Include.NON_NULL)
public class AnomalyCause implements StorageSerialisable
{
    /**
     * Result fields
     */
    public static final String PROBABILITY = "probability";
    public static final String OVER_FIELD_NAME = "overFieldName";
    public static final String OVER_FIELD_VALUE = "overFieldValue";
    public static final String BY_FIELD_NAME = "byFieldName";
    public static final String BY_FIELD_VALUE = "byFieldValue";
    public static final String CORRELATED_BY_FIELD_VALUE = "correlatedByFieldValue";
    public static final String PARTITION_FIELD_NAME = "partitionFieldName";
    public static final String PARTITION_FIELD_VALUE = "partitionFieldValue";
    public static final String FUNCTION = "function";
    public static final String FUNCTION_DESCRIPTION = "functionDescription";
    public static final String TYPICAL = "typical";
    public static final String ACTUAL = "actual";
    public static final String INFLUENCERS = "influencers";

    /**
     * Metric Results
     */
    public static final String FIELD_NAME = "fieldName";

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

    private String fieldName;

    private String overFieldName;
    private String overFieldValue;

    private List<Influence> influencers;


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
        return Objects.hash(probability,
                Arrays.hashCode(actual),
                Arrays.hashCode(typical),
                byFieldName,
                byFieldValue,
                correlatedByFieldValue,
                fieldName,
                function,
                functionDescription,
                overFieldName,
                overFieldValue,
                partitionFieldName,
                partitionFieldValue,
                influencers);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (other instanceof AnomalyCause == false)
        {
            return false;
        }

        AnomalyCause that = (AnomalyCause)other;

        return this.probability == that.probability &&
                Objects.deepEquals(this.typical, that.typical) &&
                Objects.deepEquals(this.actual, that.actual) &&
                Objects.equals(this.function, that.function) &&
                Objects.equals(this.functionDescription, that.functionDescription) &&
                Objects.equals(this.fieldName, that.fieldName) &&
                Objects.equals(this.byFieldName, that.byFieldName) &&
                Objects.equals(this.byFieldValue, that.byFieldValue) &&
                Objects.equals(this.correlatedByFieldValue, that.correlatedByFieldValue) &&
                Objects.equals(this.partitionFieldName, that.partitionFieldName) &&
                Objects.equals(this.partitionFieldValue, that.partitionFieldValue) &&
                Objects.equals(this.overFieldName, that.overFieldName) &&
                Objects.equals(this.overFieldValue, that.overFieldValue) &&
                Objects.equals(this.influencers, that.influencers);
    }

    @Override
    public void serialise(StorageSerialiser serialiser) throws IOException
    {
        serialiser.add(PROBABILITY, probability);

        if (typical != null)
        {
            if (typical.length == 1)
            {
                serialiser.add(AnomalyCause.TYPICAL, typical[0]);
            }
            else
            {
                serialiser.add(AnomalyCause.TYPICAL, typical);
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

        DotNotationReverser reverser = serialiser.newDotNotationReverser();

        if (byFieldName != null)
        {
            serialiser.add(AnomalyCause.BY_FIELD_NAME, byFieldName);
            if (byFieldValue != null)
            {
                reverser.add(byFieldName, byFieldValue);
            }
        }
        if (byFieldValue != null)
        {
            serialiser.add(AnomalyCause.BY_FIELD_VALUE, byFieldValue);
        }
        if (correlatedByFieldValue != null)
        {
            serialiser.add(AnomalyCause.CORRELATED_BY_FIELD_VALUE, correlatedByFieldValue);
        }
        if (fieldName != null)
        {
            serialiser.add(AnomalyCause.FIELD_NAME, fieldName);
        }
        if (function != null)
        {
            serialiser.add(AnomalyCause.FUNCTION, function);
        }
        if (functionDescription != null)
        {
            serialiser.add(AnomalyCause.FUNCTION_DESCRIPTION, functionDescription);
        }
        if (partitionFieldName != null)
        {
            serialiser.add(AnomalyCause.PARTITION_FIELD_NAME, partitionFieldName);
            if (partitionFieldValue != null)
            {
                reverser.add(partitionFieldName, partitionFieldValue);
            }
        }
        if (partitionFieldValue != null)
        {
            serialiser.add(AnomalyCause.PARTITION_FIELD_VALUE, partitionFieldValue);
        }
        if (overFieldName != null)
        {
            serialiser.add(AnomalyCause.OVER_FIELD_NAME, overFieldName);
            if (overFieldValue != null)
            {
                reverser.add(overFieldName, overFieldValue);
            }
        }
        if (overFieldValue != null)
        {
            serialiser.add(AnomalyCause.OVER_FIELD_VALUE, overFieldValue);
        }

        serialiser.addReverserResults(reverser);
    }
}
