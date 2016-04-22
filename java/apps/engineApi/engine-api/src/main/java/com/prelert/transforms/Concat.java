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
import java.util.StringJoiner;

import org.apache.log4j.Logger;


/**
 * Concatenate input fields
 */
public class Concat extends Transform
{
    private static final String EMPTY_STRING = "";

    private final String m_Delimiter;

    public Concat(List<TransformIndex> readIndicies, List<TransformIndex> writeIndicies, Logger logger)
    {
        super(readIndicies, writeIndicies, logger);
        m_Delimiter = EMPTY_STRING;
    }

    public Concat(String join, List<TransformIndex> readIndicies, List<TransformIndex> writeIndicies, Logger logger)
    {
        super(readIndicies, writeIndicies, logger);
        m_Delimiter = join;
    }

    public String getDelimiter()
    {
        return m_Delimiter;
    }

    /**
     * Concat has only 1 output field
     */
    @Override
    public TransformResult transform(String[][] readWriteArea)
    throws TransformException
    {
        if (m_WriteIndicies.isEmpty())
        {
            return TransformResult.FAIL;
        }

        TransformIndex writeIndex = m_WriteIndicies.get(0);

        StringJoiner joiner = new StringJoiner(m_Delimiter);
        for (TransformIndex i : m_ReadIndicies)
        {
            joiner.add(readWriteArea[i.array][i.index]);
        }
        readWriteArea[writeIndex.array][writeIndex.index] = joiner.toString();

        return TransformResult.OK;
    }
}
