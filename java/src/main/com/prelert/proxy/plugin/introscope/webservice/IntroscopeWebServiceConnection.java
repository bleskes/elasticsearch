/************************************************************
 *                                                          *
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2012     *
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

package com.prelert.proxy.plugin.introscope.webservice;

import static com.prelert.proxy.plugin.introscope.IntroscopePlugin.PATH_SEPARATOR;
import static com.prelert.proxy.plugin.introscope.IntroscopePlugin.RESOURCE_PATH_ATTRIBUTE;

import java.rmi.RemoteException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import com.prelert.data.Attribute;
import com.prelert.data.Notification;
import com.prelert.data.TimeSeriesConfig;
import com.prelert.data.TimeSeriesData;
import com.prelert.data.TimeSeriesDataPoint;
import com.prelert.proxy.data.SourceConnectionConfig;
import com.prelert.proxy.plugin.introscope.*;

import com.wily.introscope.client.alerts.interfaces.IIntroscopeClient;
import com.wily.introscope.client.alerts.manager.IntroscopeClient;
import com.wily.introscope.server.webservicesapi.IntroscopeWebServicesException;
import com.wily.introscope.server.webservicesapi.alerts.DMgmtModuleAlertDefnSnapshot;
import com.wily.introscope.server.webservicesapi.alerts.IAlertPollingService;
import com.wily.introscope.server.webservicesapi.alerts.ManagementModuleBean;
import com.wily.introscope.server.webservicesapi.metricsdata.IMetricsDataService;
import com.wily.introscope.server.webservicesapi.metricsdata.MetricData;
import com.wily.introscope.server.webservicesapi.metricsdata.TimesliceGroupedMetricData;
import com.wily.introscope.server.webservicesapi.metricslist.IMetricsListService;
import com.wily.introscope.server.webservicesapi.metricslist.Metric;


/**
 *  Introscope Web Services Client.
 *  
 *  Uses the Web Services API to gather Time series metric data and 
 *  alert notifications.
 */
public class IntroscopeWebServiceConnection extends IntroscopeConnection
{
	private static Logger s_Logger = Logger.getLogger(IntroscopeWebServiceConnection.class);
	
	private static final int WEB_SERVICES_PORT = 8081;
	
