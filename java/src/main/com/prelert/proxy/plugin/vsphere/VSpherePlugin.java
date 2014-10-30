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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.xml.rpc.ServiceException;

import org.apache.log4j.Logger;

import com.prelert.data.ConnectionStatus;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;
import com.prelert.data.Notification;
import com.prelert.data.TimeSeriesData;
import com.prelert.proxy.data.SourceConnectionConfig;
import com.prelert.proxy.plugin.InternalPlugin;
import com.prelert.proxy.plugin.NotificationPlugin;
import com.prelert.proxy.plugin.Plugin;
import com.prelert.proxy.plugin.vsphere.VSpherePerformanceData.PerformanceData;
import com.vmware.security.credstore.CredentialStore;
import com.vmware.security.credstore.CredentialStoreFactory;
import com.vmware.vim25.PerfSampleInfo;
import com.vmware.vim25.RuntimeFault;


/**
 * Plugin for collecting VMware vSphere events, tasks and performance data.
 * 
 * This plugin does not support the retrieval of historical data as VCentre 
 * stores data more than 1 hour older at 5minute intervals. So this plugin is 
 * only used for pulling real-time data out of VCentre.
 */
public class VSpherePlugin extends Plugin implements NotificationPlugin, InternalPlugin
{ 
	static Logger s_Logger = Logger.getLogger(VSpherePlugin.class);
	
	/*
	 * Local copy of the Properties passed to loadProperties(Properties).
	 * Kept for the duplicate() function.
	 */
	private Properties m_Properties;
	
	/*
	 * properties
	 */
	private String m_Username;
	private String m_Host;
	private java.net.URL m_WebServiceUrl;
	private boolean m_IgnoreCert;
	private DataSourceCategory m_DataSourceType;
	private List<VSphereResourceSelection> m_ResourceSelectionFilters;
	private long m_OldestQueryTimeMs;
	
	/*
	 * VSphere Manager objects
	 */
	private VSphereEventCollector m_EventCollector;
	private VSphereTaskCollector m_TaskCollector;
	private VSpherePerformanceData m_PerformanceData;
	
	// Used to keep track of the last datapoint's timestamp.
	// Needed when adjusting for clock skew.
	private Date m_LastQueryEndTime;

		
	static 
	{
		// TODO VSpherePlugin - don't bother with SSL certificates for now.
		System.setProperty("axis.socketSecureFactory",
						"org.apache.axis.components.net.SunFakeTrustSocketFactory");
	}
	
