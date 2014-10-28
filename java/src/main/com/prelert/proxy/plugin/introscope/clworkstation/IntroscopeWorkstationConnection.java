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

package com.prelert.proxy.plugin.introscope.clworkstation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import com.prelert.data.Attribute;
import com.prelert.data.Notification;
import com.prelert.data.TimeSeriesConfig;
import com.prelert.data.TimeSeriesData;
import com.prelert.data.TimeSeriesDataPoint;
import com.prelert.proxy.data.SourceConnectionConfig;
import com.prelert.proxy.plugin.introscope.IntroscopeConnection;
import com.prelert.proxy.plugin.introscope.IntroscopePlugin;
import com.wily.introscope.clws.ISessionHandle;
import com.wily.introscope.clws.protocol.LogonCredentials;
import com.wily.introscope.spec.server.beans.clw.ICommandLineWorkstationService;
import com.wily.isengard.IsengardException;
import com.wily.isengard.messageprimitives.service.MessageServiceFactory;
import com.wily.isengard.messageprimitives.service.ServiceException;
import com.wily.util.feedback.ApplicationFeedback;

public abstract class IntroscopeWorkstationConnection extends IntroscopeConnection 
{
	protected static final String LIST_AGENTS_CMD = "list agents matching %1$s";
	
	protected static final String HISTORICAL_DATA_CMD = "get historical data from agents matching %1$s " +
			"and metrics matching %2$s between %3$tY/%3$tm/%3$td %3$tH:%3$tM:%3$tS and " +
			"%4$tY/%4$tm/%4$td %4$tH:%4$tM:%4$tS with frequency of %5$d seconds";
	
	protected static final String RECENT_DATA_CMD = "get historical data from agents matching %1$s " +
			"and metrics matching %2$s for past %3$d minutes " +
			"with frequency of %4$d seconds";
	
	protected static final String LIST_MAN_MODULES_CMD = "list management modules matching (%1$s)";

	protected static final String ALERT_AGENT_PATH = 
			"Custom Metric Host \\(Virtual\\)\\|Custom Metric Process \\(Virtual\\)\\|Custom Metric Agent \\(Virtual\\)";

	
	private static Logger s_Logger = Logger.getLogger(IntroscopeWorkstationConnection.class);
	
	protected ISessionHandle m_EnterpriseManager;
	protected ICommandLineWorkstationService m_ClwService;
	
	
	/**
	 * Objects to handle closing the workstation connection 
	 * after a timeout period has expired.
	 */
	private static final long RESET_CONNECTION_TIMEOUT_MS = 30 * 60 * 1000; // 30 minutes
	private Timer m_DisconnectTimer;
	private CloseConnectionTimerTask m_DisconnectTimerTask;

	/**
	 * Stops the connection being closed when it is in use.
	 */
	final protected Object m_ConnectionMutex = new Object();
	
	
	public IntroscopeWorkstationConnection()
	{
		m_DisconnectTimer = new Timer();
		m_DisconnectTimerTask = new CloseConnectionTimerTask();
	}

