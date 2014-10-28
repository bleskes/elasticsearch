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

package com.prelert.dao.proxy.control;

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
import com.prelert.data.ProcessStatus;
import com.prelert.proxy.dao.RemoteControlDAO;
import com.prelert.proxy.data.DataTypeConfig;

/**
 * 
 */
public class ProxyControlDAO extends RemoteProxyDAO
{
	static Logger s_Logger = Logger.getLogger(ProxyControlDAO.class);
			
	private RemoteControlDAO m_RemoteControlDAO;
	
	public ProxyControlDAO()
	{
		m_RemoteControlDAO = null;
	}
	
	
	/**
	 * Starts the CAV by starting the proxy input managers.
	 * 
	 * @param timeOfIncident - Time of incident to be analysed.
	 * @param datatypes - List of datatype configs that will be used in the CAV.
	 * @return true if successful.
	 * @throws ProxyDataAccessException if an error occurs starting the CAV 
	 * 	remotely through the Proxy.
	 */
	public boolean startCav(Date timeOfIncident, List<DataTypeConfig> datatypes) 
	{
		s_Logger.debug("startCav() RMI call");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteControlDAO().startCav(timeOfIncident, datatypes);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteControlDAO";
					s_Logger.error("startCav: " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error starting the CAV: RemoteControlDAO";
				s_Logger.error("startCav(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}
	
	
	/**
	 * Pauses or resumes the CAV.
	 * @param pause - if true pause the CAV else resume it.
	 * @return true if successful.
	 * @throws ProxyDataAccessException if an error occurs pausing/resuming the CAV 
	 * 	remotely through the Proxy.
	 */
	public boolean pauseCav(boolean pause)
	{
		s_Logger.debug("pauseCav(" + pause + ") RMI call");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				if (pause)
				{
					return getRemoteControlDAO().pauseCav();
				}
				else
				{
					return getRemoteControlDAO().resumeCav();
				}
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteControlDAO";
					s_Logger.error("pauseCav: " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error pausing the CAV: RemoteControlDAO";
				s_Logger.error("pauseCav(" + pause + "): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}
	
	
	/**
	 * Stops the CAV running.
	 * This is different to pausing the CAV as it cannot be restarted afterwards. 
	 * To start another CAV call <code>resetCav()</code> first. 
	 * 
	 * @return true if successful
	 * @throws ProxyDataAccessException if an error occurs stopping the CAV 
	 * 	remotely through the Proxy.
	 */
	public boolean stopCav()
	{
		s_Logger.debug("stopCav() RMI call");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteControlDAO().stopCav();
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteControlDAO";
					s_Logger.error("stopCav: " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error stopping the CAV: RemoteControlDAO";
				s_Logger.error("stopCav(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}
	
	
	/**
	 * Returns true if the Proxy is running in CAV mode.
	 * @return <code>true</code> if a CAV is running, <code>false</code> otherwise.
	 * @throws ProxyDataAccessException if an error occurs obtaining the running
	 * 	state of the CAV remotely through the Proxy.
	 */
	public boolean isCavRunning()
	{
		s_Logger.debug("isCavRunning() RMI call");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteControlDAO().isCavRunning();
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteControlDAO";
					s_Logger.error("isCavRunning: " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting Proxy Status: RemoteControlDAO";
				s_Logger.error("isCavRunning(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}
	
	/**
	 * Returns the status of the Prelert processes.
	 * @return list of <code>ProcessStatus</code> indicating the status of the
	 * 	various Prelert processes.
	 * @throws ProxyDataAccessException if an error occurs obtaining the status
	 * 	of the processes remotely through the Proxy.
	 */
	public List<ProcessStatus> getProcessesStatus()
	{
		s_Logger.debug("getProcessesStatus() RMI call");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteControlDAO().getProcessesStatus();
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteControlDAO";
					s_Logger.error("getProcessesStatus: " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting Process Status: RemoteControlDAO";
				s_Logger.error("getProcessesStatus(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
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
	private void resetRemoteDAO()
	{
		m_RemoteControlDAO = null;
	}
}