	public VSpherePlugin()
	{
		m_ResourceSelectionFilters = new ArrayList<VSphereResourceSelection>(); 
	}
	
	
	/**
	 * Reads the properties from the <code>properties</code> parameter.
	 * 
	 * The following properties are compulsory and must be set:
	 * <b>username</b> vCenter login username
	 * <b>host</b> vCenter host
	 * <b>url</b> vCenter REST api url.
	 * <b>dataCollectionType</b> Either time_series or notification
	 * 
	 * The following properties are optional:
	 * <b>ignoreCert</b> Ignore SSL certificates if true. Default is
	 * true.
	 * 
	 * <b>password</b> If set this is the password used to connect to 
	 * vCentre. If not set the plugin will look for a password in the
	 * vCenter credential store.
	 * 
	 * <b>dataCenterRegex</b> Only pull performance data from Data Centres
	 * that match this regular expression.
	 * 
	 * <b>vmRegex</b> Only pull performance data from VMs that match
	 * this regular expression.
	 * 
	 * <b>oldestQueryTime</b> If no data is returned  by getAllDataPointsForTimeSpan()
	 * it can get stuck asking for old points so limit the age of the oldest points 
	 * it will ask for.
	 * 
	 * @param properties 
	 * @param config Isn't used in this function.
	 * @throws InvalidPluginPropertyException
	 */
	@Override
	public boolean configure(SourceConnectionConfig config, Properties properties)
	throws InvalidPluginPropertyException 
	{
		m_Properties = properties;
		
		m_Username = config.getUsername();
		m_Host = config.getHost();
		
		String type = getCompulsoryProperty(properties, "dataCollectionType").toUpperCase();
		try
		{
			m_DataSourceType =  DataSourceCategory.getValue(type);
			switch (m_DataSourceType)
			{
			case TIME_SERIES:
					setDataSourceType(new DataSourceType("vCenter", m_DataSourceType));
					break;
			case NOTIFICATION:
					setDataSourceType(new DataSourceType("vCenter Events", m_DataSourceType));
					break;					
			default:
				throw new InvalidPluginPropertyException("Invalid property 'dataCollectionType'. " +
				"Should be either 'time_series' or 'notification'."); 
			}
		}
		catch (IllegalArgumentException e)
		{
			throw new InvalidPluginPropertyException("Invalid dataCollectionType = " + type);
		}
		
		String urlString = getCompulsoryProperty(m_Properties, "url");
		try
		{
			m_WebServiceUrl = new URL(urlString);
		}
		catch (MalformedURLException me)
		{
			throw new InvalidPluginPropertyException("Invalid Url for vSphere web services." +
					" Url = " + urlString);
		}
		
		m_IgnoreCert = Boolean.parseBoolean(properties.getProperty("ignoreCert", "true"));	
		
		String vmRegex = properties.getProperty("vmRegex", ".*");
		String hostRegex = properties.getProperty("hostRegex", ".*");
		String dataCenterRegex = properties.getProperty("dataCenterRegex", ".*");
		
		//if (!vmRegex.equals(".*") && !dataCenterRegex.equals(".*"))  // TODO - why this here.
		{
			// A regex has been defined so set a filter.
			m_ResourceSelectionFilters.add(new VSphereResourceSelection(dataCenterRegex, hostRegex, vmRegex));
		}
		
		final Long DEFAULT_OLDEST_QUERY_TIME = new Long(600);
		String oldestQueryTimeSecsStr = properties.getProperty("oldestQueryTimeSecs", DEFAULT_OLDEST_QUERY_TIME.toString());
		try
		{
			m_OldestQueryTimeMs = Long.parseLong(oldestQueryTimeSecsStr) * 1000;
		}
		catch (NumberFormatException e)
		{
			m_OldestQueryTimeMs = DEFAULT_OLDEST_QUERY_TIME * 1000;
			s_Logger.error("Could not parse 'oldestQueryTimeSecs=" + m_OldestQueryTimeMs
								+ "'. Using default value = " + DEFAULT_OLDEST_QUERY_TIME);
		}
		
		
		//  Get users password either from the properties file of the credential store.
		String password = config.getPassword();

		// Password not in the properties file so read it from the credential store.
		if (password == null)
		{
			try
			{
				CredentialStore credentialStore = CredentialStoreFactory.getCredentialStore();    
				char[] pass = credentialStore.getPassword(m_Host, m_Username);

				if (pass == null)
				{
					throw new IOException();
				}

				password = new String(pass); 
			}
			catch (IOException e)
			{
				throw new InvalidPluginPropertyException(
						"Cannot read password from the Credential Store for" +
						"host=" + m_Host + ", username=" + m_Username);
			}
		}
		
		
		try 
		{
			connect(password);
		} 
		catch (IOException e) 
		{
			s_Logger.error(e);
			throw new InvalidPluginPropertyException(
							"Could not connect to the VSphere web. " +
							"IOException thrown with message = " + e.toString());
		} 
		catch (ServiceException e) 
		{
			s_Logger.error(e);
			throw new InvalidPluginPropertyException(
							"Could not connect to the VSphere web. " +
							"IOException thrown with message = " + e.toString());
		}
		
		return true;
	}
	
	
	/**
	 * Connect to vCenter using the url and username from the properties file 
	 * and the given password. 
	 * 
	 * @param password
	 * @return
	 * @throws IOException
	 * @throws ServiceException
	 */
	public boolean connect(String password) throws IOException, ServiceException
	{
		// Create connections and data collection objects
		VSphereConnection connection;
		
		switch(m_DataSourceType)
		{
		case NOTIFICATION:
			connection = new VSphereConnection();
			connection.connect(m_WebServiceUrl, m_Username, password, m_IgnoreCert);
			m_EventCollector = new VSphereEventCollector(connection);
			m_EventCollector.start(); 
			
			connection = new VSphereConnection();
			connection.connect(m_WebServiceUrl, m_Username, password, m_IgnoreCert);
			m_TaskCollector = new VSphereTaskCollector(connection);
			m_TaskCollector.start();
			
			break;
		
		case TIME_SERIES:
			connection = new VSphereConnection();
			connection.connect(m_WebServiceUrl, m_Username, password, m_IgnoreCert);
			m_PerformanceData = new VSpherePerformanceData(connection, m_ResourceSelectionFilters);
			
			break;			
		}
		
		
		return true;
	}
	
