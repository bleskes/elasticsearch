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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import org.apache.log4j.Logger;

import com.prelert.transforms.TransformException;

/**
 * A transform that attempts to parse a String timestamp
 * according to a timeFormat. It converts that
 * to a long that represents the equivalent epoch.
 */
public class DateFormatTransform extends DateTransform
{
    private final String m_TimeFormat;
    private DateFormat m_DateFormat;

    public DateFormatTransform(String timeFormat, List<TransformIndex> readIndicies,
            List<TransformIndex> writeIndicies, Logger logger)
    {
        super(readIndicies, writeIndicies, logger);

        m_TimeFormat = timeFormat;
        m_DateFormat = new SimpleDateFormat(m_TimeFormat);
    }

    @Override
    protected boolean parseAndWriteDate(String field, String[][] readWriteArea) throws TransformException
    {
        try
        {
            m_Epoch = m_DateFormat.parse(field).getTime() / 1000;

            TransformIndex writeIndex = m_WriteIndicies.get(0);
            readWriteArea[writeIndex.array][writeIndex.index] = Long.toString(m_Epoch);
            return true;
        }
        catch (ParseException pe)
        {
            String message = String.format("Cannot parse date '%s' with format string '%s'",
                    field, m_TimeFormat);

            throw new ParseTimestampException(message);
        }
    }
}