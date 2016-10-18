
package org.elasticsearch.xpack.prelert.job;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.elasticsearch.action.support.ToXContentToBytes;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.ParseFieldMatcherSupplier;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.ConstructingObjectParser;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Analysis limits for autodetect
 * <p>
 * If an option has not been set it shouldn't be used so the default value is picked up instead.
 */
@JsonInclude(Include.NON_NULL)
public class AnalysisLimits extends ToXContentToBytes implements Writeable {
    /**
     * Serialisation field names
     */
    public static final ParseField MODEL_MEMORY_LIMIT = new ParseField("modelMemoryLimit");
    public static final ParseField CATEGORIZATION_EXAMPLES_LIMIT = new ParseField("categorizationExamplesLimit");

    public static final ConstructingObjectParser<AnalysisLimits, ParseFieldMatcherSupplier> PARSER = new ConstructingObjectParser<>(
            "analysis_limits", a -> new AnalysisLimits((Long) a[0], (Long) a[1]));

    static {
        PARSER.declareLong(ConstructingObjectParser.constructorArg(), MODEL_MEMORY_LIMIT);
        PARSER.declareLong(ConstructingObjectParser.optionalConstructorArg(), CATEGORIZATION_EXAMPLES_LIMIT);
    }

    /**
     * It is initialised to 0.  A value of 0 indicates it was not set, which in
     * turn causes the C++ process to use its own default limit.  A negative
     * value means no limit.  All negative input values are stored as -1.
     */
    private final long modelMemoryLimit;

    /**
     * It is initialised to <code>null</code>.
     * A value of <code>null</code> indicates it was not set.
     */
    private final Long categorizationExamplesLimit;

    @JsonCreator
    public AnalysisLimits(@JsonProperty("modelMemoryLimit") long modelMemoryLimit,
                          @JsonProperty("categorizationExamplesLimit") Long categorizationExamplesLimit) {
        if (modelMemoryLimit < 0) {
            // All negative numbers mean "no limit"
            this.modelMemoryLimit = -1;
        } else {
            this.modelMemoryLimit = modelMemoryLimit;
        }
        this.categorizationExamplesLimit = categorizationExamplesLimit;
    }

    public AnalysisLimits(StreamInput in) throws IOException {
        this(in.readLong(), in.readOptionalLong());
    }

    /**
     * Maximum size of the model in MB before the anomaly detector
     * will drop new samples to prevent the model using any more
     * memory
     *
     * @return The set memory limit or 0 if not set
     */
    public long getModelMemoryLimit() {
        return modelMemoryLimit;
    }

    /**
     * Gets the limit to the number of examples that are stored per category
     *
     * @return the limit or <code>null</code> if not set
     */
    public Long getCategorizationExamplesLimit() {
        return categorizationExamplesLimit;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeLong(modelMemoryLimit);
        out.writeOptionalLong(categorizationExamplesLimit);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(MODEL_MEMORY_LIMIT.getPreferredName(), modelMemoryLimit);
        if (categorizationExamplesLimit != null) {
            builder.field(CATEGORIZATION_EXAMPLES_LIMIT.getPreferredName(), categorizationExamplesLimit);
        }
        builder.endObject();
        return builder;
    }

    /**
     * Overridden equality test
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other instanceof AnalysisLimits == false) {
            return false;
        }

        AnalysisLimits that = (AnalysisLimits) other;
        return this.modelMemoryLimit == that.modelMemoryLimit &&
                Objects.equals(this.categorizationExamplesLimit, that.categorizationExamplesLimit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modelMemoryLimit, categorizationExamplesLimit);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(MODEL_MEMORY_LIMIT.getPreferredName(), modelMemoryLimit);
        if (categorizationExamplesLimit != null) {
            map.put(CATEGORIZATION_EXAMPLES_LIMIT.getPreferredName(), categorizationExamplesLimit);
        }
        return map;
    }
}
