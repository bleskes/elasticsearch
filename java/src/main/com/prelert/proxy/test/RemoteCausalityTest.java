/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2013     *
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

import static org.junit.Assert.assertEquals;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.prelert.data.Evidence;
import com.prelert.data.ProbableCause;
import com.prelert.proxy.dao.RemoteCausalityDAO;
import com.prelert.proxy.dao.RemoteObjectFactoryDAO;


/**
 * Test suite for the Proxy functions exposed by the CausalityDAO interface.
 * 
 * Each test compares the results returned by the proxy to the results from 
 * a known good source.
 * 
 * Tests will only succeed if both the Proxy is already running.
 * @author dkyle
 */
public class RemoteCausalityTest 
{
	private static RemoteCausalityDAO s_RemoteDAO; 
	private static LocalDatabaseTestUtil s_ReferenceDatabase;

	@BeforeClass
	public static void oneTimeSetup() throws RemoteException, MalformedURLException, NotBoundException, Exception
	{
		// Create and install a security manager
		if (System.getSecurityManager() == null)
		{
			System.setSecurityManager(new RMISecurityManager());
		}
		
		Registry registry = LocateRegistryForTests.getRegistry();
		RemoteObjectFactoryDAO factory = (RemoteObjectFactoryDAO)registry.lookup("com.prelert.RemoteObjectFactory");
		
		s_RemoteDAO = factory.getCausalityDAO("");

		s_ReferenceDatabase = new LocalDatabaseTestUtil();
	}
	
	//@Test
	public void testGetProbableCause() throws RemoteException
	{
		Comparator<ProbableCause> comparator = new Comparator<ProbableCause>()
			{
				@Override
				public int compare(ProbableCause a, ProbableCause b)
				{
					return a.getEvidenceId() - b.getEvidenceId();
				}
			};

		
		List<ProbableCause> ref;
		List<ProbableCause> test;
		
		ref = s_ReferenceDatabase.getCausalityDAO().getProbableCauses(15000, 900, true);
		test = s_RemoteDAO.getProbableCauses(15000, 900, true);	
		
		Collections.sort(ref, comparator);
		Collections.sort(test, comparator);
		
		for (int i =0; i < ref.size(); ++i)
		{
			ProbableCause r = ref.get(i);
			ProbableCause t = test.get(i);

			assertEquals(r, t);
		}
		
		assertEquals(ref, test);
		
		ref = s_ReferenceDatabase.getCausalityDAO().getProbableCauses(14455, 900, true);
		test = s_RemoteDAO.getProbableCauses(14455, 900, true);		
		
		Collections.sort(ref, comparator);
		Collections.sort(test, comparator);
		
		for (int i =0; i < ref.size(); ++i)
		{
			ProbableCause r = ref.get(i);
			ProbableCause t = test.get(i);

			assertEquals(r, t);
		}
		
		//assertEquals(ref, test);
		
		ref = s_ReferenceDatabase.getCausalityDAO().getProbableCauses(16538, 900, true);
		test = s_RemoteDAO.getProbableCauses(16538, 900, true);	
		
		Collections.sort(ref, comparator);
		Collections.sort(test, comparator);
		
		assertEquals(ref, test);		

		ref = s_ReferenceDatabase.getCausalityDAO().getProbableCauses(29090, 900, true);
		test = s_RemoteDAO.getProbableCauses(29090, 900, true);
		
		Collections.sort(ref, comparator);
		Collections.sort(test, comparator);
		
		assertEquals(ref, test);	
	}
	

