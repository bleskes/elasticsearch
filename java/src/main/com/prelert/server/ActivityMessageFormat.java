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

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import com.prelert.data.Attribute;
import com.prelert.data.CausalityAggregate;
import com.prelert.data.Incident;


/**
 * <code>ActivityMessageFormat</code> formats data from activities to produce 
 * concatenated messages in a locale-sensitive way for use in UI displays.
 * @author Pete Harverson
 */
public class ActivityMessageFormat
{
	
	/**
	 * Formats the description of an incident for the specified locale. The message returned
	 * will depend on:
	 * <ul>
	 * <li>Number of notifications and/or time series</li> 
	 * <li>Number of shared attributes that are the same for every piece of evidence 
	 * 		within the incident</li>
	 * <li>Number of distinct values of the field, excluding type, that
	 * 		exhibits most commonality among the evidence within the activity, 
	 * 		but is not the same for every piece of evidence within the activity</li>
	 * </ul>
	 * Example return values are:
	 * <ul>
	 * <li><i>5 time series, with the most common field being service PRELERT_FX</i></li>
	 * <li><i>4 time series, all with service PRELERT_FX</i></li>
	 * <li><i>100 time series features, all with type mdhmon, across 2 metric values (cached and active)</i></li>
	 * <li><i>12 notifications, all with service CNX and type p2pslog, with the next most common field being node 1234</i></li>
	 * </ul>
	 * @param incident the incident for which to build the localised description.
	 * @param locale the locale to use for formatting the description.
	 * @return the incident description for the client locale.
	 */
	public static String formatIncidentDescription(Incident incident, Locale locale)
	{	
		ResourceBundle bundle = ResourceBundle.getBundle("prelert_messages", locale);
		
		// Determine the text to use for number of notifications and/or time series.
		//		activity.count.timeseries={0} time series
		//		activity.notification.count={0} notification
		//		activity.notifications.count={0} notifications
		//		activity.both.count={0} notification and {1} time series
		//		activity.bothmultiple.count={0} notifications and {1} time series
		int notificationCount = incident.getNotificationCount();
		int seriesCount = incident.getTimeSeriesCount();
		
		String countsText = "";
		String countsPattern;
		if (notificationCount == 0)
		{
			countsPattern = bundle.getString("activity.count.timeseries");
			countsText = MessageFormat.format(countsPattern, seriesCount);
		}
		else if (notificationCount == 1)
		{
			if (seriesCount > 0)
			{
				countsPattern = bundle.getString("activity.both.count");
				countsText = MessageFormat.format(countsPattern, notificationCount, seriesCount);
			}
			else
			{
				countsPattern = bundle.getString("activity.notification.count");
				countsText = MessageFormat.format(countsPattern, notificationCount);
			}
		}
		else
		{
			if (seriesCount > 0)
			{
				countsPattern = bundle.getString("activity.bothmultiple.count");
				countsText = MessageFormat.format(countsPattern, notificationCount, seriesCount);
			}
			else
			{
				countsPattern = bundle.getString("activity.notifications.count");
				countsText = MessageFormat.format(countsPattern, notificationCount);
			}
		}

		List<Attribute> sharedFields = incident.getSharedAttributes();
		String commonFieldName = incident.getCommonAttributeName();
		int commonFieldValueCount = incident.getCommonAttributeValueCount();
		List<String> commonFieldValues = incident.getCommonFieldTopValues();
		int numSharedFields = 0;
		if (sharedFields != null && sharedFields.size() > 0)
		{
			numSharedFields = sharedFields.size();
		}
		
		
		// Determine the pattern to use, depending on the number of shared fields
		// and the number of distinct values for the common field.	
		//
		// Should always match a pattern, but if not just return the text showing
		// the number of notifications and time series.
		String localizedDesc = countsText;
		String pattern;
		String commonValuesText;
		if (numSharedFields == 0)
		{
			if (commonFieldValueCount == 1)
			{
				// activity.description.pattern1={0}, with the most common field being {1} {2}
				// e.g. 5 time series, with the most common field being service PRELERT_FX
				pattern = bundle.getString("activity.description.pattern1");
				localizedDesc = MessageFormat.format(pattern, countsText, commonFieldName, commonFieldValues.get(0));
			}
			else if (commonFieldValueCount > 1)
			{
				// activity.description.pattern2={0}, across {1} {2} values {3}
				// e.g. 23 notifications and 9 time series, across 9 source values (most common lon-01, lon-03, lon-02)
				pattern = bundle.getString("activity.description.pattern2");
				commonValuesText = formatCommonValues(commonFieldValues, commonFieldValueCount, locale);	
				localizedDesc = MessageFormat.format(pattern, countsText, 
						commonFieldValueCount, commonFieldName, commonValuesText);
			}
			else
			{
				localizedDesc = countsText;
			}
		}
		else
		{
			StringBuilder sharedFieldsText = new StringBuilder();
			Attribute sharedField;
			
			if (numSharedFields == 1)
			{
				sharedField = sharedFields.get(0);
				sharedFieldsText.append(sharedField.getAttributeName()).
					append(' ').append(sharedField.getAttributeValue());
				
				if (commonFieldValueCount == 0)
				{
					// activity.description.pattern3={0}, all with {1}
					// e.g. 4 time series, all with service PRELERT_FX
					pattern = bundle.getString("activity.description.pattern3");
					localizedDesc = MessageFormat.format(pattern, countsText, sharedFieldsText);
				}
				else if (commonFieldValueCount == 1)
				{
					// activity.description.pattern4={0}, all with {1}, with the next most common field being {2} {3}
					// e.g. 12 notifications, all with service CNX, with the next most common field being node 1234
					pattern = bundle.getString("activity.description.pattern4");
					localizedDesc = MessageFormat.format(pattern, countsText, sharedFieldsText,
							commonFieldName, commonFieldValues.get(0));
				}
				else
				{
					// activity.description.pattern5={0}, all with {1}, across {2} {3} values {4}
					// e.g. 100 time series features, all with type mdhmon, across 2 metric values (cached and active)
					pattern = bundle.getString("activity.description.pattern5");
					commonValuesText = formatCommonValues(commonFieldValues, commonFieldValueCount, locale);
					localizedDesc = MessageFormat.format(pattern, countsText, sharedFieldsText,
							commonFieldValueCount, commonFieldName, commonValuesText);
				}
				
			}
			else
			{
				// number of shared fields > 1.
				for (int i = 0; i < (numSharedFields-1); i++)
				{
					sharedField = sharedFields.get(i);
					if (i > 0)
					{
						sharedFieldsText.append(", ");
					}
					
					sharedFieldsText.append(sharedField.getAttributeName()).
						append(' ').append(sharedField.getAttributeValue());
				}
				
				StringBuilder andSharedFieldsText = new StringBuilder();
				sharedField = sharedFields.get(numSharedFields-1);
				andSharedFieldsText.append(sharedField.getAttributeName()).
					append(' ').append(sharedField.getAttributeValue());
				
				
				if (commonFieldValueCount == 0)
				{
					// activity.description.pattern6={0}, all with {1} and {2}
					// e.g. 101 time series features, all with service PRELERT_DX and type mdhmon
					pattern = bundle.getString("activity.description.pattern6");
					localizedDesc = MessageFormat.format(pattern, countsText, 
							sharedFieldsText, andSharedFieldsText);
				}
				else if (commonFieldValueCount == 1)
				{
					// activity.description.pattern7={0}, all with {1} and {2}, with the next most common field being {3} {4}
					// e.g. 12 notifications, all with service CNX and type p2pslog, with the next most common field being node 1234
					pattern = bundle.getString("activity.description.pattern7");
					localizedDesc = MessageFormat.format(pattern, countsText, 
							sharedFieldsText, andSharedFieldsText, 
							commonFieldName, commonFieldValues.get(0));
				}
				else
				{
					// activity.description.pattern8={0}, all with {1} and {2}, across {3} {4} values {5}
					// e.g. 14 time series, all with service CNX and type mdhmon, across 9 source values (most common lon-01, lon-04, lon-05)
					pattern = bundle.getString("activity.description.pattern8");
					commonValuesText = formatCommonValues(commonFieldValues, commonFieldValueCount, locale);
					localizedDesc = MessageFormat.format(pattern, countsText, 
							sharedFieldsText, andSharedFieldsText,
							commonFieldValueCount, commonFieldName, commonValuesText);
				}
			}
		}
		
		return localizedDesc;

	}
	
	
	/**
	 * Formats a list of common attribute values, such as used in an activity description.
	 * @param values list of values.
	 * @param totalNumberValues total number of common attribute values, which may be
	 * 	greater than the size of the list of values passed to the method.
	 * @param locale the locale to use for formatting the values.
	 * @return formatted list of common attribute values.
	 */
	protected static String formatCommonValues(List<String> values, int totalNumberValues,
			Locale locale)
	{
		ResourceBundle bundle = ResourceBundle.getBundle("prelert_messages", locale);
		String pattern;
		
		// TODO - for time series where common attribute name is description can lead
		// to long return values. Limit to two values? Or would be shortened if back-end
		// procs used metric in place of description.
		if (values.size() < totalNumberValues)
		{
			// activity.commonValues.pattern2=(most common {0} and {1})
			pattern = bundle.getString("activity.commonValues.pattern2");
		}
		else
		{
			// activity.commonValues.pattern1=({0} and {1})
			pattern = bundle.getString("activity.commonValues.pattern1");
		}
		
		StringBuilder commonValuesBuilder = new StringBuilder();
		for (int i = 0; i < (values.size()-1); i++)
		{
			if (i > 0)
			{
				commonValuesBuilder.append(", ");
			}
			
			commonValuesBuilder.append(values.get(i));
		}
		
		return MessageFormat.format(pattern, commonValuesBuilder.toString(), 
				values.get(values.size()-1));
	}
	
	
	/**
	 * Formats a summary of aggregated causality data for the specified locale. 
	 * locale of the client, based on the Accept-Language header of the current Http request. 
	 * If no Accept-Language header is supplied, the default locale of the server will be used.
	 * @param aggregate aggregated causality data.
	 * @param aggregateBy name of the attribute by which the causality data has 
	 * 	been aggregated.
	 * @return summary of the aggregated causality data for the client locale.
	 */
	public static String formatCausalitySummary(
			CausalityAggregate aggregate, String aggregateBy, Locale locale)
	{
		ResourceBundle bundle = ResourceBundle.getBundle("prelert_messages", locale);
		String localizedDesc = new String();
		
		String aggregateValue = aggregate.getAggregateValue();
		int notificationCount = aggregate.getNotificationCount();
		int featureCount = aggregate.getFeatureCount();
		List<String> sources = aggregate.getSourceNames();
		int sourceCount = aggregate.getSourceCount();
		
		
		// Build the String to indicate the sources the causality data occurs on.
		String sourcesText;
		if (sourceCount <= 3)
		{
			StringBuilder sourcesBuilder = new StringBuilder();
			for (int i = 0; i < sourceCount; i++)
			{
				if (i > 0)
				{
					sourcesBuilder.append(',');
				}
				sourcesBuilder.append(sources.get(i));
			}
			sourcesText = sourcesBuilder.toString();
		}
		else
		{
			String manySourcesPattern = bundle.getString("incident.summary.manySources");
			sourcesText = MessageFormat.format(manySourcesPattern, sourceCount);
		}
		
		String pattern;
		if ( (aggregateBy != null) && (aggregateBy.equals("description")) && 
				(aggregateValue != null) )
		{
			pattern = bundle.getString("incident.summary.patternByDesc");
			localizedDesc = MessageFormat.format(pattern, aggregateValue, 
					(notificationCount + featureCount), sourcesText);
		}
		else
		{
			String aggregateValueText = "";
			
			if ( (aggregateValue != null) )
			{
				if (aggregateBy.equals("source") == false)
				{
					aggregateValueText = aggregateValue;
				}
			}
			else
			{
				// Differentiate between the 'Others' row and the case where the value
				// of the aggregate attribute is null in the original notification/feature.
				if (aggregate.isAggregateValueNull() == true)
				{
					if (aggregateBy != null)
					{
						String absentPattern = bundle.getString("incident.summary.attributeAbsent");
						aggregateValueText = MessageFormat.format(absentPattern, aggregateBy);
					}
				}
				else
				{
					String otherPattern = bundle.getString("incident.summary.attributeOther");
					if (aggregateBy.equals("source") == false)
					{
						aggregateValueText = MessageFormat.format(otherPattern, aggregateBy);
					}
					else
					{
						aggregateValueText = MessageFormat.format(otherPattern, "");
					}
				}
			}
			
			if (notificationCount > 0)
			{
				if (featureCount > 0)
				{
					// Both notifications and features.
					pattern = bundle.getString("incident.summary.pattern3");
					localizedDesc = MessageFormat.format(pattern, aggregateValueText, 
							notificationCount, featureCount, sourcesText);
				}
				else
				{
					// Notifications only.
					pattern = bundle.getString("incident.summary.pattern1");
					localizedDesc = MessageFormat.format(pattern, aggregateValueText, 
							notificationCount, sourcesText);
				}
			}
			else
			{
				// Features only
				pattern = bundle.getString("incident.summary.pattern2");
				localizedDesc = MessageFormat.format(pattern, aggregateValueText, 
						featureCount, sourcesText);
			}
		}
		
		return localizedDesc;
	}
}
