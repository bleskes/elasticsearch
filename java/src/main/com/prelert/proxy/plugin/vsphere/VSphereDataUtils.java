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

package com.prelert.proxy.plugin.vsphere;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.prelert.data.Attribute;
import com.prelert.data.Notification;
import com.prelert.data.TimeSeriesConfig;
import com.prelert.data.TimeSeriesData;
import com.prelert.data.TimeSeriesDataPoint;
import com.prelert.proxy.plugin.vsphere.VSpherePerformanceData.PerformanceData;
import com.vmware.vim25.AlarmActionTriggeredEvent;
import com.vmware.vim25.AlarmStatusChangedEvent;
import com.vmware.vim25.DynamicData;
import com.vmware.vim25.EntityEventArgument;
import com.vmware.vim25.Event;
import com.vmware.vim25.EventEx;
import com.vmware.vim25.KeyAnyValue;
import com.vmware.vim25.LocalizableMessage;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.PerfCounterInfo;
import com.vmware.vim25.PerfEntityMetric;
import com.vmware.vim25.PerfMetricIntSeries;
import com.vmware.vim25.PropertyChange;
import com.vmware.vim25.TaskInfo;
import com.vmware.vim25.TaskInfoState;
import com.vmware.vim25.TaskReason;
import com.vmware.vim25.TaskReasonAlarm;
import com.vmware.vim25.TaskReasonSchedule;
import com.vmware.vim25.TaskReasonUser;

/**
 * Utility class for vSphere Web Services API. Provides static toString
 * methods for some vSphere data types and conversion functions for 
 * vSphere -> Prelert data objects. 
 */
public class VSphereDataUtils 
{
	static Logger s_Logger = Logger.getLogger(VSphereDataUtils.class);
	
	/**
	 * When the source cannot determined  
	 */
	private static final String API_SOURCE = "vCenter";
	
	/**
	 * Never instantiate this class
	 */
	private VSphereDataUtils()
	{
		
	}
	
	/**
	* Returns a string representation of <code>PropertyChange</code>.
	* @param propChange
	* @return
	*/
	static public String propertyChangeToString(PropertyChange propChange)
	{
		StringBuilder builder = new StringBuilder("[");
		builder.append(propChange.getName() + ", ");
		builder.append(propChange.getOp() + ", ");
		builder.append(propChange.getVal());
		builder.append("]");

		return builder.toString();      
	}


	
	
	/**
	 * Convert vSphere performance data to Prelert TimeSeriesData.
	 * vSphere time series data has a <code>counterId</code> which is used 
	 * to look up details of the performance counter. The 
	 * <code>PerformanceData</code> type encapsulates all the required details.
	 * 
	 * @param entityIdToHostName maps managed object reference id to 
	 * 	      a human display name for the entity (VM, Host, DataCenter, etc).
	 * @param perfData
	 * @return
	 */
	static List<TimeSeriesData> performanceDataToTimeSeries(PerformanceData perfData,
											Map<String, String> entityIdToHostName)
	{
		List<TimeSeriesData> results = new ArrayList<TimeSeriesData>();
		
		PerfEntityMetric entityMetric = perfData.getPerfEntityMetric();
		
		String entity = entityMetric.getEntity().get_value();
		String source = entity;
		
		if (entityIdToHostName.containsKey(entity))
		{
			source = entityIdToHostName.get(entity);
		}
		
		if (entityMetric.getSampleInfo() == null)
		{
			// This could happen if the query interval is smaller than the sample interval.
			return results;
		}
		
		for (int i = 0 ; i < entityMetric.getValue().length; i++)
		{
			int counterId = entityMetric.getValue(i).getId().getCounterId();
			PerfCounterInfo counterInfo = perfData.getCounterInfoMap().get(counterId);
			if (counterInfo == null)
			{
				s_Logger.error("Could not locate counter for metric counter id = " 
								+ counterId);
				continue;
			}
			
			StringBuilder dataTypeBuilder = new StringBuilder("VSphere ");
			dataTypeBuilder.append(counterInfo.getGroupInfo().getLabel());
			
			String metric = counterInfo.getNameInfo().getLabel() + " (" + counterInfo.getUnitInfo().getLabel() + ")";
			
			TimeSeriesConfig config = new TimeSeriesConfig(
								dataTypeBuilder.toString(),
								metric,
								source);
			
			StringBuilder description = new StringBuilder(counterInfo.getNameInfo().getSummary());
			if (counterInfo.getUnitInfo() != null)
			{
				description.append(". ");
				description.append(counterInfo.getUnitInfo().getSummary());
			}
			config.setDescription(description.toString());
			
			List<Attribute> attributes = new ArrayList<Attribute>();

			String counterInstance = entityMetric.getValue(i).getId().getInstance();
			if (counterInstance != null && !counterInstance.equals(""))
			{
				String counterName = counterInfo.getGroupInfo().getLabel();				
				attributes.add(new Attribute(counterName, counterInstance));
			}
					
			config.setAttributes(attributes);
							
			List<TimeSeriesDataPoint> points = new ArrayList<TimeSeriesDataPoint>();

			PerfMetricIntSeries intSeries = (PerfMetricIntSeries)entityMetric.getValue(i);
			for (int j = 0; j < intSeries.getValue().length; j++)
			{
				long value = intSeries.getValue(j);
				long time = entityMetric.getSampleInfo(j).getTimestamp().getTimeInMillis();

				points.add(new TimeSeriesDataPoint(time, value));
			}
			
			results.add(new TimeSeriesData(config, points));
		}
	
		return results;
	}
	
