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

package com.prelert.transforms.date;

import java.util.List;

import org.apache.log4j.Logger;

import com.prelert.transforms.Transform;
import com.prelert.transforms.TransformException;

/**
 * Abstract class introduces the {@link #epochMs()} method for
 * date transforms
 */
public abstract class DateTransform extends Transform
{
    protected static final int SECONDS_TO_MS = 1000;

    private long m_EpochMs;

    public DateTransform(List<TransformIndex> readIndicies, List<TransformIndex> writeIndicies, Logger logger)
    {
        super(readIndicies, writeIndicies, logger);
    }

    /**
     * The epoch time from the last transform
     * @return
     */
    public long epochMs()
    {
        return m_EpochMs;
    }

    /**
     * Expects 1 input and 1 output.
     */
    @Override
    public final TransformResult transform(String[][] readWriteArea) throws TransformException
    {
        if (m_ReadIndicies.isEmpty())
        {
            throw new ParseTimestampException("Cannot parse null string");
        }

        if (m_WriteIndicies.isEmpty())
        {
            throw new ParseTimestampException("No write index for the datetime format transform");
        }

        TransformIndex i = m_ReadIndicies.get(0);
        String field = readWriteArea[i.array][i.index];

        if (field == null)
        {
            throw new ParseTimestampException("Cannot parse null string");
        }

        m_EpochMs = toEpochMs(field);
        TransformIndex writeIndex = m_WriteIndicies.get(0);
        readWriteArea[writeIndex.array][writeIndex.index] = Long.toString(m_EpochMs / SECONDS_TO_MS);
        return TransformResult.OK;
    }

    protected abstract long toEpochMs(String field) throws TransformException;
}
