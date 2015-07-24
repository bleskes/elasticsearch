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

import java.util.List;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;


public class RegexSplit extends Transform
{
    private final Pattern m_Pattern;

    public RegexSplit(String regex, List<TransformIndex> readIndicies,
            List<TransformIndex> writeIndicies, Logger logger)
    {
        super(readIndicies, writeIndicies, logger);

        m_Pattern = Pattern.compile(regex);
    }

    @Override
    public TransformResult transform(String[][] readWriteArea)
    throws TransformException
    {
        TransformIndex readIndex = m_ReadIndicies.get(0);
        String field = readWriteArea[readIndex.array][readIndex.index];

        String [] split = m_Pattern.split(field);

        warnIfOutputCountIsNotMatched(split.length, field);

        int count = Math.min(split.length, m_WriteIndicies.size());
        for (int i=0; i<count; i++)
        {
            TransformIndex index = m_WriteIndicies.get(i);
            readWriteArea[index.array][index.index] = split[i];
        }

        return TransformResult.OK;
    }

    private void warnIfOutputCountIsNotMatched(int splitCount, String field)
    {
        if (splitCount != m_WriteIndicies.size())
        {
            String warning = String.format(
                    "Transform 'split' has %d output(s) but splitting value '%s' resulted to %d part(s)",
                    m_WriteIndicies.size(), field, splitCount);
            m_Logger.warn(warning);
        }
    }
}

