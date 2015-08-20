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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;


public class TransformConfigsTest
{
    @Test
    public void test_Input_Output_FieldNames()
    {
        List<TransformConfig> transforms = new ArrayList<>();
        transforms.add(createConcatTransform(Arrays.asList("a", "b", "c"), Arrays.asList("c1")));
        transforms.add(createConcatTransform(Arrays.asList("d", "e", "c"), Arrays.asList("c2")));
        transforms.add(createConcatTransform(Arrays.asList("f", "a", "c"), Arrays.asList("c3")));

        TransformConfigs tcs = new TransformConfigs(transforms);

        List<String> inputNames = Arrays.asList("a", "b", "c", "d", "e", "f");
        Set<String> inputSet = new HashSet<>(inputNames);
        assertEquals(inputSet, tcs.inputFieldNames());

        List<String> outputNames = Arrays.asList("c1", "c2", "c3");
        Set<String> outputSet = new HashSet<>(outputNames);
        assertEquals(outputSet, tcs.outputFieldNames());
    }

    private TransformConfig createConcatTransform(List<String> inputs, List<String> outputs)
    {
        TransformConfig concat = new TransformConfig();
        concat.setTransform(TransformType.CONCAT.prettyName());
        concat.setInputs(inputs);
        concat.setOutputs(outputs);
        return concat;
    }
}
