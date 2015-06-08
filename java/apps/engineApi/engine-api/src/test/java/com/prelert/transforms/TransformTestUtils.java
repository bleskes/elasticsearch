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

package com.prelert.transforms;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Range;
import com.prelert.job.transform.TransformConfig;
import com.prelert.job.transform.TransformType;
import com.prelert.job.transform.condition.Condition;
import com.prelert.job.transform.condition.Operator;
import com.prelert.transforms.Transform.TransformIndex;

public final class TransformTestUtils
{
    private TransformTestUtils()
    {
    }

    public static List<TransformIndex> createIndexArray(TransformIndex...indexs)
    {
        List<TransformIndex> result = new ArrayList<Transform.TransformIndex>();
        for (TransformIndex i : indexs)
        {
            result.add(i);
        }

        return result;
    }

    public static TransformConfig createValidTransform(TransformType type)
    {
        List<String> inputs = createValidArgs(type.arityRange());
        List<String> args = createValidArgs(type.argumentsRange());
        List<String> outputs = createValidArgs(type.outputsRange());

        Condition condition = null;
        if (type.hasCondition())
        {
            condition = new Condition(Operator.EQ, "100");
        }

        TransformConfig tr = new TransformConfig();
        tr.setTransform(type.toString());
        tr.setInputs(inputs);
        tr.setArguments(args);
        tr.setOutputs(outputs);
        tr.setCondition(condition);
        return tr;
    }

    private static List<String> createValidArgs(Range<Integer> range)
    {
        List<String> args = new ArrayList<>();
        int validCount = getValidCount(range);
        for (int arg = 0; arg < validCount; ++arg)
        {
            args.add(Integer.toString(arg));
        }
        return args;
    }

    private static int getValidCount(Range<Integer> range)
    {
        return range.hasUpperBound() ? range.upperEndpoint() : range.lowerEndpoint();
    }
}
