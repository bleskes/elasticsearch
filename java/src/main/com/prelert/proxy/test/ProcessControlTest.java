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

import static org.junit.Assert.*;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.prelert.data.ProcessStatus;
import com.prelert.data.ProcessStatus.ProcessRunStatus;
import com.prelert.process.ProcessManager;

public class ProcessControlTest 
{
	private static ProcessManager m_ProcessManager;
	
	@BeforeClass
	public static void oneTimeSetup() throws RemoteException, MalformedURLException, NotBoundException, Exception
	{
		m_ProcessManager = new ProcessManager();
	}
	
	
	@Test
	public void testControlProcesses() throws RemoteException, InterruptedException
	{
		List<ProcessStatus> statusList = m_ProcessManager.getProcessesStatus();

		boolean result = ProcessManager.startProcesses();
		assertTrue(result);

		Thread.sleep(20000);
		
		statusList = m_ProcessManager.getProcessesStatus();
		
		// should be running or starting
		for (ProcessStatus status : statusList)
		{
			assertEquals(ProcessRunStatus.RUNNING, status.getStatus()) ;
		}
		
		result = ProcessManager.stopProcesses();
		assertTrue(result);

		Thread.sleep(5000);
		
		statusList = m_ProcessManager.getProcessesStatus();
		for (ProcessStatus status : statusList)
		{
			assertEquals(ProcessRunStatus.STOPPED, status.getStatus());
		}
	}
	
}