	/**
	 * Constructs a Prelert Notification from a VSphere Event object.
	 * 
	 * @param entityIdToResourceName maps managed object reference id to 
	 * 	      a human display name for the entity (VM, Host, DataCenter, etc).
	 * @param event
	 * @return
	 */
	static public Notification eventToNotification(Event event, 
												Map<String, String> entityIdToResourceName)
	{
		String preferedHostId = null;
		if (event.getVm() != null)
		{
			preferedHostId = event.getVm().getVm().get_value();
		}			
		else if (event.getHost() != null)
		{
			preferedHostId = event.getHost().getHost().get_value();
		}		
		else if (event.getComputeResource() != null)
		{
			preferedHostId = event.getComputeResource().getComputeResource().get_value();
		}	
		else if (event.getDatacenter() != null)
		{
			preferedHostId = event.getDatacenter().getDatacenter().get_value();
		}
			
		String source = API_SOURCE;
		if (preferedHostId != null && 
					entityIdToResourceName.containsKey(preferedHostId))
		{
			source = entityIdToResourceName.get(preferedHostId);
		}
					
		Notification result = new Notification("VSphere Event",	source);
		
		result.setTimeMs(event.getCreatedTime().getTimeInMillis());
		result.setDescription(event.getClass().getSimpleName());
		               
		result.setCount(1);
		result.setSeverity(getEventSeverity(event));

		
		result.setAttributes(eventAttributesFromObjectProperties(event, entityIdToResourceName));
		result.addAttribute(new Attribute("Message", event.getFullFormattedMessage()));
		
		addAttributeIfNotNullOrEmpty("Username", event.getUserName(), result);		
		
		addAttributeIfNotNull(attributeFromEntityEventArg("", event.getVm(), entityIdToResourceName),result);
		addAttributeIfNotNull(attributeFromEntityEventArg("", event.getHost(), entityIdToResourceName), result);
		addAttributeIfNotNull(attributeFromEntityEventArg("", event.getDatacenter(), entityIdToResourceName), result);
		addAttributeIfNotNull(attributeFromEntityEventArg("", event.getComputeResource(), entityIdToResourceName), result);
		addAttributeIfNotNull(attributeFromEntityEventArg("", event.getDs(), entityIdToResourceName), result);
		addAttributeIfNotNull(attributeFromEntityEventArg("", event.getNet(), entityIdToResourceName), result);
		addAttributeIfNotNull(attributeFromEntityEventArg("", event.getDvs(), entityIdToResourceName), result);
	
		// TODO fix the database proc which hangs if duplicate 
		// attributes are added. See the Postgres function add_evidence();
		filterDuplicateAttributes(result);
		
		
		return result;
	}
	
