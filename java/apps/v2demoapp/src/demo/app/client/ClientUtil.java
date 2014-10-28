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

package demo.app.client;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.NumberFormat;

import static demo.app.data.DateTimeFormatPatterns.*;
import demo.app.data.TimeFrame;
import demo.app.data.gxt.EvidenceModel;

/**
 * Class containing a number of utility functions for use in client-side code.
 * @author Pete Harverson
 */
public class ClientUtil
{
	public static final DateTimeFormat WEEK_TIME_FORMAT = 
		DateTimeFormat.getFormat(WEEK_PATTERN);
	
	public static final DateTimeFormat DAY_TIME_FORMAT = 
		DateTimeFormat.getFormat(DAY_PATTERN);
	
	public static final DateTimeFormat HOUR_TIME_FORMAT = 
		DateTimeFormat.getFormat(HOUR_PATTERN);
	
	public static final DateTimeFormat MINUTE_TIME_FORMAT = 
		DateTimeFormat.getFormat(MINUTE_PATTERN);
	
	public static final DateTimeFormat SECOND_TIME_FORMAT = 
		DateTimeFormat.getFormat(SECOND_PATTERN);
	
	
	/**
	 * Locale-sensitive constants used in client applications, such as labels for
	 * UI controls.
	 */
	public static ClientConstants CLIENT_CONSTANTS = GWT.create(ClientConstants.class);
		
	
	/**
	 * Retrieves the value of the time field as a Date object for the 
	 * specified EventRecord in the context of the specified time frame.
	 * @param record EventRecord for which to obtain the value of the time field.
	 * @param timeFrame	time frame context of record e.g. WEEK, DAY, 
	 * 			HOUR, MINUTE or SECOND.
	 * @return recorded time of event record.
	 */
	public static Date parseTimeField(EvidenceModel record, TimeFrame timeFrame)
	{
		Date date = null;
		
		String timeVal = record.getTime(timeFrame);
		
		// Need to use GWT DateTimeFormat class as java.text.SimpleDateFormat
		// not supported. Note that GWT DateTimeFormat cannot be used on the
		// server-side too due to its call to GWT.create().
		DateTimeFormat dateFormatter = getDateTimeFormat(timeFrame);
		if (dateFormatter != null && timeVal != null)
		{
			date = dateFormatter.parse(timeVal);
		}
		
		return date;
	}
	
	
	/**
	 * Formats a Date object into a String representation suitable for displaying
	 * a Date/Time value in a display with the specified TimeFrame.
	 * @param dateTime the Date/Time to format.
	 * @param timeFrame	time frame context of display e.g. WEEK, DAY, 
	 * 			HOUR, MINUTE or SECOND.
	 * @return string representation for this date.
	 */
	public static String formatTimeField(Date dateTime, TimeFrame timeFrame)
	{
		String timeStr = "";
		
		// Need to use GWT DateTimeFormat class as java.text.SimpleDateFormat
		// not supported. Note that GWT DateTimeFormat cannot be used on the
		// server-side too due to its call to GWT.create().
		DateTimeFormat dateFormatter = getDateTimeFormat(timeFrame);
		if (dateFormatter != null && dateTime != null)
		{
			timeStr = dateFormatter.format(dateTime);
		}
		
		return timeStr;
	}
	
	
	/**
	 * Parses text to produce a numeric value. A {@link NumberFormatException} is
	 * thrown if either the text is empty or if the parse does not consume all
	 * characters of the text.
	 * 
	 * @param text the string being parsed
	 * @return a parsed number value
	 * @throws NumberFormatException if the entire text could not be converted
	 *           into a number
	 */
	public static int parseInteger(String text)
	{
		double asDbl = NumberFormat.getDecimalFormat().parse(text);
		
		return (int)asDbl;
	}
	
	
	/**
	 * Returns the DateTimeFormat used to format dates in the client for a 
	 * display of the specified time frame.
	 * @param timeFrame time frame context of display e.g. WEEK, DAY, 
	 * 			HOUR, MINUTE or SECOND.
	 * @return the DateTimeFormat to format dates for the specified time frame.
	 */
	protected static DateTimeFormat getDateTimeFormat(TimeFrame timeFrame)
	{
		DateTimeFormat dateFormatter = null;
		switch (timeFrame)
		{
			case ALL:
			case WEEK:
				dateFormatter = WEEK_TIME_FORMAT;
				break;
			case DAY:
				dateFormatter = DAY_TIME_FORMAT;
				break;
			case HOUR:
				dateFormatter = HOUR_TIME_FORMAT;
				break;
			case MINUTE:
				dateFormatter = MINUTE_TIME_FORMAT;
				break;
			case SECOND:
				dateFormatter = SECOND_TIME_FORMAT;
				break;
		}
		
		return dateFormatter;
	}
	
}