	/**
	 * Connect to the Enterprise Manager and create the callback
	 * notification classes.
	 */
	@Override
	public void connect(SourceConnectionConfig connectConfig) throws Exception
	{
		if (m_Connected)
		{
			throw new UnsupportedOperationException("IntroscopeWorkstationConnection: a connection is " +
								"already open to " + getHostUsernameString());
		}
		
		s_Logger.debug("New Connection with params " + connectConfig);
		
		m_ConnectionConfig = connectConfig;
		
		LogonCredentials logon = new LogonCredentials();
		logon.setUserName(connectConfig.getUsername());
		logon.setPassword(connectConfig.getPassword());
		logon.setHostName(connectConfig.getHost());
		logon.setPort(new Integer(connectConfig.getPort()).toString());
		
		ApplicationFeedback feedback = new ApplicationFeedback("CLW");
		feedback.setShouldBuffer(false);
		
		try
		{
			m_EnterpriseManager = logon.logon(feedback);
		}
		catch (IsengardException e)
		{
			s_Logger.error(String.format("Could not connect to an Enterprise Manager with connection params " +
										connectConfig));
			s_Logger.error(e);
			
			throw e;
		}

		try 
		{
			 m_ClwService = (ICommandLineWorkstationService)MessageServiceFactory.getService(
					 											m_EnterpriseManager.getPostOffice(), 
																ICommandLineWorkstationService.class);
		} 
		catch (com.wily.isengard.messageprimitives.ConnectionException e) 
		{
			s_Logger.error("Could not get the Command Line Workstation Service: " + e);
			return;
		}
		catch (ServiceException e) 
		{
			s_Logger.error("Could not get the Command Line Workstation Service: " + e);
			return;
		}
		
		
		resetDisconnectTimer();
		
		m_Connected = true;
	}
							
	
	/**
	 * Reconnects to the Enterprise manager if the connection has
	 * been closed.
	 */
	protected void reconnectIfDisconnected()
	{
		if (isConnected() == false)
		{
			try 
			{
				connect(m_ConnectionConfig);
			} 
			catch (Exception e) 
			{
				s_Logger.error("Cannot reconnect Command Line Workstation Service: " + e);
			}
		}
	}
	
	/**
	 * Closes the connection.
	 * isConnected() will always return false after this.
	 */
	protected void disconnect()
	{
		logoff();
	}
	
	/**
	 * isConnected() will always return false after this 
	 * function is called even if the logoff errors.
	 */
	@Override
	public void logoff() 
	{
		if (m_DisconnectTimer != null)
		{
			m_DisconnectTimer.cancel();
		}
		m_DisconnectTimer = null;
		
		if (isConnected())
		{
			s_Logger.debug("Closing Connection");
		}
		else
		{
			s_Logger.debug("Closing connection that is already closed");
		}
		
		try 
		{
			m_EnterpriseManager.logoff();
		}
		catch (Exception e) 
		{
			s_Logger.error(getHostUsernameString() + 
					" Error logging off from the Enterprise manager: " + e);
		}
		finally
		{
			m_Connected = false;
		}
	}


	/**
	 * Returns all the alerts between start and end dates for the given
	 * module and alert if specified. 
	 * 
	 * @param start
	 * @param end
	 * @param moduleRegex - If null then all alerts are returned.
	 * @param agent - Unused
	 * @param alertRegex - Can be a null value in which case all the alerts
	 *                for the module are checked.
	 * @return
	 * @throws ConnectionException - if the connection errors or the thread is 
	 * 								 interrupted.
	 */
	@Override
	public List<Notification> getAlerts(Date start, Date end, String moduleRegex,
								String agent, String alertRegex) 
	throws ConnectionException
	{
		// get an extra point so we don't miss any status changes.
		Date preStartTime = new Date(start.getTime() - 
								IntroscopeConnection.DEFAULT_INTERVAL * 1000);
				
		String module;
		if (moduleRegex == null)
		{
			module = "Alerts\\|.*";
		}
		else
		{
			module = "Alerts\\|" + moduleRegex;
		}
		
		
		String metricRegex;
		if (alertRegex == null)
		{
			if (moduleRegex == null)
			{
				metricRegex = "Alerts\\|.*";
			}
			else
			{
				metricRegex = module + ".*";
			}
		}
		else
		{
			metricRegex = module + ":" + alertRegex;
		}
		
		Collection<TimeSeriesData> data = getMetricData(ALERT_AGENT_PATH, metricRegex, preStartTime, end, 
											IntroscopeConnection.DEFAULT_INTERVAL);
		
		return processTimeSeriesForNotifications(data, module);
	}
	