	// TODO this function shouldn't be necessary.
	/**
	 * The Prelert database procs don't like duplicate attributes so use 
	 * this as a sanity check to filter them out.
	 * Logs an error message if a duplicate is found. 
	 * 
	 * @param attributes
	 */
	static private void filterDuplicateAttributes(Notification notification)
	{
		Set<String> attributeNames = new HashSet<String>();
		
		for (Attribute attr : notification.getAttributes())
		{
			if (!attributeNames.add(attr.getAttributeName()))
			{
				s_Logger.error("Duplicate Attribute = " + attr.getAttributeName());
			}
		}
		
		List<Attribute> filteredAttrs = new ArrayList<Attribute>();
		for (Attribute attr : notification.getAttributes())
		{
			if (attributeNames.contains(attr.getAttributeName()))
			{
				filteredAttrs.add(attr);
				attributeNames.remove(attr.getAttributeName());
			}
		}
		
		notification.setAttributes(filteredAttrs);
	}
	
	
	/**
	 * If a subclass of <code>EntityEventArgument</code> contains a 
	 * <code>ManagedObjectReference</code> to an known entity then
	 * look up the resource name and add as an attribute.
	 * 
	 * @param attributeNamePrefix Prefix the returned attribute name with 
	 * 							  <code>attributeNamePrefix</code> + "."
	 * 							  This is to prevent attributes being created with the 
	 *  						  same name when recursively introspecting the objects
	 * 							  members.					
	 * @param eventArg
	 * @param entityIdToResourceName
	 * @return <code>null</code> or an <code>Attribute</code>.
	 */
	static private Attribute attributeFromEntityEventArg(String attributeNamePrefix,
												EntityEventArgument eventArg,
												Map<String, String> entityIdToResourceName)
	{
		if (eventArg == null)
		{
			return null;
		}
		
		Attribute attribute = null;

		try 
		{
			BeanInfo eventInfo = Introspector.getBeanInfo(eventArg.getClass(), EntityEventArgument.class);
			PropertyDescriptor[] props = eventInfo.getPropertyDescriptors();
			
			for (PropertyDescriptor prop : props)
			{
				Method readMethod = prop.getReadMethod();
					try 
					{
						if (readMethod.getReturnType().equals(ManagedObjectReference.class))
						{
							ManagedObjectReference ref = 
											(ManagedObjectReference)readMethod.invoke(eventArg, new Object[0]);

							String entity = entityIdToResourceName.get(ref.get_value());
							if (entity == null)
							{
								entity = eventArg.getName();
							}
							attribute = new Attribute(attributeNamePrefix + prop.getName(), entity);
						}
					} 
					catch (IllegalArgumentException e) 
					{
						s_Logger.error("eventAttributes() IllegalArgumentException = " + e);
					} 
					catch (IllegalAccessException e) 
					{
						s_Logger.error("eventAttributes() IllegalAccessException = " + e);
					}
					catch (InvocationTargetException e) 
					{
						s_Logger.error("eventAttributes() InvocationTargetException = " + e);
					}
								
			}
		} 
		catch (IntrospectionException e) 
		{
			s_Logger.error(e);
		}
		
		return attribute;	
	}
	
	
	/**
	 * Get properties from the event.
	 * 
	 * Uses Introspection to generate attributes from the event.
	 * If a property is a String or DynamicData then an attribute
	 * is created.
	 * 
	 * @param event
	 * @param entityIdToResourceName map from managed object reference id
	 *        to a display name.
	 * @return
	 */
	static private List<Attribute> eventAttributesFromObjectProperties(Event event, 
								Map<String, String> entityIdToResourceName)
	{
		List<Attribute> attributes = new ArrayList<Attribute>();

		try 
		{
			BeanInfo eventInfo = Introspector.getBeanInfo(event.getClass(), Event.class);
			PropertyDescriptor[] props = eventInfo.getPropertyDescriptors();
			
			for (PropertyDescriptor prop : props)
			{
				Method readMethod = prop.getReadMethod();
					try 
					{
						if (readMethod.getReturnType().equals(String.class))
						{
							String value = (String)readMethod.invoke(event, new Object[0]);

							attributes.add(new Attribute(prop.getName(), value));
						}
						else if (EntityEventArgument.class.isAssignableFrom(readMethod.getReturnType()))
						{
							EntityEventArgument eventArg = (EntityEventArgument)readMethod.invoke(event, new Object[0]);
							Attribute attr = attributeFromEntityEventArg(prop.getName() + ".", eventArg, entityIdToResourceName);
							if (attr != null)
							{
								attributes.add(attr);
							}
						}
						else if (DynamicData.class.isAssignableFrom(readMethod.getReturnType()))
						{
							DynamicData obj = (DynamicData)readMethod.invoke(event, new Object[0]);
							attributes.addAll(attributesFromObject(prop.getName(), obj));
						}
					} 
					catch (IllegalArgumentException e) 
					{
						s_Logger.error("eventAttributes() IllegalArgumentException = " + e);
					} 
					catch (IllegalAccessException e) 
					{
						s_Logger.error("eventAttributes() IllegalAccessException = " + e);
					}
					catch (InvocationTargetException e) 
					{
						s_Logger.error("eventAttributes() InvocationTargetException = " + e);
					}
								
			}
		} 
		catch (IntrospectionException e) 
		{
			s_Logger.error(e);
		}
		
		return attributes;
	}
	
	
	/**
	 * Recursively list all the properties of the DynamicData object 
	 * return all Strings in the list of attributes.
	 * 
	 * @param objectName 
	 * @param dynamicObject
	 * @return
	 */
	static private List<Attribute> attributesFromObject(String objectName, DynamicData dynamicObject)
	{
		if (dynamicObject == null)
		{
			return Collections.emptyList();
		}
		
		List<Attribute> attributes = new ArrayList<Attribute>();
		
		if (dynamicObject instanceof EntityEventArgument)
		{
			attributes.add(new Attribute(objectName + ".name", ((EntityEventArgument)dynamicObject).getName()));
			return attributes;
		}
		
		
		try 
		{
			BeanInfo eventInfo = Introspector.getBeanInfo(dynamicObject.getClass(), DynamicData.class);
			PropertyDescriptor[] props = eventInfo.getPropertyDescriptors();
			
			for (PropertyDescriptor prop : props)
			{
				try 
				{
					Method readMethod = prop.getReadMethod();
					if (readMethod.getReturnType().equals(String.class))
					{
						String objProp = objectName + "." + prop.getName();
						String value = (String)readMethod.invoke(dynamicObject, new Object[0]);

						attributes.add(new Attribute(objProp, value));
					}
					else if (readMethod.getReturnType().equals(DynamicData.class))
					{
						String objProp = objectName + "." + prop.getName();
						// is some complex type so call this recursively 
						attributes.addAll(attributesFromObject(objProp, (DynamicData)readMethod.invoke(dynamicObject, new Object[0])));

					}
				} 
				catch (IllegalArgumentException e) 
				{
					s_Logger.error("eventAttributes() IllegalArgumentException = " + e);
				} 
				catch (IllegalAccessException e) 
				{
					s_Logger.error("eventAttributes() IllegalAccessException = " + e);
				}
				catch (InvocationTargetException e) 
				{
					s_Logger.error("eventAttributes() InvocationTargetException = " + e);
				}


			}
		} 
		catch (IntrospectionException e) 
		{
			s_Logger.error(e);
		}
		
		
		return attributes;
	}
	
