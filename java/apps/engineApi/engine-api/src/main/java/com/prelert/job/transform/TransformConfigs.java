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
     * Set of all the field names that are required as inputs to
     * transforms
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

    /**
     * Set of all the field names that are outputted (i.e. created)
     * by transforms
     * @return
     */
    public Set<String> outputFieldNames()
    {
        Set<String> fields = new HashSet<>();
        for (TransformConfig t : m_Transforms)
        {
            fields.addAll(t.getOutputs());
        }

        return fields;
    }



}
