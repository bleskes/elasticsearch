
package org.elasticsearch.xpack.prelert.job;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Analysis limits for autodetect
 * <p>
 * If an option has not been set it shouldn't be used so the default value is picked up instead.
 */
@JsonInclude(Include.NON_NULL)
public class AnalysisLimits {
    /**
     * Serialisation field names
     */
    public static final String MODEL_MEMORY_LIMIT = "modelMemoryLimit";
    public static final String CATEGORIZATION_EXAMPLES_LIMIT = "categorizationExamplesLimit";

    /**
     * It is initialised to 0.  A value of 0 indicates it was not set, which in
     * turn causes the C++ process to use its own default limit.  A negative
     * value means no limit.  All negative input values are stored as -1.
     */
    private long modelMemoryLimit;

    /**
     * It is initialised to <code>null</code>.
     * A value of <code>null</code> indicates it was not set.
     */
    private Long categorizationExamplesLimit;

    public AnalysisLimits() {
        modelMemoryLimit = 0;
        categorizationExamplesLimit = null;
    }

    public AnalysisLimits(long modelMemoryLimit, Long categorizationExamplesLimit) {
        if (modelMemoryLimit < 0) {
            // All negative numbers mean "no limit"
            this.modelMemoryLimit = -1;
        } else {
            this.modelMemoryLimit = modelMemoryLimit;
        }
        this.categorizationExamplesLimit = categorizationExamplesLimit;
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

    public void setModelMemoryLimit(long value) {
        if (value < 0) {
            // All negative numbers mean "no limit"
            modelMemoryLimit = -1;
        } else {
            modelMemoryLimit = value;
        }
    }

    /**
     * Gets the limit to the number of examples that are stored per category
     *
     * @return the limit or <code>null</code> if not set
     */
    public Long getCategorizationExamplesLimit() {
        return categorizationExamplesLimit;
    }

    public void setCategorizationExamplesLimit(Long value) {
        categorizationExamplesLimit = value;
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
        return this.modelMemoryLimit == that.modelMemoryLimit
                && Objects.equals(this.categorizationExamplesLimit,
                that.categorizationExamplesLimit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modelMemoryLimit, categorizationExamplesLimit);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(MODEL_MEMORY_LIMIT, modelMemoryLimit);
        if (categorizationExamplesLimit != null) {
            map.put(CATEGORIZATION_EXAMPLES_LIMIT, categorizationExamplesLimit);
        }
        return map;
    }
}
