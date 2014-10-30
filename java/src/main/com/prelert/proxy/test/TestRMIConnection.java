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

package com.prelert.proxy.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.prelert.proxy.dao.RemoteCausalityDAO;
import com.prelert.proxy.dao.RemoteControlDAO;
import com.prelert.proxy.dao.RemoteDataSourceDAO;
import com.prelert.proxy.dao.RemoteEvidenceDAO;
import com.prelert.proxy.dao.RemoteIncidentDAO;
import com.prelert.proxy.dao.RemoteObjectFactoryDAO;
import com.prelert.proxy.dao.RemoteTimeSeriesDAO;



public class TestRMIConnection 
{
	private RemoteObjectFactoryDAO factory;
	
	private RemoteIncidentDAO incidentDAO; 
	private RemoteEvidenceDAO evidenceDAO;        
	private RemoteCausalityDAO causalityDAO;        
	private RemoteDataSourceDAO dataSourceDAO;        
	private RemoteTimeSeriesDAO timeSeriesDAO;        
	private RemoteControlDAO controlDAO;      

	@BeforeClass
	public static void oneTimeSetup()
	{
		// Create and install a security manager
		if (System.getSecurityManager() == null)
		{
			System.setSecurityManager(new RMISecurityManager());
		}
	}
	
	
	@Before
	public void setup()
	{
		try
		{
			Registry registry = LocateRegistryForTests.getRegistry();
			
			factory = (RemoteObjectFactoryDAO)registry.lookup("com.prelert.RemoteObjectFactory");
			
			incidentDAO = factory.getIncidentDAO("");
			evidenceDAO = factory.getEvidenceDAO("");       
			causalityDAO = factory.getCausalityDAO("");    
			dataSourceDAO = factory.getDataSourceDAO("");  
			timeSeriesDAO = factory.getTimeSeriesDAO("");   
			
			controlDAO = (RemoteControlDAO)registry.lookup("com.prelert.ControlServer");		
		}
		catch (RemoteException re)
		{
			fail("Failed with RemoteException: " + re.getMessage());
		}
		catch (NotBoundException nbe)
		{
			fail("Failed with NotBoundException: " + nbe.getMessage());
		}
	}

	@Test
	public void testConnections()
	{	
		assertNotNull("Could not connect to RemoteServerFactory", factory);
		assertNotNull("Could not connect to RemoteCauaslityDAO", causalityDAO);
		assertNotNull("Could not connect to RemoteDataSourceDAO", dataSourceDAO);
		assertNotNull("Could not connect to RemoteEvidenceDAO", evidenceDAO);
		assertNotNull("Could not connect to RemoteIncidentDAO", incidentDAO);
		assertNotNull("Could not connect to RemoteTimeSeriesDAO", timeSeriesDAO);
		assertNotNull("Could not connect to RemoteControlDAO", controlDAO);
	}

}
