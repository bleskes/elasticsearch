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


package com.prelert.proxy.plugin.itrs;

import static com.itrsgroup.activemodel.core.ActiveModelRepositoryHolder.getRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.itrsgroup.activemodel.client.ActiveModelClientException;
import com.itrsgroup.activemodel.client.ActiveModelClientUtil;
import com.itrsgroup.activemodel.client.core.UserDetailsImpl;
import com.itrsgroup.activemodel.dataview.ActiveDataView;
import com.itrsgroup.activemodel.dataview.ActiveDataViewEvent;
import com.itrsgroup.activemodel.dataview.ActiveDataViewListener;
import com.itrsgroup.activemodel.dataview.ActiveDataViewModel;
import com.itrsgroup.activemodel.list.ActiveList;
import com.itrsgroup.activemodel.list.ActiveListModel;

import com.prelert.data.ConnectionStatus;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;
import com.prelert.data.ConnectionStatus.Status;
import com.prelert.data.TimeSeriesData;
import com.prelert.proxy.data.SourceConnectionConfig;
import com.prelert.proxy.inputmanager.querymonitor.QueryTookTooLongException;
import com.prelert.proxy.plugin.ErrorGettingDataPointsException;
import com.prelert.proxy.plugin.InternalPlugin;
import com.prelert.proxy.plugin.Plugin;
import com.prelert.proxy.plugin.PluginProperty;

/**
 * The ITRS Geneos Web Services plugin.
 * 
 * Geneos does not store data points so they cannot be queried, instead 
 * the web service pushes out points which are then cached by the plugin.
 * 
 * The name of the file containing the web service paths in one of the 
 * config parameters, the plugin registers its data point cache to receive
 * the data. 
 */


// TODO
// There is a bug in the new API where the sample interval parameter is
// always Null if the path to the dataview contains '"' instead of '\''.
// e.g. 
// /geneos/gateway[(@name='Demo Gateway')]/directory/...  is ok
// /geneos/gateway[(@name="Demo Gateway")]/directory/... will not return 
// a sample interval because Demo Gateway is in double quotes.
// Unfortunately when you call list paths for dataviews the API returns
// paths with the @name parameters in double quotes which have to be 
// replaced with single quotes before a sample interval is returned.
//
// This requires more testing and ITRS haven't been notified of the 
// bug yet. 


public class ItrsPlugin extends Plugin implements InternalPlugin 
{
	/*
	 * Static members
	 */
	private static Logger s_Logger = Logger.getLogger(ItrsPlugin.class);
	
	// Required plugin properties.
	static private final List<PluginProperty> s_RequiredProperties = new ArrayList<PluginProperty>();
	static 
	{
		s_RequiredProperties.add(new PluginProperty("ConfigFile", false));
	}
	
	public static final int WEBSERVICE_PORT = 8001;
	public static final String ITRS_DATATYPE = "ITRS";
	
	/*
	 * Non static members
	 */
	private List<String> m_ServicePaths;
	private List<ItrsPointCache> m_PointCaches;
	private List<ActiveDataView> m_DataViews;	
	
	private SourceConnectionConfig m_ConnectionConfig;
	