	@Test
	public void testGetAtTime() throws RemoteException
	{
		int evidenceId = 0;
		Date time = new Date();
		List<String> filterAttributes = new ArrayList<String>();
		List<String> filterValues = new ArrayList<String>();
		int pageSize = 20;
		
		List<Evidence> ref = s_ReferenceDatabase.getCausalityDAO().getAtTime(
				true, evidenceId, time, filterAttributes, filterValues, pageSize);
		List<Evidence> test = s_RemoteDAO.getAtTime(
				true, evidenceId, time, filterAttributes, filterValues, pageSize);
		assertEquals(ref, test);
	}
	
	
	@Test
	public void testGetFirstPage() throws RemoteException
	{
		int evidenceId = 6343;
		List<String> filterAttributes = null;
		List<String> filterValues = null;
		int pageSize = 20;
		
		List<Evidence> ref = s_ReferenceDatabase.getCausalityDAO().getFirstPage(
				true, evidenceId, filterAttributes, filterValues, pageSize);
		List<Evidence> test = s_RemoteDAO.getFirstPage(
				true, evidenceId, filterAttributes, filterValues, pageSize);
		assertEquals(ref, test);
		
		evidenceId = 14811;
		
		ref = s_ReferenceDatabase.getCausalityDAO().getFirstPage(
				true, evidenceId, filterAttributes, filterValues, pageSize);
		test = s_RemoteDAO.getFirstPage(
				true, evidenceId, filterAttributes, filterValues, pageSize);
		assertEquals(ref, test);
		
		
		evidenceId = 16920;
		
		ref = s_ReferenceDatabase.getCausalityDAO().getFirstPage(
				true, evidenceId, filterAttributes, filterValues, pageSize);
		test = s_RemoteDAO.getFirstPage(
				true, evidenceId, filterAttributes, filterValues, pageSize);
		assertEquals(ref, test);
		
		evidenceId = 28800;
		
		ref = s_ReferenceDatabase.getCausalityDAO().getFirstPage(
				true, evidenceId, filterAttributes, filterValues, pageSize);
		test = s_RemoteDAO.getFirstPage(
				true, evidenceId, filterAttributes, filterValues, pageSize);
		assertEquals(ref, test);
		
		evidenceId = 16474;
		
		ref = s_ReferenceDatabase.getCausalityDAO().getFirstPage(
				true, evidenceId, filterAttributes, filterValues, pageSize);
		test = s_RemoteDAO.getFirstPage(
				true, evidenceId, filterAttributes, filterValues, pageSize);
		assertEquals(ref, test);
	}
	
	
	@Test
	public void testGetLastPage() throws RemoteException
	{
		int evidenceId = 6343;
		List<String> filterAttributes = null;
		List<String> filterValues = null;
		int pageSize = 20;
		
		List<Evidence> ref = s_ReferenceDatabase.getCausalityDAO().getLastPage(
				true, evidenceId, filterAttributes, filterValues, pageSize);
		List<Evidence> test = s_RemoteDAO.getLastPage(
				true, evidenceId, filterAttributes, filterValues, pageSize);
		assertEquals(ref, test);
		
		evidenceId = 14811;
		
		ref = s_ReferenceDatabase.getCausalityDAO().getLastPage(
				true, evidenceId, filterAttributes, filterValues, pageSize);
		test = s_RemoteDAO.getLastPage(
				true, evidenceId, filterAttributes, filterValues, pageSize);
		assertEquals(ref, test);
		
		
		evidenceId = 16920;
		
		ref = s_ReferenceDatabase.getCausalityDAO().getLastPage(
				true, evidenceId, filterAttributes, filterValues, pageSize);
		test = s_RemoteDAO.getLastPage(
				true, evidenceId, filterAttributes, filterValues, pageSize);
		assertEquals(ref, test);
		
		evidenceId = 28800;
		
		ref = s_ReferenceDatabase.getCausalityDAO().getLastPage(
				true, evidenceId, filterAttributes, filterValues, pageSize);
		test = s_RemoteDAO.getLastPage(
				true, evidenceId, filterAttributes, filterValues, pageSize);
		assertEquals(ref, test);
		
		evidenceId = 16474;
		
		ref = s_ReferenceDatabase.getCausalityDAO().getLastPage(
				true, evidenceId, filterAttributes, filterValues, pageSize);
		test = s_RemoteDAO.getLastPage(
				true, evidenceId, filterAttributes, filterValues ,pageSize);
		assertEquals(ref, test);
	}
	
	
	@Test
	public void testGetNextPage() throws RemoteException
	{
		Calendar cal = Calendar.getInstance();
		cal.set(2010, 02, 10, 15, 01, 02);
		
		int bottomRowId = 14803;
		Date bottomRowTime = cal.getTime();
		List<String> filterAttributes = null;
		List<String> filterValues = null;
		int pageSize = 20;
		
		List<Evidence> ref = s_ReferenceDatabase.getCausalityDAO().getNextPage(
				true, bottomRowId, bottomRowTime, filterAttributes, filterValues ,pageSize);
		List<Evidence> test = s_RemoteDAO.getNextPage(
				true, bottomRowId, bottomRowTime, filterAttributes, filterValues, pageSize);
		assertEquals(ref, test);

		cal.set(2010, 02, 10, 15, 00, 30);
		bottomRowId = 16579;
		bottomRowTime = cal.getTime();
		
		ref = s_ReferenceDatabase.getCausalityDAO().getNextPage(
				true, bottomRowId, bottomRowTime, filterAttributes, filterValues, pageSize);
		test = s_RemoteDAO.getNextPage(
				true, bottomRowId, bottomRowTime, filterAttributes, filterValues, pageSize);
		assertEquals(ref, test);
		
		cal.set(2010, 02, 10, 15, 01, 30);
		bottomRowId = 16636;
		bottomRowTime = cal.getTime();
		
		ref = s_ReferenceDatabase.getCausalityDAO().getNextPage(
				true, bottomRowId, bottomRowTime, filterAttributes, filterValues, pageSize);
		test = s_RemoteDAO.getNextPage(
				true, bottomRowId, bottomRowTime, filterAttributes, filterValues, pageSize);
		assertEquals(ref, test);
		
		cal.set(2010, 02, 10, 15, 16, 00);
		bottomRowId = 16222;
		bottomRowTime = cal.getTime();
		
		ref = s_ReferenceDatabase.getCausalityDAO().getNextPage(
				true, bottomRowId, bottomRowTime, filterAttributes, filterValues, pageSize);
		test = s_RemoteDAO.getNextPage(
				true, bottomRowId, bottomRowTime, filterAttributes, filterValues, pageSize);
		assertEquals(ref, test);
	}
	
	
	@Test
	public void testGetPreviousPage() throws RemoteException
	{
		Calendar cal = Calendar.getInstance();
		cal.set(2010, 02, 10, 15, 01, 02);
		
		int topRowId = 14803;
		Date topRowTime = cal.getTime();
		List<String> filterAttributes = null;
		List<String> filterValues = null;
		int pageSize = 20;
		
		List<Evidence> ref = s_ReferenceDatabase.getCausalityDAO().getPreviousPage(
				true, topRowId, topRowTime, filterAttributes, filterValues, pageSize);
		List<Evidence> test = s_RemoteDAO.getPreviousPage(
				true, topRowId, topRowTime, filterAttributes, filterValues, pageSize);
		assertEquals(ref, test);

		cal.set(2010, 02, 10, 15, 00, 30);
		topRowId = 16576;
		topRowTime = cal.getTime();
		
		ref = s_ReferenceDatabase.getCausalityDAO().getPreviousPage(
				true, topRowId, topRowTime, filterAttributes, filterValues, pageSize);
		test = s_RemoteDAO.getPreviousPage(
				true, topRowId, topRowTime, filterAttributes, filterValues, pageSize);
		assertEquals(ref, test);
		
		cal.set(2010, 02, 10, 15, 00, 30);
		topRowId = 16597;
		topRowTime = cal.getTime();
		
		ref = s_ReferenceDatabase.getCausalityDAO().getPreviousPage(
				true, topRowId, topRowTime, filterAttributes, filterValues, pageSize);
		test = s_RemoteDAO.getPreviousPage(
				true, topRowId, topRowTime, filterAttributes, filterValues, pageSize);
		assertEquals(ref, test);
		
		cal.set(2010, 02, 10, 15, 16, 00);
		topRowId = 16214;
		topRowTime = cal.getTime();
		
		ref = s_ReferenceDatabase.getCausalityDAO().getPreviousPage(
				true, topRowId, topRowTime, filterAttributes, filterValues, pageSize);
		test = s_RemoteDAO.getPreviousPage(
				true, topRowId, topRowTime, filterAttributes, filterValues, pageSize);
		assertEquals(ref, test);
		
	}
	
	
	@Test
	public void testGetEarliestEvidence() throws RemoteException
	{
		int evidenceId = 6343;
		List<String> filterAttributes = null;
		List<String> filterValues = null;
		
		Evidence ref = s_ReferenceDatabase.getCausalityDAO().getEarliestEvidence(true, evidenceId, filterAttributes, filterValues);
		Evidence test = s_RemoteDAO.getEarliestEvidence(true, evidenceId, filterAttributes, filterValues);
		assertEquals(ref, test);
		
		evidenceId = 16920;
		ref = s_ReferenceDatabase.getCausalityDAO().getEarliestEvidence(true, evidenceId, filterAttributes, filterValues);
		test = s_RemoteDAO.getEarliestEvidence(true, evidenceId, filterAttributes, filterValues);
		assertEquals(ref, test);
		
		evidenceId = 14807;
		ref = s_ReferenceDatabase.getCausalityDAO().getEarliestEvidence(true, evidenceId, filterAttributes, filterValues);
		test = s_RemoteDAO.getEarliestEvidence(true, evidenceId, filterAttributes, filterValues);
		assertEquals(ref, test);
		
		evidenceId = 10659;
		ref = s_ReferenceDatabase.getCausalityDAO().getEarliestEvidence(true, evidenceId, filterAttributes, filterValues);
		test = s_RemoteDAO.getEarliestEvidence(true, evidenceId, filterAttributes, filterValues);
		assertEquals(ref, test);
	}
	