	/**
	 * Returns a Prelert Severity value for 
	 * If the severity cannot be determined from the Event the default
	 * value Clear=! is returned.
	 * 
	 * See the javadoc for {@link Notification} for a description of 
	 * severity values.
	 * 
	 * @param event
	 * @return Prelert severity value for the Event.
	 */
	static public int getEventSeverity(Event event)
	{
		int severity = 1; // default is clear
		
		if (event instanceof EventEx)
		{
			String severityString = ((EventEx)event).getSeverity();
			
			if ("error".equals(severityString))
			{
				severity = 6;
			}
			else if ("warning".equals(severityString))
			{
				severity = 4;
			}			
		}
		else if (event instanceof AlarmStatusChangedEvent)
		{
			String status = ((AlarmStatusChangedEvent)event).getTo();
			if ("red".equals(status))
			{
				severity = 6;
			}
			else if ("yellow".equals(status))
			{
				severity = 4;
			}
		}
		else if (event instanceof AlarmActionTriggeredEvent)
		{
			severity = 6;
		}
		
		
		return severity;
	}
	
	
	/**
	 * Constructs a Prelert Notification from a VSphere TaskInfo object.
	 * 
	 * @param entityIdToResourceName maps managed object reference id to 
	 * 	      a human display name for the entity (VM, Host, DataCenter, etc).
	 * @param taskInfo
	 * @return
	 */
	static public Notification taskInfoToNotification(TaskInfo taskInfo, 
			 				Map<String, String> entityIdToResourceName)
	{
		// Default source is the entity name. 
		// If the entity is null the use the vCenter API name
		// else try to get the hostname from the map.
		String source = taskInfo.getEntityName();
		if (taskInfo.getEntity() == null)
		{
			source = API_SOURCE;
		}
		else if (entityIdToResourceName.containsKey(taskInfo.getEntity().get_value()))
		{
			source = entityIdToResourceName.get(taskInfo.getEntity().get_value());
		}
		
		Notification result = new Notification("VSphere Event", source);
		
		int severity = 1;
		StringBuilder description = new StringBuilder();
		if (taskInfo.getDescription() != null)
		{
			description.append(localizableMessageToString(taskInfo.getDescription()));
			description.append(": ");
		}
		
		if (taskInfo.isCancelled())
		{
			description.append("Task Cancelled.");
		}
		if (taskInfo.getState().equals(TaskInfoState.queued))
		{
			result.setTimeMs(taskInfo.getQueueTime().getTimeInMillis());
			description.append("Task queued.");
		}
		else if (taskInfo.getState().equals(TaskInfoState.running))
		{
			description.append("Task running.");
			result.setTimeMs(taskInfo.getStartTime().getTimeInMillis());
		}
		else if (taskInfo.getState().equals(TaskInfoState.success))
		{
			description.append("Task completed successfully.");
			result.setTimeMs(taskInfo.getCompleteTime().getTimeInMillis());
		}
		else if (taskInfo.getState().equals(TaskInfoState.error))
		{
			description.append("Task error.");
			result.setTimeMs(taskInfo.getCompleteTime().getTimeInMillis());
			severity = 6;
		}
			
		result.setDescription(description.toString());
		
		result.getAttributes().addAll(attributesFromTaskReason(taskInfo.getReason()));		
		
		result.setCount(1);
		result.setSeverity(severity);
		
		if (taskInfo.getError() != null && taskInfo.getError().getLocalizedMessage() != null)
		{
			result.addAttribute(new Attribute("Error Message", taskInfo.getError().getLocalizedMessage()));
		}
		
		return result;
	}
	
	
	static private void addAttributeIfNotNull(Attribute attr, Notification notification)
	{
		if (attr != null)
		{
			notification.addAttribute(attr);
		}
	}
	
