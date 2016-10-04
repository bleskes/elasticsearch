
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
    private List<TransformConfig> tansforms;

    public TransformConfigs(List<TransformConfig> transforms)
    {
        tansforms = transforms;
        if (tansforms == null)
        {
            tansforms = Collections.emptyList();
        }
    }


    public List<TransformConfig> getTransforms()
    {
        return tansforms;
    }


    /**
     * Set of all the field names that are required as inputs to
     * transforms
     * @return
     */
    public Set<String> inputFieldNames()
    {
        Set<String> fields = new HashSet<>();
        for (TransformConfig t : tansforms)
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
        for (TransformConfig t : tansforms) {
            fields.addAll(t.getOutputs());
        }

        return fields;
    }
}