	/**
	 * WARNING: This function has not been tested.
	 */
	@Override
	public ConnectionStatus testConnection(SourceConnectionConfig config, Properties properties)
	{
		String urlString =  properties.getProperty("url");
		if (urlString == null)
		{
			return new ConnectionStatus(ConnectionStatus.Status.CONNECTION_FAILED);
		}

		VSphereConnection connection = new VSphereConnection();
		
		boolean ignoreCert = Boolean.parseBoolean(properties.getProperty("ignorecert", "true"));	
		
		try 
		{
			URL url = new URL(urlString);
			if (connection.connect(url, config.getUsername(), config.getPassword(), ignoreCert))
			{
				return new ConnectionStatus(ConnectionStatus.Status.CONNECTION_OK);
			}
			else
			{
				return new ConnectionStatus(ConnectionStatus.Status.CONNECTION_FAILED);
			}
		} 
		catch (RuntimeFault e)
		{
			return new ConnectionStatus(ConnectionStatus.Status.CONNECTION_FAILED);
		} 
		catch (RemoteException e) 
		{
			return new ConnectionStatus(ConnectionStatus.Status.CONNECTION_FAILED);
		}
		catch (ServiceException e) 
		{
			return new ConnectionStatus(ConnectionStatus.Status.CONNECTION_FAILED);
		} 
		catch (MalformedURLException e)
		{
			return new ConnectionStatus(ConnectionStatus.Status.CONNECTION_FAILED);
		}
	}
	
