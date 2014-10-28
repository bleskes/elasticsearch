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

package com.prelert.server;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.prelert.data.Attribute;
import com.prelert.data.TimeFrame;
import static com.prelert.data.DateTimeFormatPatterns.*;


/**
 * Class contains a number of utility functions for use in server-side code.
 * @author Pete Harverson
 */
public class ServerUtil
{
	
	/**
	 * Parses the specified String representation of a time which has been
	 * formatted in the context of the specified time frame.
	 * @param timeAsStr EventRecord for which to obtain the value of the time field.
	 * @param timeFrame	time frame context in which the specified String is formatted
	 * 			 e.g. WEEK, DAY, HOUR, MINUTE or SECOND.
	 * @return parsed Date/Time value of the supplied String.
	 * @throws ParseException if the value of the time field in the supplied
	 *  record cannot be parsed to a Date object.
	 */
	public static Date parseTimeField(String timeAsStr, TimeFrame timeFrame) 
		throws ParseException
	{
		Date date = null;
		
		SimpleDateFormat dateFormatter = getDateTimeFormat(timeFrame);
		
		if (dateFormatter != null && timeAsStr != null)
		{
			date = dateFormatter.parse(timeAsStr);
		}
		
		return date;
	}
	
	
	/**
	 * Formats a Date object into a String representation in the format for
	 * the specified TimeFrame.
	 * @param dateTime the Date/Time to format.
	 * @param timeFrame	time frame context of display e.g. WEEK, DAY, 
	 * 			HOUR, MINUTE or SECOND.
	 * @return string representation for this date.
	 */
	public static String formatTimeField(Date dateTime, TimeFrame timeFrame)
	{
		String timeStr = "";
		
		SimpleDateFormat dateFormatter = getDateTimeFormat(timeFrame);
		if (dateFormatter != null && dateTime != null)
		{
			timeStr = dateFormatter.format(dateTime);
		}
		
		return timeStr;
	}
	
	
	/**
	 * Returns the DateFormat used to format dates in the context of the specified
	 * time frame.
	 * @param timeFrame time frame context of display e.g. WEEK, DAY, 
	 * 			HOUR, MINUTE or SECOND.
	 * @return the SimpleDateFormat to format dates for the specified time frame.
	 */
	protected static SimpleDateFormat getDateTimeFormat(TimeFrame timeFrame)
	{
		SimpleDateFormat dateFormatter = null;
		switch (timeFrame)
		{
			case WEEK:
				dateFormatter = new SimpleDateFormat(WEEK_PATTERN);
				break;
			case DAY:
				dateFormatter = new SimpleDateFormat(DAY_PATTERN);
				break;
			case HOUR:
				dateFormatter = new SimpleDateFormat(HOUR_PATTERN);
				break;
			case MINUTE:
				dateFormatter = new SimpleDateFormat(MINUTE_PATTERN);
				break;
			case SECOND:
				dateFormatter = new SimpleDateFormat(SECOND_PATTERN);
				break;
		}
		
		return dateFormatter;
	}
	
	
	/**
	 * Prepares the argument to be passed to database procedures/functions for 
	 * filter attribute names or values.
	 * @param values list of attribute names or values to use as a filter argument.
	 * @return semi-colon delimited list of filter attribute names or values, or
	 * 		<code>null</code> if the supplied list is <code>null</code> or empty.
	 */
	public static String prepareFilterArgument(List<String> values)
	{
		String argument = null;
		
		if (values != null && values.size() > 0)
		{
			StringBuilder argBuilder = new StringBuilder();
			for (String value : values)
			{
				argBuilder.append(value).append(';');
			}
			
			argument = argBuilder.substring(0, argBuilder.length()-1);
		}
		
		return argument;
	}
	
	
	/**
	 * Prepares the attribute names argument to be passed to procedures or functions
	 * from a list of Attribute objects. 
	 * @param attributes list of attributes.
	 * @return semi-colon delimited list of attribute names, or
	 * 		<code>null</code> if the supplied list is <code>null</code> or empty.
	 */
	public static String prepareAttributeNameArgument(List<Attribute> attributes)
	{
		String argument = null;
		
		if (attributes != null && attributes.size() > 0)
		{
			String attributeName;
			String attributeValue;
			List<String> attrNames = new ArrayList<String>();
			for (Attribute attribute : attributes)
			{
				attributeName = attribute.getAttributeName();
				attributeValue = attribute.getAttributeValue();
				
				if (attributeName != null && attributeValue != null)
				{
					attrNames.add(attributeName);
				}
			}
			
			argument = prepareFilterArgument(attrNames);
		}
		
		return argument;
	}
	
	
	/**
	 * Prepares the attribute values argument to be passed to procedures or functions
	 * from a list of Attribute objects. 
	 * @param attributes list of attributes.
	 * @return semi-colon delimited list of attribute values, or
	 * 		<code>null</code> if the supplied list is <code>null</code> or empty.
	 */
	public static String prepareAttributeValueArgument(List<Attribute> attributes)
	{
		String argument = null;
		
		if (attributes != null && attributes.size() > 0)
		{
			String attributeName;
			String attributeValue;
			List<String> attrVals = new ArrayList<String>();
			for (Attribute attribute : attributes)
			{
				attributeName = attribute.getAttributeName();
				attributeValue = attribute.getAttributeValue();
				
				if (attributeName != null && attributeValue != null)
				{
					attrVals.add(attributeValue);
				}
			}
			
			argument = prepareFilterArgument(attrVals);
		}
		
		return argument;
	}
}
