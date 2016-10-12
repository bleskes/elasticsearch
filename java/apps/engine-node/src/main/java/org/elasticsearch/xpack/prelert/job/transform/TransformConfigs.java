
package org.elasticsearch.xpack.prelert.job.transform;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility class for methods involving arrays of transforms
 */
public class TransformConfigs
{
    private List<TransformConfig> transforms;

    public TransformConfigs() {
        this(null);
    }

    public TransformConfigs(List<TransformConfig> transforms)
    {
        this.transforms = transforms;
        if (transforms == null)
        {
            this.transforms = Collections.emptyList();
        }
    }

    public void setTransforms(List<TransformConfig> transforms) {
        this.transforms = transforms;
    }

    public List<TransformConfig> getTransforms()
    {
        return transforms;
    }


    /**
     * Set of all the field names that are required as inputs to
     * transforms
     * @return
     */
    public Set<String> inputFieldNames()
    {
        Set<String> fields = new HashSet<>();
        for (TransformConfig t : transforms)
        {
            fields.addAll(t.getInputs());
        }

        return fields;
    }

    /**
     * Set of all the field names that are outputted (i.e. created)
     * by transforms
     * @return
     */
    public Set<String> outputFieldNames() {
        Set<String> fields = new HashSet<>();
        for (TransformConfig t : transforms) {
            fields.addAll(t.getOutputs());
        }

        return fields;
    }
}
