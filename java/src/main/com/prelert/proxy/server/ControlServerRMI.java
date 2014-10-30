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
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.prelert.data.CavStatus;
import com.prelert.data.ProcessStatus;
import com.prelert.proxy.data.DataTypeConfig;
import com.prelert.proxy.Proxy;
import com.prelert.proxy.dao.RemoteControlDAO;
import com.prelert.proxy.plugin.Plugin.InvalidPluginPropertyException;


/**
 * This class provides methods for stopping the Proxy, 
 * controlling the input managers and getting the running
 * state of the inputmanagers. 
 */
public class ControlServerRMI extends UnicastRemoteObject implements RemoteControlDAO  
{
	private static final long serialVersionUID = 8946101944952933352L;
	
	private static final Logger s_Logger = Logger.getLogger(ControlServerRMI.class);

	private Proxy m_Proxy;
	private String m_ServerName;
	
	public ControlServerRMI() throws RemoteException
	{
		super();
		
		setServerName("com.prelert.ControlServer");
		
		m_Proxy = null;
	}
	
	public ControlServerRMI(Proxy proxy) throws RemoteException
	{
		this();
		
		m_Proxy = proxy;
	}
	
	/**
	 * Stops the Proxy gracefully.
	 * This call will not return until all proxy threads have quit.
	 */
	@Override
	public boolean shutdown()
	{
		s_Logger.info("Shutdown");
		return m_Proxy.shutdown();
	}
	
	/**
	 * Stops the Proxy.
	 * This call causes the proxy to exit immediately even if threads 
	 * are still running.
	 */
	@Override
	public void kill() throws RemoteException
	{
		s_Logger.info("Kill");
		m_Proxy.kill();
	}
	
	@Override
	public boolean startCav(Date timeOfIncident, List<DataTypeConfig> datatypes)
	throws RemoteException
	{
		s_Logger.info("Start CAV");

		// Calculate the minimum activity time and store it in the database so
		// that the engine will pick it up
		m_Proxy.getDatabaseManager().setMinActivityTime(m_Proxy.getDataCollectionManager().getMinActivityTime(timeOfIncident));

		try 
		{
			return m_Proxy.getDatabaseManager().cleanDatabase() &&
					m_Proxy.getDataCollectionManager().startCav(timeOfIncident, datatypes);
		}
		catch (InvalidPluginPropertyException e) 
		{
			throw new RemoteException(e.toString());
		}
	}


	@Override
	public boolean stopCav() throws RemoteException
	{
		s_Logger.info("Stop CAV");
		return m_Proxy.getDataCollectionManager().stopCav();
	}


	@Override
	public boolean pauseCav() throws RemoteException
	{
		s_Logger.info("Pause CAV");
		return m_Proxy.getDataCollectionManager().pauseDataCollection();
	}
	
	@Override
	public boolean resumeCav() throws RemoteException
	{
		s_Logger.info("Resume CAV");
		return m_Proxy.getDataCollectionManager().resumeDataCollection();
	}

	
	@Override
	public List<ProcessStatus> getProcessesStatus() throws RemoteException
	{
		return m_Proxy.getProcessesStatus();
	}
		

	@Override
	public boolean isCavRunning() throws RemoteException 
	{
		return m_Proxy.getDataCollectionManager().isCavRunning();
	}
	
	@Override
	public boolean isCavFinished() throws RemoteException 
	{
		return m_Proxy.getDataCollectionManager().isCavFinished();
	}
	
	@Override
	public Date getCavTimeOfIncident() throws RemoteException
	{
		return m_Proxy.getDataCollectionManager().getCavTimeOfIncident();
	}
	
	@Override
	public CavStatus getCavStatus()
	{
		return m_Proxy.getCavStatus();
	}
	
	
	
	public void setProxy(Proxy proxy)
	{
		m_Proxy = proxy;
	}
	
	public String getServerName()
	{
		return m_ServerName;
	}
	
	public void setServerName(String serverName)
	{
		m_ServerName = serverName;
	}
	
}
