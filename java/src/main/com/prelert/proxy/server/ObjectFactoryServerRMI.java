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

package com.prelert.proxy.server;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.ApplicationContext;

import com.prelert.proxy.dao.RemoteCausalityDAO;
import com.prelert.proxy.dao.RemoteDataSourceDAO;
import com.prelert.proxy.dao.RemoteEvidenceDAO;
import com.prelert.proxy.dao.RemoteIncidentDAO;
import com.prelert.proxy.dao.RemoteObjectFactoryDAO;
import com.prelert.proxy.dao.RemoteTimeSeriesDAO;
import com.prelert.proxy.dao.RemoteUserDAO;
import com.prelert.proxy.dao.configuration.RemoteConfigurationDAO;
import com.prelert.proxy.pluginLocator.PluginLocator;
import com.prelert.proxy.server.configuration.ConfigurationServerRMI;


public class ObjectFactoryServerRMI extends UnicastRemoteObject implements RemoteObjectFactoryDAO
{
	
	private static final long serialVersionUID = 7074838654617435828L;

	private Map<String, CausalityServerRMI> m_CausalityServersByOriginatorName;
	private Map<String, DataSourceServerRMI> m_DataSourceServersByOriginatorName;
	private Map<String, EvidenceServerRMI> m_EvidenceServersByOriginatorName;
	private Map<String, IncidentServerRMI> m_IncidentServersByOriginatorName;
	private Map<String, TimeSeriesServerRMI> m_TimeSeriesServersByOriginatorName;
	private Map<String, UserServerRMI> m_UserServersByOriginatorName;
	private Map<String, ConfigurationServerRMI> m_ConfigurationServersByOriginatorName;

	/**
	 * Used to create the individual plugin beans as required.
	 */
	private ApplicationContext m_ApplicationContext;


	private String m_ServerName;


	public ObjectFactoryServerRMI() throws RemoteException
	{
		setServerName("com.prelert.RemoteObjectFactory");
		
		m_CausalityServersByOriginatorName = new HashMap<String, CausalityServerRMI>();
		m_DataSourceServersByOriginatorName = new HashMap<String, DataSourceServerRMI>();
		m_EvidenceServersByOriginatorName = new HashMap<String, EvidenceServerRMI>();
		m_IncidentServersByOriginatorName = new HashMap<String, IncidentServerRMI>();
		m_TimeSeriesServersByOriginatorName = new HashMap<String, TimeSeriesServerRMI>();
		m_UserServersByOriginatorName = new HashMap<String, UserServerRMI>();
		m_ConfigurationServersByOriginatorName = new HashMap<String, ConfigurationServerRMI>();
	}


	@Override
	public RemoteCausalityDAO getCausalityDAO(String originator) throws RemoteException
	{
		CausalityServerRMI server = m_CausalityServersByOriginatorName.get(originator);
		if (server == null)
		{
			server = m_ApplicationContext.getBean("causalityServer" + originator, CausalityServerRMI.class);
			m_CausalityServersByOriginatorName.put(originator, server);
		}

		return server;
	}


	@Override
	public RemoteDataSourceDAO getDataSourceDAO(String originator)
	{
		DataSourceServerRMI server = m_DataSourceServersByOriginatorName.get(originator);
		if (server == null)
		{
			server = m_ApplicationContext.getBean("dataSourceServer" + originator, DataSourceServerRMI.class);
			m_DataSourceServersByOriginatorName.put(originator, server);
		}

		return server;
	}


	@Override
	public RemoteEvidenceDAO getEvidenceDAO(String originator)
	{
		EvidenceServerRMI server = m_EvidenceServersByOriginatorName.get(originator);
		if (server == null)
		{
			server = m_ApplicationContext.getBean("evidenceServer" + originator, EvidenceServerRMI.class);
			m_EvidenceServersByOriginatorName.put(originator, server);
		}

		return server;
	}


	@Override
	public RemoteIncidentDAO getIncidentDAO(String originator)
	{
		IncidentServerRMI server = m_IncidentServersByOriginatorName.get(originator);
		if (server == null)
		{
			server = m_ApplicationContext.getBean("incidentServer" + originator, IncidentServerRMI.class);
			// User information never comes from an external plugin, so we don't
			// need to set an application context for creating plugins
			m_IncidentServersByOriginatorName.put(originator, server);
		}

		return server;
	}


	@Override
	public RemoteTimeSeriesDAO getTimeSeriesDAO(String originator)
	{
		TimeSeriesServerRMI server = m_TimeSeriesServersByOriginatorName.get(originator);
		if (server == null)
		{
			server = m_ApplicationContext.getBean("timeSeriesServer" + originator, TimeSeriesServerRMI.class);
			m_TimeSeriesServersByOriginatorName.put(originator, server);
		}

		return server;
	}


    @Override
    public RemoteUserDAO getUserDAO(String originator) throws RemoteException
    {
		UserServerRMI server = m_UserServersByOriginatorName.get(originator);
    	if (server == null)
		{
			server = m_ApplicationContext.getBean("userServer" + originator, UserServerRMI.class);
			// User information never comes from an external plugin, so we don't
			// need to set an application context for creating plugins
			m_UserServersByOriginatorName.put(originator, server);
		}

		return server;
    }

	@Override
	public RemoteConfigurationDAO getConfigurationDAO(String originator)
	{
		ConfigurationServerRMI server = m_ConfigurationServersByOriginatorName.get(originator);
		if (server == null)
		{
			server = m_ApplicationContext.getBean("configurationServer" + originator, ConfigurationServerRMI.class);
			m_ConfigurationServersByOriginatorName.put(originator, server);
			
			server.setApplicationContext(m_ApplicationContext);
		}

		return server;
	}


	/**
	 * Set the application context used to create new plugins.
	 * @param appContext The application context used to create new plugins.
	 */
	public void setApplicationContext(ApplicationContext appContext)
	{
		m_ApplicationContext = appContext;
		
		Collection<ConfigurationServerRMI> configServers = m_ConfigurationServersByOriginatorName.values();
		for (ConfigurationServerRMI configServer : configServers)
		{
			configServer.setApplicationContext(appContext);
		}
	}


	/**
	 * Get the application context used to create new plugins.
	 * @return The application context used to create new plugins.
	 */
	public ApplicationContext getApplicationContext()
	{
		return m_ApplicationContext;
	}

	public String getServerName()
	{
		return m_ServerName;
	}


	public void setServerName(String serverName)
	{
		m_ServerName = serverName;
	}


	public PluginLocator getPluginLocator(String originator)
	{
		// It's fairly arbitrary which type of DAO we get here, as all the ones
		// that have a plugin locator share the same one.
		return ((TimeSeriesServerRMI)getTimeSeriesDAO(originator)).getPluginLocator();
	}	
}
