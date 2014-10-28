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

import java.net.URL;
import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;

import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VimServiceLocator;


// TODO If the vSphere connection is dropped then try to reconnect. 
// Find what the specific connection error is and catch it.

/**
 * Class encapulates a connection to vSphere web services. 
 * 
 * Contains the ServiceContent and Service objects which are 
 * required for querying vCenter.
 */
public class VSphereConnection 
{
	private enum ConnectionState {CONNECTED, DISCONNECTED};
	
	private VimPortType m_Service;
	private ServiceContent m_ServiceContent;
	private VimServiceLocator m_ServiceLocator;
	private ManagedObjectReference m_ServiceInstanceRef;

	private ConnectionState m_ConnectionState;
	
	
	public VSphereConnection()
	{
		m_ServiceInstanceRef = new ManagedObjectReference();
		m_ServiceInstanceRef.setType("ServiceInstance");
		m_ServiceInstanceRef.set_value("ServiceInstance");
	      
		m_ConnectionState = ConnectionState.DISCONNECTED;
	}
	
	/**
	 * Connect to vCenter Web service.
	 * @param webServiceUrl
	 * @param username
	 * @param password
	 * @param ignoreCerts
	 * @return
	 * @throws ServiceException
	 * @throws RuntimeFault
	 * @throws RemoteException
	 */
	public boolean connect(URL webServiceUrl, String username, String password,
						boolean ignoreCerts) 
	throws ServiceException, RuntimeFault, RemoteException
	{
		if (m_ConnectionState == ConnectionState.DISCONNECTED)
		{	
			disconnect();
		}
		
		m_ServiceLocator = new VimServiceLocator();
		m_ServiceLocator.setMaintainSession(true);
		m_Service = m_ServiceLocator.getVimPort(webServiceUrl);
		m_ServiceContent = m_Service.retrieveServiceContent(m_ServiceInstanceRef);

		if (m_ServiceContent.getSessionManager() != null) 
		{      	 
			m_Service.login(m_ServiceContent.getSessionManager(), username, password, null);
		}
		m_ConnectionState = ConnectionState.CONNECTED;
		
		return true;		
	}
	   
	/**
	 * Disconnect from the vCenter Web service.
	 * @throws RuntimeFault
	 * @throws RemoteException
	 */
	public void disconnect() throws RuntimeFault, RemoteException
	{
		if (m_Service != null) 
		{
			m_Service.logout(m_ServiceContent.getSessionManager());
			m_Service = null;
			m_ServiceContent = null;

			m_ConnectionState = ConnectionState.DISCONNECTED;
		}
	}
		
	public boolean isConnected()
	{
		return m_ConnectionState == ConnectionState.CONNECTED;
	}
	
	/**
	 * Get <code>ServiceContent</code> for the service. ServiceContent gives
	 * access to Managers and properties defined by the service.
	 * @return
	 */
	public ServiceContent getServiceContent()
	{
		return m_ServiceContent;
	}
	
	/** 
	 * Get the service object. The service object exposes methods to manage
	 * VCentre. 
	 * It's properties are accessible through the <code>ServiceContent</code>
	 * object.
	 * @return
	 */
	public VimPortType getService()
	{
		return m_Service;
	} 
			
}