	public ItrsPlugin()
	{
		m_ServicePaths = new ArrayList<String>();

		m_PointCaches = new ArrayList<ItrsPointCache>();
		m_DataViews = new ArrayList<ActiveDataView>();
		
		setDataSourceType(new DataSourceType(ITRS_DATATYPE, DataSourceCategory.TIME_SERIES));
	}
	
	
	@Override
	public Plugin duplicate() 
	{
		ItrsPlugin clone = new ItrsPlugin();
		clone.setName(getName());
		
		try
		{
			clone.configure(m_ConnectionConfig, m_Properties);
		}
		catch (InvalidPluginPropertyException e)
		{
			s_Logger.error("Error duplicating ItrsPlugin", e);
		}

		return clone;
	}
	
	
	/**
	 * Loads the properties and creates the connection object.
	 * 
	 * </br></br>
	 * The following is the set of valid properties:
	 * <dl>
	 * <dt>ConfigFile</dt><dd>The name of the xml file. File should contain a list of 
	 * 		pointFilters elements inside a filters tag. e.g. <br/>
	 * 		<filters> 
	 * 			<pointFilter>/geneos/gateway/directory/probe/etc</pointFilter>
	 *          ...
	 *      </filters>
	 *      </br>
	 * If ConfigFile is not set then all the dataview paths are listed and used 
	 * in for the data queries. 
	 * </dd>
	 * </dl>    
	 */
	@Override
	public boolean configure(SourceConnectionConfig config, Properties properties)
	throws InvalidPluginPropertyException 
	{
		setProperties(properties);
		m_ConnectionConfig = config;
		
		int port = (config.getPort() != null) ? config.getPort() : WEBSERVICE_PORT;
		String host = config.getHost(); 
		if (host == null)
		{
			s_Logger.warn("No Host specified defaulting to 127.0.0.1");
			config.setHost("127.0.0.1");			
		}
		
		if (config.getUsername() == null || config.getUsername().isEmpty())
		{
			s_Logger.warn("No username is set in the connection config");
		}
		
		String role = properties.getProperty("role");
		if (role == null)
		{
			s_Logger.warn("No Role specified for the Geneos user. Defaulting to 'ROLE_USER'");
			role = "ROLE_USER";
		}
		
		try
		{
			ActiveModelClientUtil.init(config.getHost(), port, 
									new UserDetailsImpl(config.getUsername(), role), 
									config.getPassword());
		}
		catch (ActiveModelClientException e)
		{
			String msg = String.format("Error connecting to host=%s, port=%d", host, port);
			s_Logger.error(msg, e);
			
			throw new InvalidPluginPropertyException(msg);
		}
		
		
		boolean isConfigured = false;
		for (PluginProperty pluginProp : s_RequiredProperties)
		{
			String propValue = properties.getProperty(pluginProp.getKey());
			if (propValue == null && pluginProp.isRequired())
			{
				throw new InvalidPluginPropertyException("Missing property '" + 
						pluginProp.getKey() + "' must be set.");
			}
			
			
//			if ("ConfigFile".equals(pluginProp.getKey()) && propValue != null)
//			{
//				try
//				{
//					setPaths(ItrsPathLoader.loadPaths(propValue));
//					isConfigured = true;
//				}
//				catch (FileNotFoundException e)
//				{
//					String msg = "Cannot find ITRS config file '" + propValue + "'.";
//					s_Logger.error(msg);
//					throw new InvalidPluginPropertyException(msg);
//				}
//			}
		}
		
		// No config file was specified in the plugin properties 
		// so list all the dataviews and use those.
		if (isConfigured == false)
		{
			setPaths(listDataViewPaths());
		}

		return true;
	}	
		

