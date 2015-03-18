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

package com.prelert.job;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.prelert.rs.data.ErrorCode;
import com.prelert.transforms.DependencySorter;

/**
 * Utility class for methods involving arrays of transforms
 */
public class TransformConfigs
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

    public boolean verify()
    throws TransformConfigurationException
    {
        String duplicatedName = outputNamesAreUnique();
        if (duplicatedName != null)
        {
            String msg = String.format("Transform output name %s is used more than once", duplicatedName);
             throw new TransformConfigurationException(msg, ErrorCode.DUPLICATED_TRANSFORM_OUTPUT_NAME);
        }

        // TODO check inputs/outputs are used


        // Check for circular dependencies
        int index = DependencySorter.checkForCircularDependencies(m_Transforms);
        if (index >= 0)
        {
            TransformConfig tc = m_Transforms.get(index);
            String msg = String.format("Transform type %s with inputs %s has a circular dependency",
                                        tc.type(), tc.getInputs());
            throw new TransformConfigurationException(msg, ErrorCode.TRANSFORM_HAS_CIRCULAR_DEPENDENCY);
        }


        for (TransformConfig tr : m_Transforms)
        {
            tr.verify();
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


}
