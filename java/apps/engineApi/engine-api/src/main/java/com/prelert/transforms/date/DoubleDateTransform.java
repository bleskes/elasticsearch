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
    private long m_Epoch;

    public DoubleDateTransform(boolean isMillisecond,
            int[] inputIndicies, int[] outputIndicies)
    {
        super(inputIndicies, outputIndicies);
        m_IsMillisecond = isMillisecond;
    }

    @Override
    public long epoch()
    {
        return m_Epoch;
    }

    @Override
    public boolean transform(String[] inputRecord, String[] outputRecord)
    throws TransformException
    {
        String field = inputRecord[m_InputIndicies[0]];
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

            outputRecord[m_OutputIndicies[0]] = Long.toString(m_Epoch);
            return true;
        }
        catch (NumberFormatException e)
        {
            String message = String.format(
                    "Cannot parse timestamp '%s' as epoch value", inputRecord[m_InputIndicies[0]]);
            throw new ParseTimestampException(message);
        }
    }
}

