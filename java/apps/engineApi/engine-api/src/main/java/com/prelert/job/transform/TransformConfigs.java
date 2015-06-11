/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2015     *
 *                                                          *
 *----------------------------------------------------------*
 *----------------------------------------------------------*
 * WARNING:                                                 *
 * THIS FILE CONTAINS UNPUBLISHED PROPRIETARY               *
 * SOURCE CODE WHICH IS THE PROPERTY OF PRELERT LTD AND     *
 * PARENT OR SUBSIDIARY COMPANIES.                          *
 * PLEASE READ THE FOLLOWING AND TAKE CAREFUL NOTE:         *
 *                                                          *
 * This source code is confidential and any person who      *
 * receives a copy of it, or believes that they are viewing *
 * it without permission is asked to notify Prelert Ltd     *
 * on +44 (0)20 3567 1249 or email to legal@prelert.com.    *
 * All intellectual property rights in this source code     *
 * are owned by Prelert Ltd.  No part of this source code   *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ************************************************************/

package com.prelert.job.transform;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.messages.Messages;
import com.prelert.job.transform.exceptions.TransformConfigurationException;
import com.prelert.job.verification.Verifiable;

/**
 * Utility class for methods involving arrays of transforms
 */
public class TransformConfigs implements Verifiable
{
    private List<TransformConfig> m_Transforms;

    public TransformConfigs(List<TransformConfig> transforms)
    {
        m_Transforms = transforms;
        if (m_Transforms == null)
        {
            m_Transforms = Collections.emptyList();
        }
    }


    public List<TransformConfig> getTransforms()
    {
        return m_Transforms;
    }


    /**
     * Set of all the field names configured as inputs to the transforms
     * @return
     */
    public Set<String> inputFieldNames()
    {
        Set<String> fields = new HashSet<>();
        for (TransformConfig t : m_Transforms)
        {
            fields.addAll(t.getInputs());
        }

        return fields;
    }

    public Set<String> outputFieldNames()
    {
        Set<String> fields = new HashSet<>();
        for (TransformConfig t : m_Transforms)
        {
            fields.addAll(t.getOutputs());
        }

        return fields;
    }

    @Override
    public boolean verify() throws TransformConfigurationException
    {
        for (TransformConfig tr : m_Transforms)
        {
            tr.verify();
        }

        String duplicatedName = outputNamesAreUnique();
        if (duplicatedName != null)
        {
            String msg = Messages.getMessage(
                    Messages.JOB_CONFIG_TRANSFORM_OUTPUT_NAME_USED_MORE_THAN_ONCE, duplicatedName);
             throw new TransformConfigurationException(msg, ErrorCodes.DUPLICATED_TRANSFORM_OUTPUT_NAME);
        }

        // Check for circular dependencies
        int index = TransformConfigs.checkForCircularDependencies(m_Transforms);
        if (index >= 0)
        {
            TransformConfig tc = m_Transforms.get(index);
            String msg = Messages.getMessage(Messages.JOB_CONFIG_TRANSFORM_CIRCULAR_DEPENDENCY,
                    tc.type(), tc.getInputs());
            throw new TransformConfigurationException(msg, ErrorCodes.TRANSFORM_HAS_CIRCULAR_DEPENDENCY);
        }

        return true;
    }


    /**
     * return null or the duplicate name
     * @return
     */
    private String outputNamesAreUnique()
    {
        Set<String> fields = new HashSet<>();
        for (TransformConfig t : m_Transforms)
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
