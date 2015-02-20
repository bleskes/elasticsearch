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

import com.prelert.transforms.TransformException;

/**
 * A transform that attempts to parse a String timestamp
 * according to a timeFormat. It converts that
 * to a long that represents the equivalent epoch.
 */
public class DateFormatTransform extends DateTransform
{
    private final String m_TimeFormat;
    private long m_Epoch;
    private DateFormat m_DateFormat;

    public DateFormatTransform(String timeFormat,
    		int[] inputIndicies, int[] outputIndicies)
    {
    	super(inputIndicies, outputIndicies);
        m_TimeFormat = timeFormat;

        m_DateFormat = new SimpleDateFormat(m_TimeFormat);
    }

    @Override
	public long epoch()
    {
    	return m_Epoch;
    }

    /**
     * Expects 1 input and 1 output.
     */
	@Override
	public boolean transform(String[] inputRecord,String[] outputRecord)
	throws TransformException
	{
		String field = inputRecord[m_InputIndicies[0]];
		if (field == null)
		{
			throw new ParseTimestampException("Cannot parse null string");
		}

        try
        {
            m_Epoch = m_DateFormat.parse(field).getTime() / 1000;

            outputRecord[m_OutputIndicies[0]] = Long.toString(m_Epoch);
            return true;
        }
        catch (ParseException pe)
        {
            String message = String.format("Cannot parse date '%s' with format string '%s'",
            		inputRecord[m_InputIndicies[0]], m_TimeFormat);

            throw new ParseTimestampException(message);
        }
	}
}