	/**
	 * Adds a new attribute to notification if the attribute value is 
	 * non-null and not a empty string.
	 * @param name
	 * @param value
	 * @param notification modified if value is non-null and not an
	 * 		  empty string.
	 */
	static private void addAttributeIfNotNullOrEmpty(String name, String value, Notification notification)
	{
		if (value != null && !value.isEmpty())
		{
			notification.addAttribute(new Attribute(name, value));
		}
	}
	
	/**
	 * Convert the Task reason which is what initiated the task 
	 * to attributes.
	 * @param reason
	 * @return
	 */
	static private List<Attribute> attributesFromTaskReason(TaskReason reason)
	{
		List<Attribute> attrs = new ArrayList<Attribute>();
		
		if (reason instanceof TaskReasonAlarm)
		{
			attrs.add(new Attribute("Alarm Name", ((TaskReasonAlarm)reason).getAlarmName()));
			attrs.add(new Attribute("Alarm Source Name", ((TaskReasonAlarm)reason).getEntityName()));
		}
		else if (reason instanceof TaskReasonSchedule)
		{
			attrs.add(new Attribute("Scheduled Task Name", ((TaskReasonSchedule)reason).getName()));
		}
		else if (reason instanceof TaskReasonUser)
		{
			attrs.add(new Attribute("User Name", ((TaskReasonUser)reason).getUserName()));
		}
		
		return attrs;
	}
	
	
	/**
	 * Replaces '{arg}' strings in the message with values from the message's
	 * key-value store.
	 * @param msg
	 * @return
	 */
	static private String localizableMessageToString(LocalizableMessage msg)
	{
		String formattedMsg = msg.getMessage();

		Pattern pattern = Pattern.compile("\\{(.*)\\}");
		Matcher match = pattern.matcher(msg.getMessage());
		while (match.find())
		{
			String argName = match.group();
			for (KeyAnyValue keyValue : msg.getArg())
			{
				if (keyValue.getKey().equals(argName))
				{
					formattedMsg.replaceAll("{" + argName + "}", 
										(String)keyValue.getValue());
				}
			}
		}

		return formattedMsg;
	}
}






