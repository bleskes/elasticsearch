
package org.elasticsearch.xpack.prelert.job.quantiles;

import org.elasticsearch.action.support.ToXContentToBytes;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.ParseFieldMatcherSupplier;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.ObjectParser;
import org.elasticsearch.common.xcontent.ObjectParser.ValueType;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.xpack.prelert.job.persistence.serialisation.StorageSerialisable;
import org.elasticsearch.xpack.prelert.job.persistence.serialisation.StorageSerialiser;
import org.elasticsearch.xpack.prelert.utils.Strings;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.io.IOException;
import java.util.Date;
import java.util.Objects;

/**
 * Quantiles Result POJO
 */
@JsonInclude(Include.NON_NULL)
public class Quantiles extends ToXContentToBytes implements Writeable, StorageSerialisable
{
    public static final String QUANTILES_ID = "hierarchical";

    /**
     * Field Names
     */
    public static final ParseField TIMESTAMP = new ParseField("timestamp");
    public static final ParseField QUANTILE_STATE = new ParseField("quantileState");

    /**
     * Elasticsearch type
     */
    public static final ParseField TYPE = new ParseField("quantiles");

    public static final ObjectParser<Quantiles, ParseFieldMatcherSupplier> PARSER = new ObjectParser<>(TYPE.getPreferredName(),
            Quantiles::new);

    static {
        PARSER.declareField(Quantiles::setTimestamp, p -> new Date(p.longValue()), TIMESTAMP, ValueType.LONG);
        PARSER.declareString(Quantiles::setQuantileState, QUANTILE_STATE);
    }

    private Date timestamp;
    private String quantileState;

    public Quantiles() {
    }

    public Quantiles(StreamInput in) throws IOException {
        if (in.readBoolean()) {
            timestamp = new Date(in.readLong());
        }
        quantileState = in.readOptionalString();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        boolean hasTimestamp = timestamp != null;
        out.writeBoolean(hasTimestamp);
        if (hasTimestamp) {
            out.writeLong(timestamp.getTime());
        }
        out.writeOptionalString(quantileState);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        if (timestamp != null) {
            builder.field(TIMESTAMP.getPreferredName(), timestamp.getTime());
        }
        if (quantileState != null) {
            builder.field(QUANTILE_STATE.getPreferredName(), quantileState);
        }
        builder.endObject();
        return builder;
    }

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

        return Objects.equals(this.getQuantileState(), that.getQuantileState());
    }

    @Override
    public void serialise(StorageSerialiser serialiser) throws IOException
    {
        serialiser.addTimestamp(timestamp).add(QUANTILE_STATE.getPreferredName(), quantileState);
    }
}

