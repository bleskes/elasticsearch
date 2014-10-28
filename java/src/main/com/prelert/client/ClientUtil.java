/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2012     *
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

import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.MessageBox.MessageBoxType;
import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.NumberFormat;

import static com.prelert.data.DateTimeFormatPatterns.*;
import com.prelert.client.ClientMessages;
import com.prelert.client.image.ClientImages;
import com.prelert.data.TimeFrame;
import com.prelert.data.gxt.UserModel;


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
	
	
	/**
	 * Time span, in seconds, used for calculating causality data metrics 
	 * e.g. peak value of time series in window of interest.
	 */
	public static final int CAUSALITY_METRICS_TIME_SPAN = 900;
	
	
	/** 
	 * Maximum number of causality items to display in a widget on initial load.
	 */
	public static final int CAUSALITY_MAX_DISPLAY_ITEMS = 10;
	
	
	private static UserModel	s_LoggedInUser;
	private static MessageBox	s_ErrorDialog;
	
	private static NumberFormat s_ShortDecimalFormat;
	
	
	/**
	 * Sets the details of the user currently logged into the client.
	 * @param user the currently logged in user.
	 */
	public static void setLoggedInUser(UserModel user)
	{
		s_LoggedInUser = user;
	}
	
	
	/**
	 * Returns the details of the user currently logged into the client.
	 * @return the currently logged in user.
	 */
	public static UserModel getLoggedInUser()
	{
		return s_LoggedInUser;
	}
	
	
	/**
	 * Returns the username of the user currently logged into the client.
	 * @return the username of the logged in user. Returns <code>null</code> if
	 * 	access to the client does not require authentication.
	 */
	public static String getLoggedInUsername()
	{
		String username = null;
		if (s_LoggedInUser != null)
		{
			username = s_LoggedInUser.getUsername();
		}
		return username;
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
	
	
	/**
	 * Returns the number of hours to display for the specified duration.
	 * @param durationMs length of time, in milliseconds.
	 * @return whole number of hours. Remainder minutes &gt;= 59.5 are rounded
	 * 	up to the next number of hours.
	 */
	public static int getNumberOfHours(long durationMs)
	{
		long hours = durationMs/3600000l;
		double mins = Math.round(((double)durationMs % 3600000d)/60000d);
		
		if (mins == 60d)
		{
			hours++;
		}
		
		return (int)hours;
	}
	
	
	/**
	 * Returns the minutes modulus, in respect to the number of hours corresponding 
	 * to the supplied duration. For example if called with a value of 
	 * 4080000 ms (= 4080 seconds = 68 minutes = 1 hour and 8 minutes), a value of 8
	 * will be returned.
	 * @param durationMs length of time, in milliseconds.
	 * @return number minutes modulus. Values of less than 1, are rounded up to 1.
	 */
	public static int getModMinutes(long durationMs)
	{
		double mins = Math.round(((double)durationMs % 3600000d)/60000d);
		if (durationMs < 60000l)
		{
			mins = 1d;	// Round up to 1 minute.
		}
		else
		{
			if (mins == 60d)
			{
				mins = 0d;
			}
		}
		
		return (int)mins;
	}
	
	
	/**
	 * Displays an application-wide instance of a GXT Warning MessageBox 
	 * with the specified text.
	 * @param errorMessage error message to display in the MessageBox.
	 */
	public static void showErrorMessage(String errorMessage)
	{
		if (s_ErrorDialog == null)
		{
			s_ErrorDialog = new MessageBox();
			s_ErrorDialog.setTitle(ClientUtil.CLIENT_CONSTANTS.errorTitle());
			s_ErrorDialog.setButtons(MessageBox.OK);
			s_ErrorDialog.setIcon(MessageBox.WARNING);
			s_ErrorDialog.setType(MessageBoxType.ALERT);
			s_ErrorDialog.setMessage(errorMessage);
			s_ErrorDialog.getDialog().setWidth(560);
		}
		else
		{
			s_ErrorDialog.updateText(errorMessage);
		}
		
		
		s_ErrorDialog.show();
	}
	
}