	@Override
	public ConnectionStatus testConnection(SourceConnectionConfig config, Properties properties) 
	{
		try
		{
			int port = (config.getPort() != null) ? config.getPort() : WEBSERVICE_PORT;
			if (config.getHost() == null)
			{
				s_Logger.warn("No Host specified defaulting to 127.0.0.1");
				config.setHost("127.0.0.1");			
			}
			
			if (config.getUsername() == null || config.getUsername().isEmpty())
			{
				s_Logger.warn("testConnection(): No username is set in the connection config");
			}
			
			String role = properties.getProperty("role");
			if (role == null)
			{
				s_Logger.warn("testConnection(): No Role specified for the Geneos user. Defaulting to 'ROLE_USER'");
				role = "ROLE_USER";
			}
			
			ActiveModelClientUtil.init(config.getHost(), port,
					new UserDetailsImpl(config.getUsername(), role), 
					config.getPassword());
					
			return new ConnectionStatus(Status.CONNECTION_OK);
		}
		catch (ActiveModelClientException e)
		{
			s_Logger.error(e);
			
			ConnectionStatus cs = new ConnectionStatus(Status.CONNECTION_FAILED);
			cs.setErrorMessage(e.getMessage());
			return cs;
		}
		finally 
		{
			// TODO disconnect
		}
	}
	
	
	/**
	 * Returns the paths of all the active dataview objects.
	 * 
	 */
	private List<String> listDataViewPaths()
	{
		ItrsItemList itemList = new ItrsItemList();
		
		ActiveListModel activeListModel = getRepository().getActiveListModel();
		ActiveList activeList = activeListModel.create();
		activeList.setPath("//dataview");

		try
		{
			itemList.listItems(activeList);
			activeListModel.register(activeList);
			itemList.waitForResults();
		}
		finally
		{
			activeListModel.unregister(activeList);
			activeList.removeListener(itemList);
		}
		
		return itemList.getItemPaths();
	}
	
	
	/**
	 * Set the list of web service paths that will be queried for data.
	 * 
	 * @param paths
	 */
	private void setPaths(List<String> paths)
	{
		m_ServicePaths = paths;
	}
	
	
	/**
	 * Setup the models and paths.
	 */
	@Override
	public void start()
	{
		
		ActiveDataViewModel activeDataViewModel = getRepository().getActiveDataViewModel();
		
		// Create an active model for each path
		for (String path : m_ServicePaths)
		{
			//path = path.replace('"', '\'');
			
			ActiveDataView activeDataView = activeDataViewModel.create();
			activeDataView.setPath(path);
			
			ItrsPointCache cache = new ItrsPointCache();
			//activeDataView.addListener(cache);	
			activeDataView.addListener(new ActiveDataViewListener()
			{
				@Override
				public void onActiveDataViewEvent(final ActiveDataViewEvent paramEvent)
				{
					//System.out.println(new Date().getSeconds());
					System.out.println("Sample interval= " + paramEvent.getDataView().getSampleInterval());
				}
			});
			activeDataViewModel.register(activeDataView);
			
			
			m_PointCaches.add(cache);			
			m_DataViews.add(activeDataView);

		}
		
		/*
		ActiveDataViewModel activeDataViewModel = getRepository().getActiveDataViewModel();
		ActiveDataView activeDataView = activeDataViewModel.create();
		
		String path = "/geneos/gateway[(@name='Demo Gateway')]/directory/"
			+ "probe[(@name='Basic Probe')]/managedEntity[(@name='Basic Entity')]/"
			+ "sampler[(@name='cpu')][(@type='Basic')]/dataview[(@name='cpu')]";	
		
		activeDataView.setPath(m_ServicePaths.get(0));
		//activeDataView.setPath(path);
		
		activeDataView.addListener(new ActiveDataViewListener()
		{
			@Override
			public void onActiveDataViewEvent(final ActiveDataViewEvent paramEvent)
			{
				System.out.println("Sample interval= " + paramEvent.getDataView().getSampleInterval());
			}
		});
		
		

		activeDataViewModel.register(activeDataView);
		*/		
	}
	
	
	/**
	 * Unregisters the view from the data model. 
	 */
	@Override
	public void stop()
	{
		// Unregister the data views.
		ActiveDataViewModel activeDataViewModel = getRepository().getActiveDataViewModel();
		
		for (ActiveDataView view : m_DataViews)
		{
			activeDataViewModel.unregister(view);
		}		
	}
		
	
	/**
	 * @param intervalSecs - unused 
	 */
	@Override
	public Collection<TimeSeriesData> getAllDataPointsForTimeSpan(Date minTime,
													Date maxTime, int intervalSecs) 
    throws QueryTookTooLongException, ErrorGettingDataPointsException 
    {
		List<TimeSeriesData> result = new ArrayList<TimeSeriesData>();
		for (ItrsPointCache cache: m_PointCaches)
		{
			result.addAll(cache.getPoints(minTime, maxTime));
		}
		
		return result;
	}
	
	
	/**
	 * The different data types in ITRS return data a different
	 * frequencies so there is no 'usual' interval.
	 */
	@Override
	public int getUsualPointIntervalSecs() 
	{	
		return 0;
	}

	@Override
	public void setUsualPointIntervalSecs(int value) 
	{
	}

}
