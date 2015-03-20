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

package com.prelert.transforms.date;

import java.util.List;

import org.apache.log4j.Logger;

import com.prelert.transforms.TransformException;

/**
 * A transformer that attempts to parse a String timestamp
 * as a double and convert that to a long that represents
 * an epoch time in seconds.
 * If m_IsMillisecond is true, it assumes the number represents
 * time in milli-seconds and will convert to seconds
 */
public class DoubleDateTransform extends DateTransform
{
    private final boolean m_IsMillisecond;


    public DoubleDateTransform(boolean isMillisecond, List<TransformIndex> readIndicies,
            List<TransformIndex> writeIndicies, Logger logger)
    {
        super(readIndicies, writeIndicies, logger);
        m_IsMillisecond = isMillisecond;
    }

    @Override
    public boolean transform(String[][] readWriteArea)
    throws TransformException
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

        try
        {
            // parse as a double and throw away the fractional
            // component
            long longValue = Double.valueOf(field).longValue();
            m_Epoch = m_IsMillisecond ? longValue / 1000 : longValue;

            i = m_WriteIndicies.get(0);
            readWriteArea[i.array][i.index] = Long.toString(m_Epoch);
            return true;
        }
        catch (NumberFormatException e)
        {
            String message = String.format(
                    "Cannot parse timestamp '%s' as epoch value", field);
            throw new ParseTimestampException(message);
        }
    }
}

