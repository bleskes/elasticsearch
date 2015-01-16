/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2014     *
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

package com.prelert.job.process.dateparsing;

/**
 * A transformer that attempts to parse a String timestamp
 * as a double and convert that to a long that represents
 * an epoch. If m_IsMillisecond is true, it will convert to seconds.
 */
public class DoubleDateTransformer implements DateTransformer {

    private final boolean m_IsMillisecond;

    public DoubleDateTransformer(boolean isMillisecond)
    {
        m_IsMillisecond = isMillisecond;
    }

    @Override
    public long transform(String timestamp) throws CannotParseTimestampException
    {
        try
        {
            // parse as a double and throw away the fractional
            // component
            long longValue = Double.valueOf(timestamp).longValue();
            return m_IsMillisecond ? longValue / 1000 : longValue;
        }
        catch (NumberFormatException e)
        {
            String message = String.format(
                    "Cannot parse timestamp '%s' as epoch value", timestamp);
            throw new CannotParseTimestampException(message, e);
        }
    }
}