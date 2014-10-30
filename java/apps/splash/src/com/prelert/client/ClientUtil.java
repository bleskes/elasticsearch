/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2010     *
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

package com.prelert.client;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.NumberFormat;

import static com.prelert.data.DateTimeFormatPatterns.*;
import com.prelert.client.ClientMessages;
import com.prelert.client.image.ClientImages;
import com.prelert.client.list.EvidenceAttributesDialog;
import com.prelert.data.TimeFrame;


/**
 * Class contains a number of utility functions for use in client-side code.
 * @author Pete Harverson
 */
public class ClientUtil
{
	/**
	 * Date/time formatting object for weekly views.
	 */
	public static final DateTimeFormat WEEK_TIME_FORMAT = 
		DateTimeFormat.getFormat(WEEK_PATTERN);
	
	/**
	 * Date/time formatting object for daily views.
	 */
	public static final DateTimeFormat DAY_TIME_FORMAT = 
		DateTimeFormat.getFormat(DAY_PATTERN);
	
	/**
	 * Date/time formatting object for hourly views.
	 */
	public static final DateTimeFormat HOUR_TIME_FORMAT = 
		DateTimeFormat.getFormat(HOUR_PATTERN);
	
	/**
	 * Date/time formatting object for 'by minute' views.
	 */
	public static final DateTimeFormat MINUTE_TIME_FORMAT = 
		DateTimeFormat.getFormat(MINUTE_PATTERN);
	
	/**
	 * Date/time formatting object for the standard 'by second' views.
	 */
	public static final DateTimeFormat SECOND_TIME_FORMAT = 
		DateTimeFormat.getFormat(SECOND_PATTERN);
	
	
	/**
	 * Locale-sensitive constants used in client applications, such as labels for
	 * UI controls.
	 */
	public static ClientMessages CLIENT_CONSTANTS = GWT.create(ClientMessages.class);
	
	
	/**
	 * Bundle of images and icons for use in the client.
	 */
	public static ClientImages CLIENT_IMAGES = GWT.create(ClientImages.class);
	
	
	/**
	 * Case-insensitive comparator instance.
	 */
	public static final CaseInsensitiveComparator<Object> CASE_INSENSITIVE_COMPARATOR =
		new CaseInsensitiveComparator<Object>();
	
	
	private static NumberFormat s_ShortDecimalFormat;
	
	private static EvidenceAttributesDialog s_EvidenceAttributeDialog;
	
	
	/**
	 * Returns the application-wide instance of the Show Attributes dialog, used for
	 * example the full details of an item of evidence.
	 * @return application-wide instance of the Show Attributes dialog.
	 */
	public static EvidenceAttributesDialog getEvidenceAttributesDialog()
	{
		if (s_EvidenceAttributeDialog == null)
		{
			s_EvidenceAttributeDialog = new EvidenceAttributesDialog();
		}
		
		return s_EvidenceAttributeDialog;
	}
	
	
	/**
	 * Parses the specified String representation of a time which has been
	 * formatted in the context of the specified time frame.
	 * @param timeAsStr formatted String representation of time to parse.
	 * @param timeFrame	time frame context in which the specified String is formatted
	 * 			 e.g. WEEK, DAY, HOUR, MINUTE or SECOND.
	 * @return parsed Date/Time value of the supplied String.
	 */
	public static Date parseTimeField(String timeAsStr, TimeFrame timeFrame)
	{
		Date date = null;
		
		// Need to use GWT DateTimeFormat class as java.text.SimpleDateFormat
		// not supported. Note that GWT DateTimeFormat cannot be used on the
		// server-side too due to its call to GWT.create().
		DateTimeFormat dateFormatter = getDateTimeFormat(timeFrame);
		if (dateFormatter != null && timeAsStr != null)
		{
			date = dateFormatter.parse(timeAsStr);
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
	public static DateTimeFormat getDateTimeFormat(TimeFrame timeFrame)
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
	
	
	/**
	 * Provides the shortened decimal format for the default locale (commonly 
	 *         one decimal place instead of the standard three) 
	 * @return a <code>NumberFormat</code> capable of producing and consuming
	 *         a shortened version decimal format.
	 */
	public static NumberFormat getShortDecimalFormat()
	{
		if (s_ShortDecimalFormat == null)
		{
			s_ShortDecimalFormat = NumberFormat.getFormat(
					CLIENT_CONSTANTS.shortDecimalFormat());
		}
		
		return s_ShortDecimalFormat;
	}
	
}