	/**
	 * Processes the alert time series for alerts. Generates an
	 * alert whenever the time series changes.
	 * 
	 * @param timeSeriesData
	 * @param moduleName - Alert module used as the Alert source.
	 * @return
	 */
	private List<Notification> processTimeSeriesForNotifications(Collection<TimeSeriesData> timeSeriesData, 
																String moduleName)
	{
		List<Notification> notifications = new ArrayList<Notification>();
		
		for (TimeSeriesData data : timeSeriesData)
		{
			List<TimeSeriesDataPoint> points = data.getDataPoints();
			
			double prevValue = 0.0;
			if (points.size() > 0)
			{
				prevValue = points.get(0).getValue();
			}
			
			for (int i=1; i<points.size(); i++)
			{
				if (prevValue != points.get(i).getValue())
				{
					// Create a new notification
					Notification notification = new Notification(getAlertDataType(), moduleName); 
					notification.setCount(1);

					String desc = data.getConfig().getMetric() + ": Status " + 
								mapStatusToSeverityString((int)points.get(i).getValue());
					notification.setDescription(desc);
					notification.setTimeMs(points.get(i).getTime());

					notification.addAttribute(new Attribute(IntroscopePlugin.ALERT_PREVIOUS_STATUS, 
									mapStatusToSeverityString((int)prevValue)));					
	

					int severity = mapStatusToPrelertSeverity((int)points.get(i).getValue());
					notification.setSeverity(severity);

					notifications.add(notification);
					
					prevValue = points.get(i).getValue();
				}
				
				
			}
		}
		
		return notifications;
	}
	
	
	/**
	 */
	@Override
	public List<String> listMetrics(String agentRegex, String metricRegex) 
	throws ConnectionException
	{
		final long QUERY_PERIOD = 30 * 1000;
		
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		cal.add(Calendar.HOUR_OF_DAY, -1);
		
		Date end = cal.getTime();
		Date start = new Date(end.getTime() - QUERY_PERIOD);

		Set<String> metrics = new HashSet<String>();
		
		Collection<TimeSeriesData> datas = getMetricData(agentRegex, metricRegex,
														start, end);
		
		for (TimeSeriesData data : datas)
		{
			TimeSeriesConfig config = data.getConfig();
			StringBuilder metricPath = new StringBuilder();

			// Sort attributes so resource paths are in order.
			Collections.sort(config.getAttributes());

			for (Attribute attr : config.getAttributes())
			{
				if (attr.getAttributeName().startsWith(IntroscopePlugin.RESOURCE_PATH_ATTRIBUTE))
				{
					// Want '|' character to appear ONLY between metrics on the path
					if (metricPath.length() == 0)
					{
						metricPath.append(attr.getAttributeValue());
					}
					else
					{
						metricPath.append("|");
						metricPath.append(attr.getAttributeValue());
					}
				}
			}
			
			metricPath.append(":");
			metricPath.append(config.getMetric());
			
			metrics.add(metricPath.toString());
		}
		
		List<String> metricList = new ArrayList<String>(metrics);
		Collections.sort(metricList);
		
		return metricList;
	}
		
	/**
	 * Returns true if the Workstation connection is open.
	 * @return
	 */
	@Override
	public boolean isConnected()
	{
		return m_Connected;
	}
			
	
	/**
	 * Resets the disconnect timer by cancelling the current task
	 * and starting another.
	 */
	protected void resetDisconnectTimer()
	{
		m_DisconnectTimerTask.cancel();

		if (m_DisconnectTimer == null) // previously cancelled.
		{
			m_DisconnectTimer = new Timer();
		}
		else
		{
			m_DisconnectTimer.purge();
		}

		m_DisconnectTimerTask = new CloseConnectionTimerTask();
		m_DisconnectTimer.schedule(m_DisconnectTimerTask, RESET_CONNECTION_TIMEOUT_MS);        
	}
	
	/**
	 * Timer Task for logging of the workstation connection.
	 *
	 */
	private class CloseConnectionTimerTask extends TimerTask 
	{
		@Override
		public void run() 
		{
			synchronized(m_ConnectionMutex)
			{
				IntroscopeWorkstationConnection.s_Logger.info(
						"Closing the Workstation connection as unused for " +
						RESET_CONNECTION_TIMEOUT_MS + "ms");

				IntroscopeWorkstationConnection.this.logoff();
			}
		}
	}
}
