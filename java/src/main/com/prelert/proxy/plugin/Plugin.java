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

package com.prelert.proxy.plugin;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.prelert.data.ConnectionStatus;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;
import com.prelert.proxy.data.SourceConnectionConfig;
import com.prelert.proxy.inputmanager.querymonitor.QueryMonitorPolicy;
import com.prelert.proxy.inputmanager.querymonitor.RealTimeQueryMonitor;


/**
 * Abstract base class for <code>Plugins</code>. Implementing sub classes 
 * need to override the {@link #loadProperties()} and {@link #duplicate()}
 * methods.
 * 
 * Non-abstract sub classes of this class should also implement one of the 
 * following interfaces
 * <ul>
 * <li>{@link com.prelert.proxy.plugin.InternalPlugin}</li>
 * <li>{@link com.prelert.proxy.plugin.ExternalPointsPlugin}</li>
 * <li>{@link com.prelert.proxy.plugin.ExternalPlugin}</li>
 * <li>{@link com.prelert.proxy.plugin.NotficationPlugin}</li>
 * </ul>
 */
public abstract class Plugin 
{
	private String m_Name;
	private DataSourceType m_DataType;
	
	private QueryMonitorPolicy m_QueryMonitorPolicy;

	protected Properties m_Properties;
	
	private boolean m_IsConfigured;
	
	private Object m_ConfigSyncObj;
	
	public Plugin()
	{
		m_ConfigSyncObj = new Object();
		
		m_QueryMonitorPolicy = new RealTimeQueryMonitor();
	}


	public Plugin(DataSourceType datatype)
	{
		this();
		
		m_DataType = datatype;
	}
	
	
	/**
	 * Returns the DataSourceType (type name and category)
	 * handled by this plugin.
	 * @return DataSourceType or <code>null</code>
	 */
	public DataSourceType getDataSourceType()
	{
		return m_DataType;
	}

	/**
	 * Set the plugin's datatype.
	 * @param dataType
	 */
	public void setDataSourceType(DataSourceType dataType)
	{
		m_DataType = dataType;
	}

	
	/**
	 * Set the datatype. This sets the datatype field of the
	 * DataSourceType member.
	 * 
	 * If the DataSourceType has not been set previously and is 
	 * <code>null</code> then a new <code>DataSourceType</code> with
	 * this type name and category <code>DataSourceCategory.TIME_SERIES</code>
	 * is created. 
	 * 
	 * @param datatype
	 */
	public void setDataType(String datatype)
	{
		if (m_DataType == null)
		{
			m_DataType = new DataSourceType(datatype, DataSourceCategory.TIME_SERIES);
		}
		
		m_DataType.setName(datatype);
	}

	/**
	 * The name of the plugin as it appears in the database or bean id string.
	 * @return
	 */
	public String getName()
	{
		return m_Name;
	}


	public void setName(String name)
	{
		m_Name = name;
	}
	
	
	/**
	 * Returns a list of properties that should be defined 
	 * for the plugin.
	 * @return
	 */
	public List<PluginProperty> getRequriedProperties()
	{
		return Collections.emptyList();
	}
	
	
	/**
	 * Once the plugin is configured start any other processes the plugin
	 * relies on. Override this method for plugin specific implementations.
	 * @see stop()
	 */
	public void start()
	{
		
	}
	
	/**
	 * In the case that a plugin has no inputmanager and just exists to 
	 * serve the gui call this method to perform any required initialisation. 
	 * 
	 * Override this method for plugin specific implementations.
	 * 
	 * @param notableDate - A date such as the time the data was collected
	 * 						 that may be useful for the plugin when initialising.
	 * 						This value may be <code>null</code> individual 
	 * 						subclasses should handle that case.
	 */
	public void initialiseForGui(Date notableDate)
	{
		
	}


	/**
	 * On shutdown, each input manager tells its plugin to stop by calling this
	 * method.  If the plugin holds any resources that will stop it from being
	 * garbage collected then it should override this method to release them.
	 */
	public void stop()
	{
		// By default a plugin does nothing when told to stop
	}

