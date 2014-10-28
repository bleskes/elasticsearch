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

package com.prelert.dao.proxy;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import com.prelert.proxy.dao.RemoteObjectFactoryDAO;


/**
 * Abstract Base RemoteoDAOProxyRMI class. 
 *
 * Connections to remote RMI objects require the <code>RMIHostName</code>
 * The <code>RMIPort</code> defaults to 1099 (RMI's "well known" port) if not
 * specified.
 *
 * Optionally each GUI can specify a different <code>OriginatorName</code>, so
 * that they can share the same proxy.  The proxy can access a different
 * database for each originator name.
 * @author dkyle
 */
public abstract class RemoteProxyDAO 
{
	private String m_RMIHostName;
	private int    m_RMIPort;
	private String m_OriginatorName;


	/**
	 * Construct with default values.
	 */
	public RemoteProxyDAO()
	{
		m_RMIHostName = "localhost";
		m_RMIPort = Registry.REGISTRY_PORT;
		m_OriginatorName = "";
	}
	
	
	/**
	 * Looks up the <code>RemoteObjectFactoryDAO</code> object in the
	 * registry and returns it.
	 * 
	 * @return
	 * @throws RemoteException
	 * @throws NotBoundException
	 */
	public RemoteObjectFactoryDAO getRemoteFactory() throws RemoteException, NotBoundException
	{
		Registry registry = LocateRegistry.getRegistry(getRMIHostName(), getRMIPort());
		return (RemoteObjectFactoryDAO)registry.lookup("com.prelert.RemoteObjectFactory");
	}


	/**
	 * Get the name of the machine on which the RMI registry server is running.
	 * @return The name of the machine on which the RMI registry server is
	 *         running.
	 */
	public String getRMIHostName()
	{
		return m_RMIHostName;
	}


	/**
	 * Set the name or IP address of the machine to find the
	 * RMI registry server on.
	 * @param serverName
	 */
	public void setRMIHostName(String serverName)
	{
		m_RMIHostName = serverName;
	}


	/**
	 * Get the TCP port that the RMI registry is running on.
	 * @return The TCP port that the RMI registry is running on.
	 */
	public int getRMIPort()
	{
		return m_RMIPort;
	}


	/**
	 * Set the TCP port that the RMI registry server should run on.
	 * @param port
	 */
	public void setRMIPort(int port)
	{
		m_RMIPort = port;
	}


	/**
	 * The originator string which is passed to the proxy to identify
	 * which database to use.
	 * @return The originator string which is passed to the proxy to
	 *         identify which database to use.
	 */
	public String getOriginatorName()
	{
		return m_OriginatorName;
	}


	/**
	 * The originator string which is passed to the proxy to identify
	 * which database to use.
	 * @param originator
	 */
	public void setOriginatorName(String originator)
	{
		m_OriginatorName = originator;
	}

}