	/**
	 * Get a property from a properties object, throwing an exception if it's
	 * not present.
	 * @param props The properties to search.
	 * @param propName The name of the property to search for.
	 * @return The property value.
	 */
	private String getCompulsoryProperty(Properties props, String propName)
	throws InvalidPluginPropertyException
	{
		String propValue = props.getProperty(propName);
		if (propValue == null)
		{
			throw new InvalidPluginPropertyException("'" + propName +
								"' property not specified for vSphere plugin");
		}

		return propValue;
	}
	
	
	/**
	 * Returns a list of notifications that have occurred since this
	 * function was last called.
	 * 
	 * This plugin does not handle retrieving notifications by date
	 * instead it returns all notifications that have occurred since the
	 * last time it was queried.
	 * 
	 * VSphere Events and Tasks are converted to Notifications.
	 * 
	 * 
	 * @param start Will be ignored 
	 * @param end Will be ignored
	 * @return A list of all the Events and Task that occurred since this 
	 * 		   function was last called.
	 */
	@Override
	public List<Notification> getNotifications(Date start, Date end)
	{
		s_Logger.info("getNotifications(" + start + ", " + end);
		
		List<Notification> results = new ArrayList<Notification>();
				
		results.addAll(m_TaskCollector.getNotificationsAndClearHistory());

		results.addAll(m_EventCollector.getNotificationsAndClearHistory());
		
		return results;
	}
	
	
	/**
	 * Queries all the Virtual Machines in VCenter's virtual data centre for 
	 * performance data and returns it as time series data.
	 * 
	 * This function attempts to handle clock skew between computers by
	 * sliding the query time window backwards if no data is returned.
	 * 
	 * The minTime parameter isn't used, instead the plugin tracks the
	 * timestamp of the last returned point and uses that as the start
	 * time for the next query.
	 * 
	 * @param minTime This parameter isn't used.
	 * @param maxTime End time for the query.
	 * @param intervalSecs This parameter isn't used.
	 * @return  
	 */
	@Override
	public Collection<TimeSeriesData> getAllDataPointsForTimeSpan(Date minTime,
													Date maxTime, int intervalSecs) 
	{
		s_Logger.info("getAllDataPointsForTimeSpan(" + minTime + ", " +
				maxTime + ", " + intervalSecs);

		// If not the first time use the timestamp of the last result
		// as the start time.
		if (m_LastQueryEndTime == null)
		{
			m_LastQueryEndTime = new Date(minTime.getTime());
		}

		// If the last time we got query data was a long time ago then
		// use minTime as the new start point and throw skip some data.
		if ((minTime.getTime() - m_LastQueryEndTime.getTime()) > m_OldestQueryTimeMs)
		{
			m_LastQueryEndTime = minTime;
		}

		Date startDate = m_LastQueryEndTime;
		Date endDate = new Date(maxTime.getTime());


		// Handle clock skew by gradually going backwards in time 
		// if no data is returned.
		List<PerformanceData> perfData = null;
		boolean gotPerfData = false;
		while (!gotPerfData)
		{
			perfData = m_PerformanceData.getPerformanceData(startDate, endDate);
			gotPerfData = perfData.size() > 0;

			if (!gotPerfData)
			{
				long period = endDate.getTime() - startDate.getTime();
				startDate = new Date(startDate.getTime() - period);
				endDate = new Date(endDate.getTime() - period);

				s_Logger.info("getAllDataPointsForTimeSpan(): No data returned. Changing " +
						"query time period to start=" + startDate + ", end=" + endDate);
			}


			if ((minTime.getTime() - startDate.getTime()) > m_OldestQueryTimeMs)
			{
				s_Logger.error("getAllDataPointsForTimeSpan(). No Performance data returned" +
				" for the oldest time period.");
				break;
			}
		}

		// Set the time of the last data point.
		if (perfData.size() > 0 && 
				perfData.get(0).getPerfEntityMetric().getSampleInfo().length > 0)
		{
			PerfSampleInfo [] sampleInfo = perfData.get(0).getPerfEntityMetric().getSampleInfo();
			long lastTimeStamp = sampleInfo[sampleInfo.length -1].getTimestamp().getTime().getTime();
			m_LastQueryEndTime = new Date(lastTimeStamp + 1);
		}


		List<TimeSeriesData> timeSeriesData = new ArrayList<TimeSeriesData>();
		for (PerformanceData data : perfData)
		{
			List<TimeSeriesData> tsData = VSphereDataUtils.performanceDataToTimeSeries(data,
					m_PerformanceData.getEntityIdToHostnameMap());

			timeSeriesData.addAll(tsData);
		}

		return timeSeriesData;
	}
	
	/**
	 * Default VSphere sampling interval is 20s.
	 */
	@Override
	public int getUsualPointIntervalSecs() 
	{
		return 20;
	}
	
	/**
	 * Default implementation does nothing.
	 */
	@Override
	public void setUsualPointIntervalSecs(int value) 
	{
	}


	/**
	 * Stops the Event and Task collector threads
	 */
	public void stop()
	{
		if (m_EventCollector != null)
		{
			m_EventCollector.quit();
		}
		
		if (m_TaskCollector != null)
		{
			m_TaskCollector.quit();
		}
	}
	

	@Override
	public Plugin duplicate() 
	{
		VSpherePlugin clone = new VSpherePlugin();
		clone.setName(getName());
		
		try
		{
			clone.configure(null, m_Properties);
		}
		catch (InvalidPluginPropertyException e)
		{
			s_Logger.error("Error duplicating vSphere plugin. " +
					"Properties could not be set.");
			s_Logger.error(e);
		}
		
		return clone;
	}

}