	/**
	 * Performs any clean up to reset the state so the plugin
	 * can be started again after it has been stopped.
	 */
	public void reset()
	{
		
	}


	/**
	 * Get a map of arguments specific to the plugin to send to the usage data
	 * gathering website.  (Note that time, customer ID and message type are NOT
	 * plugin-specific, and will be overwritten even if they are put in the
	 * returned map.)
	 * @return A map of plugin-specific usage data.  Returning null is allowed,
	 *         and is equivalent to returning an empty map.
	 */
	public Map<String, String> getPluginSpecificUsageData()
	{
		return null;
	}	


	/**
	 * The query monitor policy for this plugin
	 * @return
	 */
	public QueryMonitorPolicy getQueryMonitorPolicy()
	{
		return m_QueryMonitorPolicy;
	}


	public void setQueryMonitorPolicy(QueryMonitorPolicy value)
	{
		m_QueryMonitorPolicy = value;
	}
	
	
	/**
	 * Exception class that represents an invalid property in the plugin's
	 * property file. 
	 */
	public class InvalidPluginPropertyException extends Exception
	{
		private static final long serialVersionUID = 3475105510237029556L;
		
		public InvalidPluginPropertyException(String message)
		{
			super(message);
		}
	}


	/**
	 * Sets the plugin's connection parameters and its properties. If the 
	 * properties are invalid a <code>InvalidPluginPropertyException</code> 
	 * exception is thrown. Subclasses should override this function and 
	 * use the <code>config</code> parameter to setup the connection
	 * to the datasource (database or other).
	 * 
	 * The passed properties object can be stored with a all to setProperties.
	 *  
	 * @param config
	 * @param properties
	 * @return True is sucessfully configured.
	 * @throws InvalidPluginPropertyException
	 */
	abstract public boolean configure(SourceConnectionConfig config, Properties properties)
	throws InvalidPluginPropertyException;
	

	/**
	 * Tests the connection to the plugin's data source. If the contents
	 * of <code>config</code> are not sufficient to make the connection 
	 * (e.g. the database type for a JDBC connection) then the plugin 
	 * specific <code>properties</code> should provide the remaining 
	 * details. 
	 * 
	 * If the connection test passes the returned ConnectionStatus object
	 * will have a status value of CONNECTION_OK.
	 * 
	 * @param config The connection config.
	 * @param properties Plugin specific properties required to make
	 * 	the connection if necessary. 
	 * @return the connection status.
	 */
	abstract public ConnectionStatus testConnection(SourceConnectionConfig config, Properties properties);

	
	/**
	 * The properties object stores all the plugins properties.
	 * @return
	 */
	protected Properties getProperties()
	{
		return m_Properties;
	}


	protected void setProperties(Properties properties)
	{
		m_Properties = properties;
	}

	
	/**
	 * Subclasses must implement this method and return an instance of the 
	 * subclass. Implementing classes should return a new instance of its type.
	 * 
	 * The cloned plugin should have the same name as source plugin so always
	 * add a call to <code>clone.setName(this.getName());</code>
	 * 
	 * @return A copy of this <code>Plugin</code>
	 */
	abstract public Plugin duplicate();
	
		
	/**
	 * This value is true when the plugin has been configured and the 
	 * connection has been tested and shown to be working properly.
	 * 
	 * @return
	 */
	public boolean isConfigured()
	{
		return m_IsConfigured;
	}
	
	
	/**
	 * Set the plugin configured status.
	 * If configured == true then this function notifies all 
	 * threads waiting on the ConfigSyncObj that the plugin
	 * has been configured.
	 * 
	 * @param configured
	 */
	protected void setConfigured(boolean configured)
	{
		synchronized(m_ConfigSyncObj)
		{
			m_IsConfigured = configured;
			
			if (m_IsConfigured)
			{
				m_ConfigSyncObj.notifyAll();
			}
		}
		
	}
	
	
	/**
	 * Returns the ConfigSyncObj. Threads can wait
	 * on this object for notification that the plugin is configured.
	 * @return
	 */
	public Object getWaitOnConfigObject()
	{
		return m_ConfigSyncObj;
	}
}
