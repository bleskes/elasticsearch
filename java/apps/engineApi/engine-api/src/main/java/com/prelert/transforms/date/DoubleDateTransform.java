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
    protected long toEpochMs(String field) throws TransformException
    {
        try
        {
            long longValue = Double.valueOf(field).longValue();
            return m_IsMillisecond ? longValue : longValue * SECONDS_TO_MS;
        }
        catch (NumberFormatException e)
        {
            String message = String.format("Cannot parse timestamp '%s' as epoch value", field);
            throw new ParseTimestampException(message);
        }
    }
}

