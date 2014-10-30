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

package com.prelert.dao.proxy.configuration;

import java.rmi.AccessException;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.prelert.dao.proxy.ProxyDataAccessException;
import com.prelert.dao.proxy.RemoteProxyDAO;
import com.prelert.data.AnalysisDuration;
import com.prelert.data.CavStatus;
import com.prelert.data.ConnectionStatus;
import com.prelert.proxy.dao.RemoteControlDAO;
import com.prelert.proxy.dao.RemoteObjectFactoryDAO;
import com.prelert.proxy.dao.configuration.RemoteConfigurationDAO;
import com.prelert.proxy.data.CavAvailableDateRange;
import com.prelert.proxy.data.DataTypeConfig;

/**
 * Class for configuring plugins over a RMI connection. 
 */
public class ConfigurationProxyDAO extends RemoteProxyDAO
{
	static Logger s_Logger = Logger.getLogger(ConfigurationProxyDAO.class);

	private RemoteConfigurationDAO m_RemoteConfigDAO;
	private RemoteControlDAO m_RemoteControlDAO;
	
	public ConfigurationProxyDAO()
	{
		m_RemoteConfigDAO = null;
	}
	
	
	/**
	 * Returns a list of all the configured data types names
	 * available in the Proxy.
	 * @return
	 */
	public List<String> getConfiguredDataTypeNames()
	{
		s_Logger.debug("getDataSourceNames RMI call");
		
		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteConfigDAO().getConfiguredDataTypeNames();
			}
			catch (ConnectException ce)
			{
				resetRemoteDAOs();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteConfigurationDAO";
					s_Logger.error("getDataSourceNames(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting data type names through RemoteConfigurationDAO";
				s_Logger.error("getDataSourceNames(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}
	
	
	/**
	 * Get the configuration for the specified data type.
	 * If the configuration for the specified type cannot be found
	 * then <code>null</code> is returned. 
	 * 
	 * @param datatype 
	 * @return The DataTypeConfig or <code>null</code>.
	 */
	public DataTypeConfig getConfiguredDataType(String datatype)
	{
		s_Logger.debug("getConfiguredDataType(" + datatype + ") RMI call");
		
		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteConfigDAO().getConfiguredDataType(datatype);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAOs();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteConfigurationDAO";
					s_Logger.error("getConfiguredDataType(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting data type '" + datatype + 
									"' through RemoteConfigurationDAO";
				s_Logger.error("getConfiguredDataType(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}
	
	
	/**
	 * Returns the configurations for all the data types.
	 * @return A List of <code>DataTypeConfig</code>s.
	 */
	public List<DataTypeConfig> getConfiguredDataTypes()
	{
		s_Logger.debug("getConfiguredDataTypes() RMI call");
		
		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteConfigDAO().getConfiguredDataTypes();
			}
			catch (ConnectException ce)
			{
				resetRemoteDAOs();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteConfigurationDAO";
					s_Logger.error("getConfiguredDataTypes(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting data types through RemoteConfigurationDAO";
				s_Logger.error("getConfiguredDataTypes(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}
	
	
	/**
	 * Add a configuration for a new Data type. 
	 * 
	 * @param datatypeConfig The new configuration
	 * @return True if successful
	 */
	public boolean addDataType(DataTypeConfig datatypeConfig)
	{
		s_Logger.debug("addDataType RMI call");
		
		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteConfigDAO().addConfiguredDataType(datatypeConfig);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAOs();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteConfigurationDAO";
					s_Logger.error("addDataType(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error adding data type config through RemoteConfigurationDAO";
				s_Logger.error("addDataType(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}
	
	
	/**
	 * Returns a list of all the templated data type names
	 * available in the Proxy.
	 * 
	 * @return
	 */
	public List<String> getTemplateDataTypeNames()
	{
		s_Logger.debug("getTemplateDataTypeNames RMI call");
		
		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteConfigDAO().getTemplateDataTypeNames();
			}
			catch (ConnectException ce)
			{
				resetRemoteDAOs();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteConfigurationDAO";
					s_Logger.error("getTemplateDataTypeNames(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting template data type names through RemoteConfigurationDAO";
				s_Logger.error("getTemplateDataTypeNames(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}
	
	
	/**
	 * Get the template configuration for the specified data type.
	 * If the configuration for the specified type cannot be found
	 * then <code>null</code> is returned. 
	 * 
	 * @param datatype
	 * @return The DataTypeConfig or <code>null</code>.
	 */
	public DataTypeConfig getTemplateDataType(String datatype)
	{
		s_Logger.debug("getTemplateDataType RMI call");
		
		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteConfigDAO().getTemplateDataType(datatype);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAOs();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteConfigurationDAO";
					s_Logger.error("getTemplateDataType(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting template data type '" + datatype +
							"' through RemoteConfigurationDAO";
				s_Logger.error("getTemplateDataType(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}
	
	
	/**
	 * Returns the configurations for all the template data types.
	 * 
	 * @return A List of <code>DataTypeConfig</code>s.
	 */
	public List<DataTypeConfig> getTemplateDataTypes()
	{
		s_Logger.debug("getTemplateDataTypes RMI call");
		
		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteConfigDAO().getTemplateDataTypes();
			}
			catch (ConnectException ce)
			{
				resetRemoteDAOs();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteConfigurationDAO";
					s_Logger.error("getTemplateDataTypes(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting template data types through RemoteConfigurationDAO";
				s_Logger.error("getTemplateDataTypes(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}
	
	
	/**
	 * Reset and reload the cached datatype configurations.
	 * 
	 * @return true if the datatypes are reloaded successfully.
	 */
	public boolean reloadDataTypes()
	{
		s_Logger.debug("reloadDataTypes RMI call");
		
		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteConfigDAO().reloadDataTypes();
			}
			catch (ConnectException ce)
			{
				resetRemoteDAOs();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteConfigurationDAO";
					s_Logger.error("reloadDataTypes(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error reloading data types through RemoteConfigurationDAO";
				s_Logger.error("reloadDataTypes(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}
		
	
	/**
	 * Test the plugin's connection to its data source
	 * for the given DataTypeConfig. The connection might be via JDBC 
	 * driver or the Introscope CLW or any other source.
	 * 
	 * The <code>SourceConnectionConfig</code> and any required plugin
	 * properties should be set in <code>config</code>. 
	 * 
	 * @param config
	 * @return A connection status object. 
	 */	
	public ConnectionStatus testConnection(DataTypeConfig config)
	{
		s_Logger.debug("testConnection RMI call: " + config);
		
		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteConfigDAO().testConnection(config);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAOs();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteConfigurationDAO";
					s_Logger.error("testConnection(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error testing connection through RemoteConfigurationDAO";
				s_Logger.error("testConnection(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}
	
	
	public AnalysisDuration estimateCompletionTime(DataTypeConfig config, Date timeOfIncident) 
	{
		s_Logger.debug("estimateCompletionTime RMI call: " + timeOfIncident + " " + config);
		
		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteConfigDAO().estimateCompletionTime(config, timeOfIncident);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAOs();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteConfigurationDAO";
					s_Logger.error("estimateCompletionTime(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting the estimated analysis duration" +
						" through RemoteConfigurationDAO";
				s_Logger.error("estimateCompletionTime(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}
	
	
	/**
	 * Returns the start and end dates of the valid data collection period.
	 * i.e. this the time frame from which data can be pulled for analysis.
	 *
	 * @return The date range for which data is available.
	 */
	public CavAvailableDateRange getValidDateRange() 
	{
		s_Logger.debug("getValidDateRange RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteConfigDAO().getValidDateRange();
			}
			catch (ConnectException ce)
			{
				resetRemoteDAOs();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteConfigDAO";
					s_Logger.error("getValidDateRange(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting the dates of available data: RemoteConfiglDAO";
				s_Logger.error("getValidDateRange(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}
	
	
	/**
	 * Returns the date/time that has been set for analysis in the CAV.
	 * @return CAV time of incident.
	 */
	public Date getCavTimeOfIncident()
	{
		s_Logger.debug("getCavTimeOfIncident RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteControlDAO().getCavTimeOfIncident();
			}
			catch (ConnectException ce)
			{
				resetRemoteDAOs();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteControlDAO";
					s_Logger.error("getCavTimeOfIncident(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting the CAV analysis dates: RemoteControlDAO";
				s_Logger.error("getCavTimeOfIncident(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}
	
	
	/**
	 * Returns the CAV status. 
	 * If no CAV is running a default constructed CavStatus object is returned.
	 * @return
	 */
	public CavStatus getCavStatus()
	{
		s_Logger.debug("getCavStatus() RMI call");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteControlDAO().getCavStatus();
			}
			catch (ConnectException ce)
			{
				resetRemoteDAOs();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteControlDAO";
					s_Logger.error("getCavStatus: " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting CAV Status: RemoteControlDAO";
				s_Logger.error("getCavStatus(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}
	
	/**
	 * Returns a valid remote object or throws and exception.
	 * 
	 * This uses RMI to connect to a <code>RemoteObjectFactoryDAO</code> and
	 * queries it for the remote data object.
	 * 
	 * @return A valid remote object.
	 * @throws AccessException
	 * @throws RemoteException
	 * @throws NotBoundException
	 */
	private RemoteConfigurationDAO getRemoteConfigDAO() throws AccessException, RemoteException, NotBoundException
	{
		if (m_RemoteConfigDAO != null)
		{
			return m_RemoteConfigDAO;
		}

		RemoteObjectFactoryDAO factory = getRemoteFactory();
		m_RemoteConfigDAO = factory.getConfigurationDAO(getOriginatorName());

		return m_RemoteConfigDAO;
	}
	
	
	/**
	 * Returns the Proxy Control RMI object.
	 * @return
	 * @throws RemoteException
	 * @throws NotBoundException
	 */
	private RemoteControlDAO getRemoteControlDAO() throws RemoteException, NotBoundException
	{
		if (m_RemoteControlDAO == null)
		{
			Registry registry = LocateRegistry.getRegistry(getRMIHostName(), getRMIPort());
			m_RemoteControlDAO = (RemoteControlDAO)registry.lookup("com.prelert.ControlServer");
		}
		
		return m_RemoteControlDAO;
	}

	
	/**
	 * Sets the m_RemoteDAO member to null. 
	 * This will force a new connection to any calls to <code>getRemoteDAO</code>
	 * to try to make a new connection.
	 */
	private void resetRemoteDAOs()
	{
		m_RemoteConfigDAO = null;
		m_RemoteControlDAO = null;
	}

}
