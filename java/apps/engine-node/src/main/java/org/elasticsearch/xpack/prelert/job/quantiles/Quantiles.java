
package org.elasticsearch.xpack.prelert.job.quantiles;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.elasticsearch.xpack.prelert.job.persistence.serialisation.StorageSerialisable;
import org.elasticsearch.xpack.prelert.job.persistence.serialisation.StorageSerialiser;
import org.elasticsearch.xpack.prelert.utils.Strings;

import java.io.IOException;
import java.util.Date;
import java.util.Objects;

/**
 * Quantiles Result POJO
 */
@JsonInclude(Include.NON_NULL)
public class Quantiles implements StorageSerialisable
{
    public static final String QUANTILES_ID = "hierarchical";

    /**
     * Field Names
     */
    public static final String TIMESTAMP = "timestamp";
    public static final String QUANTILE_STATE = "quantileState";

    /**
     * Elasticsearch type
     */
    public static final String TYPE = "quantiles";

    private Date timestamp;
    private String quantileState;

    public Date getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp(Date timestamp)
    {
        this.timestamp = timestamp;
    }

    public String getQuantileState()
    {
        return Strings.nullToEmpty(quantileState);
    }

    public void setQuantileState(String quantileState)
    {
        this.quantileState = quantileState;
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(quantileState);
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

        if (other instanceof Quantiles == false)
        {
            return false;
        }

        Quantiles that = (Quantiles) other;

        return Objects.equals(this.quantileState, that.quantileState);
    }

    @Override
    public void serialise(StorageSerialiser serialiser) throws IOException
    {
        serialiser.addTimestamp(timestamp).add(QUANTILE_STATE, quantileState);
    }
}