	/*
	 * Introscope web services client. 
	 */
	private IIntroscopeClient m_IntroscopeClient;
	
	
	@Override
	public void connect(SourceConnectionConfig connectConfig) throws Exception
	{
		if (m_Connected)
		{
			throw new UnsupportedOperationException("IntroscopeWebServiceConnection: a connection is " +
					"already open to " + getHostUsernameString());
		}
		
		m_ConnectionConfig = connectConfig;
		
		m_IntroscopeClient = new IntroscopeClient(m_ConnectionConfig.getHost(), 
									WEB_SERVICES_PORT,
									m_ConnectionConfig.getUsername(),
									m_ConnectionConfig.getPassword(), null);
		
		m_Connected = true;
	}

	
	/**
	 * Does nothing.
	 */
	@Override
	public void logoff()
	{
		m_Connected = false;
	}
	
	
	@Override
	public List<Notification> getAlerts(Date start, Date end, 
									String module, String agent, String alert)
	{
		List<Notification> notifications = new ArrayList<Notification>();
		
		long startEpoch = start.getTime();
		long endEpoch = end.getTime();
		
		
		IAlertPollingService service;
		try
		{
			service = m_IntroscopeClient.getAlertPollingWS();
		}
		catch (Exception e)
		{
			s_Logger.error("getNotifications() cannot access polling service. Exception = " + e);

			return notifications;
		}
		
		try
		{
			DMgmtModuleAlertDefnSnapshot[] alerts = new DMgmtModuleAlertDefnSnapshot[1];

			if (alert == null)
			{
				
				s_Logger.debug(getHostUsernameString() + " WS getAlertSnapshots() module=" + 
										module + ", agent=" + agent);

				// Query agent for all alerts.
				alerts = service.getAlertSnapshots(module, agent);
			}
			else
			{
				s_Logger.debug(getHostUsernameString() + " WS getAlertSnapshot() module=" + 
						module + ", agent=" + agent +  ", alert=" + alert);

				DMgmtModuleAlertDefnSnapshot alertSnapshot = service.getAlertSnapshot(module, agent, alert);

				alerts[0] = alertSnapshot;
				
			}

			
			for (DMgmtModuleAlertDefnSnapshot alertSnapshot : alerts)
			{
				if (alertSnapshot.getTimeOfStatusChange() > startEpoch && alertSnapshot.getTimeOfStatusChange() < endEpoch)
				{
					HostProcessAgent hostProcAgent;
					try
					{
						hostProcAgent = new HostProcessAgent(alertSnapshot.getAgentIdentifier());

					}
					catch (ParseException pe)
					{
						s_Logger.error("getNotifications() parse error: " + pe);
						continue;
					}

					Notification notification = new Notification(getAlertDataType(), hostProcAgent.getHost()); 
					notification.setCount(1);

					String desc = alertSnapshot.getAlertIdentifier() + ": Status " + 
								mapStatusToSeverityString(alertSnapshot.getAlertDefnCurrStatus());
					notification.setDescription(desc);
					notification.setTimeMs(alertSnapshot.getTimeOfStatusChange());

					notification.addAttribute(new Attribute(IntroscopePlugin.AGENT_ATTRIBUTE, hostProcAgent.getAgent()));
					notification.addAttribute(new Attribute(IntroscopePlugin.PROCESS_ATTRIBUTE, hostProcAgent.getProcess()));
					notification.addAttribute(new Attribute(IntroscopePlugin.MANAGEMENT_MODULE_ATTRIBUTE, module));
					notification.addAttribute(new Attribute(IntroscopePlugin.ALERT_PREVIOUS_STATUS, 
									mapStatusToSeverityString(alertSnapshot.getAlertDefnPrevStatus())));					
	

					int severity = mapStatusToPrelertSeverity(alertSnapshot.getAlertDefnCurrStatus());
					notification.setSeverity(severity);

					notifications.add(notification);
				}
			}
		}
		catch (IntroscopeWebServicesException wse)
		{
			s_Logger.error(getHostUsernameString() + " Exception in getAlertSnapshots, with module=" +
								module + " agent= " + agent + " alert= " + alert);
			s_Logger.error(wse);
		}
		catch (RemoteException re)
		{
			s_Logger.error(getHostUsernameString() + " Exception in getAlertSnapshots, with module=" + 
								module + " agent= " + agent + " alert= " + alert);
			s_Logger.error(re);
		}
		
		
		return notifications;
	}
	
	
	@Override
	public List<String> listAgents(String agentRegex)
	{
		s_Logger.info(getHostUsernameString() + " WS listAgents() agentRegex=" + agentRegex);
		
		List<String> agents = new ArrayList<String>();

		try
		{
			
			IMetricsListService service = m_IntroscopeClient.getMetricListWS();
			String [] agentPaths = service.listAgents(agentRegex);
			
			agents = Arrays.asList(agentPaths);
			
		}
		catch (IntroscopeWebServicesException e)
		{
			s_Logger.error("IntroscopeWebServicesException in listAgents(): " + e);
		}
		catch (RemoteException e)
		{
			s_Logger.error("RemoteException in listAgents(): " + e);
		}
		catch (Exception e)
		{
			s_Logger.error("Exception in listAgents(): " + e);
		}
		
		return agents;
	}
	
	
	@Override
	public List<String> listMetrics(String agentRegex, String metricRegex)
	{

		s_Logger.info(getHostUsernameString() + " WS listMetrics() agentRegex=" + agentRegex +
									"metricRegex=" + metricRegex);
		
		List<String> results = new ArrayList<String>();
		
		try
		{
			IMetricsListService service = m_IntroscopeClient.getMetricListWS();
			Metric[] metrics = service.listMetrics(agentRegex, metricRegex);
			
			for (Metric metric : metrics)
			{
				results.add(metric.getMetricName());
			}
			
		}
		catch (IntroscopeWebServicesException e)
		{
			s_Logger.error("IntroscopeWebServicesException in listMetrics(): " + e);
		}
		catch (RemoteException e)
		{
			s_Logger.error("RemoteException in listMetrics(): " + e);
		}
		catch (Exception e)
		{
			s_Logger.error("Exception in listMetrics(): " + e);
		}
		
		return results;
	}
	
	
	/**
	 * @param moduleRegex <em>Not used in this implementation.</em>
	 */
	@Override
	public List<String> listManagementModules(String moduleRegex)
	{
		List<String> result = new ArrayList<String>();
		
		IAlertPollingService alertPollingService;
		try
		{
			alertPollingService = m_IntroscopeClient.getAlertPollingWS();
			
			ManagementModuleBean[] mods = alertPollingService.getAllIscopeManagmentModules();
			for (ManagementModuleBean module : mods)
			{
				result.add(module.getManModuleName());
			}
			
		}
		catch (IntroscopeWebServicesException we)
		{
			System.out.println("list Management Modules exception : " + we);
		}
		catch (Exception e) 
		{
			System.out.println("list Management Modules exception : " + e);
		}
		
		return result;
	}
	
	
	@Override
	public Collection<TimeSeriesData> getMetricData(String agentRegex, String metricRegex,
												Date start, Date stop, int intervalSecs)
	{
		s_Logger.debug(getHostUsernameString() + " WS getMetricData() agent=" + agentRegex + 
				", metric=" + metricRegex + ", starttime=" + start.getTime() +
				", endtime=" + stop.getTime() + ", interval=" + intervalSecs);
		

		Calendar startCal = Calendar.getInstance(); 
		startCal.setTime(start);
		Calendar endCal = Calendar.getInstance();
		endCal.setTime(stop);
		
		TimesliceGroupedMetricData [] groupedMetricData; 
		try
		{
			IMetricsDataService service = m_IntroscopeClient.getMetricDataWS();		
			
			groupedMetricData = service.getMetricData(agentRegex, metricRegex, 
					 						startCal, endCal, intervalSecs);

		}
		catch (Exception e)
		{
			s_Logger.error("Error in getAllDataPointsForTimeSpan(). Exception = " + e);
			return Collections.emptyList();
		}	
		
		
		HashMap<String, TimeSeriesData> externalKeyToDataMap = new HashMap<String, TimeSeriesData>();

		
		for (int i = 0; i < groupedMetricData.length; i++)
		{
			long timeMs = groupedMetricData[i].getTimesliceStartTime().getTimeInMillis();

			MetricData [] metricData = groupedMetricData[i].getMetricData();
			for (int j = 0; j < metricData.length; ++j)
			{
				String externalKey = AgentMetricPair.createExternalKey(
						metricData[j].getAgentName(), 
						metricData[j].getMetricName());


				TimeSeriesData timeSeriesData = externalKeyToDataMap.get(externalKey);
				if (timeSeriesData == null)
				{
					timeSeriesData = createTimeSeriesDataFromMetric(metricData[j], externalKey);
					externalKeyToDataMap.put(externalKey, timeSeriesData);
				}


				Double value = -1.0;
				try 
				{
					value = Double.parseDouble(metricData[j].getMetricValue());
				}
				catch (NumberFormatException e)
				{
					continue;
				}

				TimeSeriesDataPoint pt = new TimeSeriesDataPoint(timeMs, value);
				timeSeriesData.getDataPoints().add(pt);
			}
		}
		
		return externalKeyToDataMap.values();
	}
	
	
	/**
	 * Returns a <code>TimeSeriesData</code> structure with the config set
	 * from the metric data.
	 * 
	 * @param metricData
	 * @param externalKey
	 * @return A time series data structure
	 */
	private TimeSeriesData createTimeSeriesDataFromMetric(MetricData metricData, String externalKey)
	{
		TimeSeriesConfig config = createTimeSeriesConfigFromMetric(metricData, externalKey);

		return new TimeSeriesData(config, new ArrayList<TimeSeriesDataPoint>());
	}


