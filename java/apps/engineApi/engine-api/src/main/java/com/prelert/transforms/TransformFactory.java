/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2016     *
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
import java.util.Map;

import org.apache.log4j.Logger;

import com.prelert.job.transform.TransformConfig;
import com.prelert.job.transform.TransformType;
import com.prelert.transforms.Transform.TransformIndex;

/**
 * Create transforms from the configuration object.
 * Transforms need to know where to read strings from and where
 * write the output to hence input and output maps required by the
 * create method.
 */
public class TransformFactory
{
    public static final int INPUT_ARRAY_INDEX = 0;
    public static final int SCRATCH_ARRAY_INDEX = 1;
    public static final int OUTPUT_ARRAY_INDEX = 2;

    /**
     *
     * @param transformConfig
     * @param inputIndiciesMap
     * @param scratchAreaIndiciesMap
     * @param outputIndiciesMap
     * @param logger
     * @return
     */
    public Transform create(TransformConfig transformConfig,
            Map<String, Integer> inputIndiciesMap,
            Map<String, Integer> scratchAreaIndiciesMap,
            Map<String, Integer> outputIndiciesMap,
            Logger logger)
    {
        int[] input = new int[transformConfig.getInputs().size()];
        fillIndexArray(transformConfig.getInputs(), inputIndiciesMap, input);

        List<TransformIndex> readIndicies = new ArrayList<>();
        for (String field : transformConfig.getInputs())
        {
            Integer index = inputIndiciesMap.get(field);
            if (index != null)
            {
                readIndicies.add(new TransformIndex(INPUT_ARRAY_INDEX, index));
            }
            else
            {
                index = scratchAreaIndiciesMap.get(field);
                if (index != null)
                {
                    readIndicies.add(new TransformIndex(SCRATCH_ARRAY_INDEX, index));
                }
                else if (outputIndiciesMap.containsKey(field)) // also check the outputs array for this input
                {
                    index = outputIndiciesMap.get(field);
                    readIndicies.add(new TransformIndex(SCRATCH_ARRAY_INDEX, index));
                }
                else
                {
                    throw new IllegalStateException("Transform input '" + field +
                                    "' cannot be found");
                }
            }
        }

        List<TransformIndex> writeIndicies = new ArrayList<>();
        for (String field : transformConfig.getOutputs())
        {
            Integer index = outputIndiciesMap.get(field);
            if (index != null)
            {
                writeIndicies.add(new TransformIndex(OUTPUT_ARRAY_INDEX, index));
            }
            else
            {
                index = scratchAreaIndiciesMap.get(field);
                if (index != null)
                {
                    writeIndicies.add(new TransformIndex(SCRATCH_ARRAY_INDEX, index));
                }
            }
        }

        TransformType type = transformConfig.type();

        switch (type)
        {
            case DOMAIN_SPLIT:
                return new HighestRegisteredDomain(readIndicies, writeIndicies, logger);
            case CONCAT:
                if (transformConfig.getArguments().isEmpty())
                {
                    return new Concat(readIndicies, writeIndicies, logger);
                }
                else
                {
                    return new Concat(transformConfig.getArguments().get(0),
                            readIndicies, writeIndicies, logger);
                }
            case REGEX_EXTRACT:
                return new RegexExtract(transformConfig.getArguments().get(0), readIndicies,
                                            writeIndicies, logger);
            case REGEX_SPLIT:
                return new RegexSplit(transformConfig.getArguments().get(0), readIndicies,
                                            writeIndicies, logger);
            case EXCLUDE:
                if (transformConfig.getCondition().getOperator().expectsANumericArgument())
                {
                    return new ExcludeFilterNumeric(transformConfig.getCondition(),
                            readIndicies, writeIndicies, logger);
                }
                else
                {
                    return new ExcludeFilterRegex(transformConfig.getCondition(), readIndicies,
                                            writeIndicies, logger);
                }
            case LOWERCASE:
                return StringTransform.createLowerCase(readIndicies, writeIndicies, logger);
            case UPPERCASE:
                return StringTransform.createUpperCase(readIndicies, writeIndicies, logger);
            case TRIM:
                return StringTransform.createTrim(readIndicies, writeIndicies, logger);
            case GEO_UNHASH:
                return StringTransform.createGeoUnhash(readIndicies, writeIndicies, logger);
            default:
                // This code will never be hit - it's to
                // keep the compiler happy.
                throw new IllegalArgumentException("Unknown transform type " + type);
        }
    }

    /**
     * For each <code>field</code> fill the <code>indexArray</code>
     * with the index from the <code>indicies</code> map.
     *
     * @param fields
     * @param indicies
     * @param indexArray
     */
    private static void fillIndexArray(List<String> fields, Map<String, Integer> indicies,
                                        int [] indexArray)
    {
        int i = 0;
        for (String field : fields)
        {
            Integer index = indicies.get(field);
            if (index != null)
            {
                indexArray[i++] = index;
            }
//            else
//            {
//                logger.error("Field '" + field + "' not indexed");
//            }
        }
    }
}
