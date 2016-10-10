
package org.elasticsearch.xpack.prelert.job.transform.verification;


import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.job.transform.TransformConfig;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;

public class TransformConfigsVerifier
{
    private TransformConfigsVerifier()
    {
    }

    /**
     * Checks the transform configurations are valid
     * <ol>
     * <li>Call {@linkplain TransformConfigVerifier#verify(TransformConfig)} ()} on each transform</li>
     * <li>Check all the transform output field names are unique</li>
     * <li>Check there are no circular dependencies in the transforms</li>
     * </ol>
     *
     * @param transforms
     * @return boolean
     */
    public static boolean verify(List<TransformConfig> transforms) throws ElasticsearchParseException
    {
        for (TransformConfig tr : transforms)
        {
            TransformConfigVerifier.verify(tr);
        }

        String duplicatedName = outputNamesAreUnique(transforms);
        if (duplicatedName != null)
        {
            String msg = Messages.getMessage(
                    Messages.JOB_CONFIG_TRANSFORM_OUTPUT_NAME_USED_MORE_THAN_ONCE, duplicatedName);
            throw ExceptionsHelper.parseException(msg, ErrorCodes.DUPLICATED_TRANSFORM_OUTPUT_NAME);
        }

        // Check for circular dependencies
        int index = checkForCircularDependencies(transforms);
        if (index >= 0)
        {
            TransformConfig tc = transforms.get(index);
            String msg = Messages.getMessage(Messages.JOB_CONFIG_TRANSFORM_CIRCULAR_DEPENDENCY,
                    tc.type(), tc.getInputs());
            throw ExceptionsHelper.parseException(msg, ErrorCodes.TRANSFORM_HAS_CIRCULAR_DEPENDENCY);
        }

        return true;
    }


    /**
     * return null if all transform ouput names are
     * unique or the first duplicate name if there are
     * duplications
     *
     * @param transforms
     * @return
     */
    private static  String outputNamesAreUnique(List<TransformConfig> transforms)
    {
        Set<String> fields = new HashSet<>();
        for (TransformConfig t : transforms)
        {
            for (String output : t.getOutputs())
            {
                if (fields.contains(output))
                {
                    return output;
                }
                fields.add(output);
            }
        }

        return null;
    }



    /**
     * Find circular dependencies in the list of transforms.
     * This might be because a transform's input is its output
     * or because of a transitive dependency.
     *
     * If there is a circular dependency the index of the transform
     * in the <code>transforms</code> list at the start of the chain
     * is returned else -1
     *
     * @param transforms
     * @return -1 if no circular dependencies else the index of the
     * transform at the start of the circular chain
     */
    public static int checkForCircularDependencies(List<TransformConfig> transforms)
    {
        for (int i=0; i<transforms.size(); i++)
        {
            Set<Integer> chain = new HashSet<Integer>();
            chain.add(new Integer(i));

            TransformConfig tc = transforms.get(i);
            if (checkCircularDependenciesRecursive(tc, transforms, chain) == false)
            {
                return i;
            }
        }

        return -1;
    }


    private static boolean checkCircularDependenciesRecursive(TransformConfig transform,
            List<TransformConfig> transforms,
            Set<Integer> chain)
    {
        boolean result = true;

        for (int i=0; i<transforms.size(); i++)
        {
            TransformConfig tc = transforms.get(i);

            for (String input : transform.getInputs())
            {
                if (tc.getOutputs().contains(input))
                {
                    Integer index = new Integer(i);
                    if (chain.contains(index))
                    {
                        return false;
                    }

                    chain.add(index);
                    result = result && checkCircularDependenciesRecursive(tc, transforms, chain);
                }
            }
        }

        return result;
    }
}