	@Test
	public void testGetLatestEvidence() throws RemoteException
	{
		int evidenceId = 6343;
		List<String> filterAttributes = null;
		List<String> filterValues = null;
		
		Evidence ref = s_ReferenceDatabase.getCausalityDAO().getLatestEvidence(true, evidenceId, filterAttributes, filterValues);
		Evidence test = s_RemoteDAO.getLatestEvidence(true, evidenceId, filterAttributes, filterValues);
		assertEquals(ref, test);
		
		evidenceId= 16920;
		ref = s_ReferenceDatabase.getCausalityDAO().getLatestEvidence(true, evidenceId, filterAttributes, filterValues);
		test = s_RemoteDAO.getLatestEvidence(true, evidenceId, filterAttributes, filterValues);
		assertEquals(ref, test);
		
		evidenceId= 14807;
		ref = s_ReferenceDatabase.getCausalityDAO().getLatestEvidence(true, evidenceId, filterAttributes, filterValues);
		test = s_RemoteDAO.getLatestEvidence(true, evidenceId, filterAttributes, filterValues);
		assertEquals(ref, test);

		evidenceId= 10659;
		ref = s_ReferenceDatabase.getCausalityDAO().getLatestEvidence(true, evidenceId, filterAttributes, filterValues);
		test = s_RemoteDAO.getLatestEvidence(true, evidenceId, filterAttributes, filterValues);
		assertEquals(ref, test);
		
	}

}
