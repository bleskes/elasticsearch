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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * A transformer that attempts to parse a String timestamp
 * as a data according to a timeFormat. It converts that
 * to a long that represents the equivalent epoch.
 */
public class DateFormatDateTransformer implements DateTransformer {

    private final String m_TimeFormat;

    public DateFormatDateTransformer(String timeFormat)
    {
        m_TimeFormat = timeFormat;
    }

    @Override
    public long transform(String timestamp) throws CannotParseTimestampException
    {
        try
        {
            DateFormat dateFormat = new SimpleDateFormat(m_TimeFormat);
            return dateFormat.parse(timestamp).getTime() / 1000;
        }
        catch (ParseException pe)
        {
            String message = String.format("Cannot parse date '%s' with format string '%s'",
                    timestamp, m_TimeFormat);
            throw new CannotParseTimestampException(message, pe);
        }
    }
}