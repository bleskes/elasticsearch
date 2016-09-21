
package org.elasticsearch.xpack.prelert.job.results;

import org.elasticsearch.xpack.prelert.job.persistence.serialisation.StorageSerialisable;
import org.elasticsearch.xpack.prelert.job.persistence.serialisation.StorageSerialiser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Influence field name and list of influence field values/score pairs
 */
public class Influence implements StorageSerialisable
{
    /**
     * Note all publicly exposed field names are "influencer" not "influence"
     */
    public static final String INFLUENCER_FIELD_NAME = "influencerFieldName";
    public static final String INFLUENCER_FIELD_VALUES = "influencerFieldValues";


    private String field;
    private List<String> fieldValues;

    public Influence()
    {
        fieldValues = new ArrayList<String>();
    }

    public Influence(String field)
    {
        this();
        this.field = field;
    }

    public String getInfluencerFieldName()
    {
        return field;
    }

    public void setInfluencerFieldName(String field)
    {
        this.field = field;
    }

    public List<String> getInfluencerFieldValues()
    {
        return fieldValues;
    }

    public void setInfluencerFieldValues(List<String> values)
    {
        this.fieldValues = values;
    }

    public void addInfluenceFieldValue(String value)
    {
        fieldValues.add(value);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(field, fieldValues);
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

        Influence other = (Influence) obj;

        return Objects.equals(field, other.field) &&
                Objects.equals(fieldValues, other.fieldValues);
    }

    @Override
    public void serialise(StorageSerialiser serialiser) throws IOException
    {
        serialiser.add(INFLUENCER_FIELD_NAME, field).add(INFLUENCER_FIELD_VALUES, fieldValues);
    }
}
