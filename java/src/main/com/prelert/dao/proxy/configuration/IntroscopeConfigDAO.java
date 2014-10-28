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
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.prelert.dao.proxy.ProxyDataAccessException;
import com.prelert.dao.proxy.RemoteProxyDAO;
import com.prelert.data.AnalysisDuration;
import com.prelert.data.ConnectionStatus;
import com.prelert.proxy.dao.RemoteObjectFactoryDAO;
import com.prelert.proxy.dao.configuration.RemoteConfigurationDAO;
import com.prelert.proxy.dao.configuration.introscope.RemoteIntroscopeConfigDAO;
import com.prelert.proxy.data.CavAvailableDateRange;
import com.prelert.proxy.data.SourceConnectionConfig;


/**
 * Configure the Introscope plugins through RMI calls.
 * 
 * Class provides access to the methods of the RemoteIntroscopeConfigDAO
 * interface. 
 * @see RemoteIntroscopeConfigDAO
 */
public class IntroscopeConfigDAO extends RemoteProxyDAO
{
	static Logger s_Logger = Logger.getLogger(IntroscopeConfigDAO.class);

	private RemoteIntroscopeConfigDAO m_RemoteDAO;

	
	public IntroscopeConfigDAO()
	{
		m_RemoteDAO = null;
	}

	
	/**
	 * Test the connection paramaters. Returns CONNECTION_OK if
	 * successful. If a connection can be made but the Enterprise Manager's health
	 * metrics cannot be read then MISSING_HEALTH_METRICS is returned.
	 * 
	 * @param config
	 * @return CONNECTION_OK if connected successfully.
	 */
	public ConnectionStatus testConnection(SourceConnectionConfig config)
	{
		s_Logger.debug("testConnection RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().testConnection(config);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteIntroscopeConfigDAO";
					s_Logger.error("testConnection(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error testing the connection through RemoteIntroscopeConfigDAO";
				s_Logger.error("testConnection(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}
	
	
	/**
	 * Returns the start and end dates of the valid data collection period.
	 * 
	 * @return
	 */
	public CavAvailableDateRange getCavDateRange()
	{
		s_Logger.debug("getCavDateRange() RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getValidDateRange();
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteIntroscopeConfigDAO";
					s_Logger.error("getCavDateRange(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting CAV available date range RemoteIntroscopeConfigDAO";
				s_Logger.error("getCavDateRange(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}
	
	
	/**
	 * Returns the list of all the agents available in Introscope.
	 * The optional agentRegex parameter specifies a regex to filter 
	 * the agents by. If <code>null</code> or empty all agents 
	 * <em>excluding</em> the Custom Metric Agents are returned.
	 * To get everything pass ".*".
	 * 
	 * @param agentRegex - if <code>null</code> or empty this is ignored.
	 * @param connectionParams - EM connection details.
	 * @return
	 */
	public List<String> listAgents(String agentRegex, SourceConnectionConfig connectionParams)
	{
		s_Logger.debug("getAgents RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().listAgentsOnEM(agentRegex, connectionParams);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteIntroscopeConfigDAO";
					s_Logger.error("getAgents(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting agents through the RemoteIntroscopeConfigDAO";
				s_Logger.error("getAgents(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}
	
	
	/**
	 * Return the list of agents that have previously been chosen
	 * to be monitored by the EM. If the connectionParams don't match
	 * the current connection params then an empty list is returned.
	 * 
	 * @param connectionParams - if this matches the current connection
	 * 							 parameters the saved list of selected
	 * 							 agents is returned.
	 * @return the list of agents being monitored.
	 */
	public List<String> getAgents(SourceConnectionConfig connectionParams)
	{
		s_Logger.debug("getAgents RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getAgents(connectionParams);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteIntroscopeConfigDAO";
					s_Logger.error("getAgents(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting agents through the RemoteIntroscopeConfigDAO";
				s_Logger.error("getAgents(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}
	
	
	/**
	 * Returns the estimated time in ms it will take to collect all the 
	 * data for the CAV.
	 * 
	 * -1 is returned if a proper estimate could not be made because no
	 * data was returned by the queries.
	 * 
	 * @param agents
	 * @param timeOfIncident
	 * @param connectionConfig
	 * @return The estimated duration in milliseconds or -1 if there was an error.
	 */
	public AnalysisDuration estimateCavDuration(List<String> agents, Date timeOfIncident,
			SourceConnectionConfig connectionConfig)
	{
		s_Logger.debug("estimateCavDuration RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().estimateCompletionTime(agents, timeOfIncident, connectionConfig);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteIntroscopeConfigDAO";
					s_Logger.error("estimateCavDuration(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error estimating CAV duration through the RemoteIntroscopeConfigDAO";
				s_Logger.error("estimateCavDuration(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}
	
	
	/**
	 * Resets the list of agents, metric queries and connection 
	 * settings used by the Introscope plugins.
	 * @return
	 */
	public boolean resetConfiguration()
	{
		s_Logger.debug("resetConfiguration RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().resetConfiguration();
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteIntroscopeConfigDAO";
					s_Logger.error("resetConfiguration(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error resetting the Introscope Configuration through the RemoteIntroscopeConfigDAO";
				s_Logger.error("resetConfiguration(): " + errMsg, e);
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
	private RemoteIntroscopeConfigDAO getRemoteDAO() throws AccessException, RemoteException, NotBoundException
	{
		if (m_RemoteDAO != null)
		{
			return m_RemoteDAO;
		}

		RemoteObjectFactoryDAO factory = getRemoteFactory();
		RemoteConfigurationDAO configDAO = factory.getConfigurationDAO(getOriginatorName());
		
		m_RemoteDAO = (RemoteIntroscopeConfigDAO)configDAO.getConfigServer(
									"com.prelert.proxy.plugin.introscope.IntroscopePlugin");

		return m_RemoteDAO;
	}


	/**
	 * Sets the m_RemoteDAO member to null. 
	 * This will force a new connection to any calls to <code>getRemoteDAO</code>
	 * to try to make a new connection.
	 */
	private void resetRemoteDAO()
	{
		m_RemoteDAO = null;
	}
}
