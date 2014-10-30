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

package com.prelert.server;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;

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
	 * This delimiter is used for procedures that interact with the
	 * time_series_attribute_list_lookup table, when none of the attribute names
	 * or values contain a semi-colon.  If any attribute name or value contains
	 * a semi-colon, the more complex delimiter below is used.  (Obviously if we
	 * were starting out now it wouldn't be done like this, but we need to
	 * retain backwards compatibility.)
	 */
	public static final String SIMPLE_DELIMITER = ";";
	
	
	/**
	 * This delimiter does not contain characters with a special meaning
	 * in regular expressions so it safe to use in Java string functions 
	 * that take regular expressions. 
	 * Use in cases where the SIMPLE_DELIMITER may occur in the data naturally.
	 */
	public static final String REGEX_SAFE_DELIMITER = "=%=";


	/**
	 * This delimiter is used when the database procedures need to be passed
	 * multiple strings in one field.  It's chosen to minimise the risk that it
	 * will occur in any data imported into Prelert.  This constant may only
	 * be changed if the database procedures are changed at the same time.  It
	 * must also be consistent with <code>DELIMITER_REGEX</code> below.
	 */
	public static final String DELIMITER = "^`!";


	/**
	 * This regex is used to parse items where a database procedure has returned
	 * multiple strings in one field.  It's chosen to minimise the risk that it
	 * will occur in any data imported into Prelert.  This constant may only
	 * be changed if the database procedures are changed at the same time.  It
	 * must also be consistent with <code>DELIMITER</code> above.
	 */
	public static final String DELIMITER_REGEX = "\\^`!";
	
	
	/**
	 * The delimiter used to separate elements in the metric path
	 * but does not have to be used to delimit the source or metric
	 * parts of the path. 
	 * <br/> 
	 * If the default vales are used to build the metric path 
	 * {@linkplain #METRIC_PATH_DELIMITER}, {@linkplain #METRIC_PATH_METRIC_PREFIX},
	 * {@linkplain #METRIC_PATH_SOURCE_PREFIX} then the formatted metric path will 
	 * be of the form:<br/>
	 * source//attribute1|attribute2|...|attributeN:metric
	 */
	public static final String METRIC_PATH_DELIMITER = "|";

	
	/**
	 * The delimiter used to separate the metric from the rest of the
	 * metric path.
	 * <br/> 
	 * If the default vales are used to build the metric path 
	 * {@linkplain #METRIC_PATH_DELIMITER}, {@linkplain #METRIC_PATH_METRIC_PREFIX},
	 * {@linkplain #METRIC_PATH_SOURCE_PREFIX} then the formatted metric path will 
	 * be of the form:<br/>
	 * source//attribute1|attribute2|...|attributeN:metric
	 */
	public static final String METRIC_PATH_METRIC_PREFIX = ":";

	
	/**
	 * The delimiter used to separate the source from the rest of the
	 * metric path.
	 * <br/> 
	 * If the default vales are used to build the metric path 
	 * {@linkplain #METRIC_PATH_DELIMITER}, {@linkplain #METRIC_PATH_METRIC_PREFIX},
	 * {@linkplain #METRIC_PATH_SOURCE_PREFIX} then the formatted metric path will 
	 * be of the form:<br/>
	 * source//attribute1|attribute2|...|attributeN:metric
	 */
	public static final String METRIC_PATH_SOURCE_PREFIX = "//";

	
	/**
	 * Returns the pathname of the <code>$PRELERT_HOME</code> directory, as defined
	 * by the <code>prelert.home</code> System property.
	 * @return the pathname of the <code>$PRELERT_HOME</code> directory, or
	 * 	<code>null</code> if <code>prelert.home</code> has not been defined as a
	 * 	System property.
	 */
	public static String getPrelertHome()
	{
		return System.getProperty("prelert.home");
	}
	
	
	/**
	 * Returns the pathname of the <code>config</code> directory under
	 * <code>$PRELERT_HOME</code> (as defined
	 * by the <code>prelert.home</code> System property).
	 * @return the pathname of the Prelert config directory, or <code>null</code> 
	 * if <code>prelert.home</code> has not been defined as a
	 * 	System property.
	 */
	public static String getPrelertConfigHome()
	{
		String prelertConfigHome = null;
		String prelertHome = getPrelertHome();
		if (prelertHome != null)
		{
			prelertConfigHome = new String(prelertHome) + File.separatorChar + "config";
		}
		
		return prelertConfigHome;
	}
	

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
	 * @return delimited list of filter attribute names or values, or
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
				argBuilder.append(value).append(DELIMITER);
			}

			argument = argBuilder.substring(0, argBuilder.length() - DELIMITER.length());
		}

		return argument;
	}


	/**
	 * Prepares the attribute names argument to be passed to procedures or functions
	 * from a list of Attribute objects.
	 * @param attributes list of attributes.
	 * @return delimited list of attribute names, or
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
	 * @return delimited list of attribute values, or
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


	/**
	 * Prepares a delimited list of alternating attributes names and values to
	 * be passed to procedures or functions from a list of Attribute objects.
	 * The resulting string has the attributes sorted alphabetically by name.
	 * @param delim The delimiter to use.
	 * @param attributes List of attributes.
	 * @return A delimited list of alternating attribute names and values, or
	 * 		   <code>null</code> if the supplied list is <code>null</code> or
	 *         empty.
	 */
	public static String prepareAttributeNameValueArgument(String delim, List<Attribute> attributes)
	{
		String argument = null;

		if (attributes != null && attributes.size() > 0)
		{
			StringBuilder argBuilder = new StringBuilder();

			SortedSet<Attribute> sortedAttributes = new TreeSet<Attribute>();
			sortedAttributes.addAll(attributes);

			String attributeName;
			String attributeValue;
			for (Attribute attribute : sortedAttributes)
			{
				attributeName = attribute.getAttributeName();
				attributeValue = attribute.getAttributeValue();

				if (attributeName != null && attributeValue != null)
				{
					argBuilder.append(attributeName).append(delim).append(attributeValue).append(delim);
				}
			}

			argument = argBuilder.substring(0, argBuilder.length() - delim.length());
		}

		return argument;
	}

	
	/**
	 * Create a metric path from the provided attribute names.
	 * Calls {@linkplain prepareMetricPath(List<Attribute> attributes, 
	 * String sourceDelimiter, String metricPathDelimiter, String metricDelimiter, 
	 * String delimiter)} with the default delimiters {@linkplain #METRIC_PATH_DELIMITER}, 
	 * {@linkplain #METRIC_PATH_METRIC_PREFIX} and 
	 * {@linkplain #METRIC_PATH_SOURCE_PREFIX}.
	 * 
	 * @param delimiter Delimits the metric path values. Should
	 * 			be {@link SIMPLE_DELIMITER} if a {@link SIMPLE_DELIMITER} does not 
	 * 			occur in any of the attribute name or values; 
	 * 			otherwise it should be {@link DELIMITER}. 
	 * 		    Use the result of {@link #delimiterForAttributes} for this argument.
	 * @param attributes List of attributes the order of which determines the
	 * 		  order of elements in the resulting metric path.
	 * @return The metric path
	 */
	static public String prepareMetricPath(String delimiter, List<Attribute> attributes)
	{
		return prepareMetricPath(delimiter, attributes, METRIC_PATH_SOURCE_PREFIX, 0,
								METRIC_PATH_DELIMITER, METRIC_PATH_METRIC_PREFIX);
	}

	
	/**
	 * Create a metric path from the attribute names using the provided delimiters.
	 * The resulting path starts with 'source', ends with 'metric' with all the 
	 * attribute names in the middle.  
	 * 
	 * @param attributes List of attributes the order of which determines the
	 * 		  order of elements in the resulting metric path.
	 * @param sourcePrefix Delimits the source from the rest of the path
	 * @param sourcePosition Where the source should come one the path. 
	 * 			0 is the first element on the path. If < 0 then 0 is used.
	 * @param sourcePosition - The position of the source in the metric path. 
	 * 							If 0 then source is the first element in the path before
	 * 							all attributes, else attribute 0 is the first element
	 * 							and source is inserted between the attributes. 
	 * 							If >= attributes.size() the source is inserted
	 * 							after the attributes and before the metric.
	 * @param metricPathDelimiter Delimits individual elemements of the path
	 * 			(excluding source and metric)
	 * @param metricPrefix Delimits the metric from the rest of the path
	 * @param delimiter Delimits the metric path values. Should
	 * 			be {@link SIMPLE_DELIMITER} if a {@link SIMPLE_DELIMITER} does not 
	 * 			occur in any of the attribute name or values; 
	 * 			otherwise it should be {@link DELIMITER}. 
	 * 		    Use the result of {@link #delimiterForAttributes} for this argument.
	 * @return The metric path
	 */
	static public String prepareMetricPath(String delimiter, List<Attribute> attributes, 
									String sourcePrefix,
									int sourcePosition,
									String metricPathDelimiter,
									String metricPrefix)
	{
		StringBuilder metricPath = new StringBuilder();
		
		if (sourcePosition < 0 )
		{
			sourcePosition = 0;
		}
		
		metricPath.append(sourcePrefix);  // start of path prefix
		metricPath.append(delimiter);
		
		
		int pos = 0;
		if (sourcePosition == 0)
		{
			metricPath.append("source");
			metricPath.append(delimiter);
			
			pos = 1;
		}
			
		for (Attribute attr : attributes)
		{
			if (pos == sourcePosition)
			{
				// source cannot be at position 0 if here.
				metricPath.append(metricPathDelimiter); 
				metricPath.append(delimiter);
				metricPath.append("source");
				metricPath.append(delimiter);
			}
			
			if (pos > 0) // don't add this delimter right after the sourcePrefix.
			{
				metricPath.append(metricPathDelimiter); 
				metricPath.append(delimiter);
			}
			metricPath.append(attr.getAttributeName());
			metricPath.append(delimiter);
			
			pos++;
		}
		
		
		if (sourcePosition >= attributes.size())
		{
			// source cannot be at position 0 if here.
			metricPath.append(metricPathDelimiter); 
			metricPath.append(delimiter);
			metricPath.append("source");
			metricPath.append(delimiter);
		}
		
		metricPath.append(metricPrefix); // metric prefix
		metricPath.append(delimiter);
		metricPath.append("metric");
		
		return metricPath.toString();
	}

	
	/**
	 * Get the most required delimiter to be used when calling database
	 * procedures that interact with the time_series_attribute_list_lookup
	 * table.
	 * @param attributes List of attributes to check.  May be null or empty.
	 * @return The required delimiter.  This will likely be passed back to the
	 *         <code>prepareAttributeNameValueArgument</code> method, as well
	 *         as being passed as an argument in its own right to a database
	 *         procedure.
	 */
	public static String delimiterForAttributes(List<Attribute> attributes)
	{
		// If any attribute name or value contains a semi-colon, the delimiter
		// will be a complex 3 character string.  Otherwise it will be a
		// semi-colon.
		if (attributes != null && attributes.size() > 0)
		{
			String attributeName;
			String attributeValue;
			for (Attribute attribute : attributes)
			{
				attributeName = attribute.getAttributeName();
				if (attributeName != null && attributeName.contains(SIMPLE_DELIMITER))
				{
					return DELIMITER;
				}

				attributeValue = attribute.getAttributeValue();
				if (attributeValue != null && attributeValue.contains(SIMPLE_DELIMITER))
				{
					return DELIMITER;
				}
			}
		}

		return SIMPLE_DELIMITER;
	}


	/**
	 * Helper routine extracts the Attribute names from a list of
	 * <code>Attributes</code> and returns the names as a list.
	 * @param attributes to extract names from.
	 * @return List of Attribute names
	 */
	public static List<String> attributeNames(List<Attribute> attributes)
	{
		List<String> attrNames = new ArrayList<String>();

		for (Attribute attribute : attributes)
		{
			attrNames.add(attribute.getAttributeName());			
		}
		
		return attrNames;
	}
	
	
	/**
	 * Parses the specified delimited String in the format
	 * 'string1^`!string2^`!string3^`!...' to a list of Strings.
	 * @param delimitedStrings delimited String e.g. string1^`!string2^`!string3^`!
	 * @return the list of Strings.
	 */
	public static List<String> parseStrings(String delimitedStrings)
	{
		String[] tokens = delimitedStrings.split(DELIMITER_REGEX, -1);
		
		// Return a resizable ArrayList of Strings.
		ArrayList<String> strings = new ArrayList<String>();
		for (String token : tokens)
		{
			strings.add(token);
		}
		
		return strings;
	}


	/**
	* Parses the time series attributes in the specified delimited String
	* in the format 'name1^`!value1^`!name2^`!value2^`!...'
	* to a list of Attribute objects.
	* @param attributes delimiter separated list e.g. appId^`!PRISM_Sink_Dist_App^`!username^`!257_PRISM_VOBR_0
	* @return the list of Attributes.
	* @throws NoSuchElementException if the String does not contain an even number of tokens.
	*/
	public static List<Attribute> parseAttributes(String attributes) throws NoSuchElementException
	{
		// Cannot use a StringTokenizer in case any non-null zero length
		// attribute values are returned.
		ArrayList<Attribute> attributeList = new ArrayList<Attribute>();
		String[] tokens = attributes.split(DELIMITER_REGEX, -1);
		
		String attributeName;
		String attributeValue;
		
		List<String> tokenList = Arrays.asList(tokens);
		Iterator<String> tokenIterator = tokenList.iterator();
		while (tokenIterator.hasNext())
		{
			attributeName = tokenIterator.next();
			attributeValue = tokenIterator.next();

			attributeList.add(new Attribute(attributeName, attributeValue));
		}

		return attributeList;
	}

}
