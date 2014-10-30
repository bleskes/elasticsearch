/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2009     *
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
 ***********************************************************/

package com.prelert.server;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.prelert.data.EventRecord;
import com.prelert.data.TimeFrame;


/**
 * Class contains a number of utility functions for use in server-side code.
 * @author Pete Harverson
 */
public class ServerUtil
{
	/**
	 * Retrieves the value of the time field as a Date object for the 
	 * specified EventRecord in the context of the specified time frame.
	 * @param record EventRecord for which to obtain the value of the time field.
	 * @param timeFrame	time frame context of record e.g. WEEK, DAY, 
	 * 			HOUR, MINUTE or SECOND.
	 * @return recorded time of event record.
	 * @throws ParseException if the value of the time field in the supplied
	 *  record cannot be parsed to a Date object.
	 * 
	 */
	public static Date parseTimeField(EventRecord record, TimeFrame timeFrame) 
		throws ParseException
	{
		Date date = null;
		
		String timeVal = record.getTime(timeFrame);
		
		// Need to use GWT DateTimeFormat class as java.text.SimpleDateFormat
		// not supported. Note that GWT DateTimeFormat cannot be used on the
		// server-side too due to its call to GWT.create().
		SimpleDateFormat dateFormatter = null;
		switch (timeFrame)
		{
			case WEEK:
				dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				break;
			case DAY:
				dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
				break;
			case HOUR:
				dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:00-59");
				break;
			case MINUTE:
				dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
				break;
			case SECOND:
				dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				break;
		}
		
		if (dateFormatter != null && timeVal != null)
		{
			date = dateFormatter.parse(timeVal);
		}
		
		return date;
	}
}