	/**
	 * Creates a new <code>TimeSeriesConfig</code> with its externalKey property
	 * set to <code>externalKey</code> and any other attributes set.
	 * 
	 * @param datatype
	 * @param metricData
	 * @param externalKey
	 * @return
	 */
	private TimeSeriesConfig createTimeSeriesConfigFromMetric(MetricData metricData, 
															String externalKey)
	{
		HostProcessAgent hostProcAgent;
		try
		{
			hostProcAgent = new HostProcessAgent(metricData.getAgentName());

		}
		catch (ParseException pe)
		{
			s_Logger.error("Agent path should be of the form (.*)\\|(.*)\\|(.*). Arg=" 
					+ metricData.getAgentName());

			throw new IllegalArgumentException("Agent path should be of the form (.*)\\|(.*)\\|(.*). Arg=" 
					+ metricData.getAgentName());
		}

		String source = hostProcAgent.getHost();

		String metric = "";
		String [] metricSplit = metricData.getMetricName().split(":");
		if (metricSplit.length > 0)
		{
			metric = metricSplit[metricSplit.length -1];
		}

		TimeSeriesConfig config = new TimeSeriesConfig(getMetricDataType(), metric, source);
		config.setExternalKey(externalKey);

		List<Attribute> attributes = new ArrayList<Attribute>();
		attributes.add(new Attribute(IntroscopePlugin.PROCESS_ATTRIBUTE, hostProcAgent.getProcess(),
									PATH_SEPARATOR, 1));
		attributes.add(new Attribute(IntroscopePlugin.AGENT_ATTRIBUTE, hostProcAgent.getAgent(),
									PATH_SEPARATOR, 2));
	
		if (metricSplit.length > 1) // has resource path and metric
		{
			String [] resourcePaths = metricSplit[0].split("\\|");
			for (int i = 0; i < resourcePaths.length; i++)
			{
				Attribute attr = new Attribute(RESOURCE_PATH_ATTRIBUTE + i, resourcePaths[i],
													PATH_SEPARATOR, i + 3);
				attributes.add(attr);
			}
		}
		
		config.setAttributes(attributes);

		return config;
	}


	@Override
	public boolean isConnected() 
	{
		return m_Connected;
	}


	/**
	 * This function is not supported in the Web Services. 
	 * It will not return it simply throws a UnsupportedOperationException.
	 */
	@Override
	public Collection<TimeSeriesData> getMetricDataForLastNMinutes(String agentRegex,
						String metricRegex, int lastNMinutes, int intervalSecs)
	throws ConnectionException 
	{
		throw new UnsupportedOperationException("Cannot getMetricData for the last N minutes.");
	}


}
