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

import java.time.format.DateTimeParseException;
import java.util.List;

import org.apache.log4j.Logger;

import com.prelert.transforms.TransformException;
import com.prelert.utils.time.DateTimeFormatterTimestampConverter;
import com.prelert.utils.time.TimestampConverter;

/**
 * A transform that attempts to parse a String timestamp
 * according to a timeFormat. It converts that
 * to a long that represents the equivalent epoch.
 */
public class DateFormatTransform extends DateTransform
{
    private final String m_TimeFormat;
    private final TimestampConverter m_DateToEpochConverter;

    public DateFormatTransform(String timeFormat, List<TransformIndex> readIndicies,
            List<TransformIndex> writeIndicies, Logger logger)
    {
        super(readIndicies, writeIndicies, logger);

        m_TimeFormat = timeFormat;
        m_DateToEpochConverter = DateTimeFormatterTimestampConverter.ofPattern(timeFormat);
    }

    @Override
    protected long toEpochMs(String field) throws TransformException
    {
        try
        {
            return m_DateToEpochConverter.toEpochMillis(field);
        }
        catch (DateTimeParseException pe)
        {
            String message = String.format("Cannot parse date '%s' with format string '%s'",
                    field, m_TimeFormat);

            throw new ParseTimestampException(message);
        }
    }
}