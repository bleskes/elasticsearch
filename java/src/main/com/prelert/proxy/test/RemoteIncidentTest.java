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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.prelert.data.CausalityAggregate;
import com.prelert.data.Incident;
import com.prelert.data.MetricTreeNode;
import com.prelert.proxy.dao.RemoteIncidentDAO;
import com.prelert.proxy.dao.RemoteObjectFactoryDAO;

/**
 * Test suite for the Proxy functions exposed by the IncidenDAO interface.
 *
 * Each test compares the results returned by the proxy to the results from
 * a known good source.
 */

public class RemoteIncidentTest
{
	private static RemoteIncidentDAO s_RemoteDAO;
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

		s_RemoteDAO = factory.getIncidentDAO("");

		s_ReferenceDatabase = new LocalDatabaseTestUtil();
	}


	@Test
	public void testGetEarliestTime() throws RemoteException
	{
		Date referenceDate = s_ReferenceDatabase.getIncidentDAO().getEarliestTime();
		Date testDate = s_RemoteDAO.getEarliestTime();

		assertEquals(referenceDate, testDate);
	}


	@Test
	public void testGetLatestTime() throws RemoteException
	{
		Date referenceDate = s_ReferenceDatabase.getIncidentDAO().getLatestTime();
		Date testDate = s_RemoteDAO.getLatestTime();

		assertEquals(referenceDate, testDate);
	}


	@Test
	public void testGetIncidents() throws RemoteException
	{
		Date earliest = s_ReferenceDatabase.getIncidentDAO().getEarliestTime();
		Date latest = s_ReferenceDatabase.getIncidentDAO().getLatestTime();

		List<Incident> refIncidents = s_ReferenceDatabase.getIncidentDAO().getIncidentsAdaptive(earliest, latest, 0);
		List<Incident> testIncidents = s_RemoteDAO.getIncidentsAdaptive(earliest, latest, 0);

		assertEquals(refIncidents.size(), testIncidents.size());
		
		refIncidents = s_ReferenceDatabase.getIncidentDAO().getIncidents(earliest, latest, 0);
		testIncidents = s_RemoteDAO.getIncidents(earliest, latest, 0);

		assertEquals(refIncidents.size(), testIncidents.size());
	}


	@Test
	public void testGetIncidentAttributeNames() throws RemoteException
	{
		List<String> refNames = s_ReferenceDatabase.getIncidentDAO().getIncidentAttributeNames(16538);
		List<String> testNames = s_RemoteDAO.getIncidentAttributeNames(16538);

		assertEquals(refNames, testNames);
	}


	@Test
	public void testGetIncidentAttributeValues() throws RemoteException
	{
		List<String> refValues = s_ReferenceDatabase.getIncidentDAO().getIncidentAttributeValues(16538, "type");
		List<String> testValues = s_RemoteDAO.getIncidentAttributeValues(16538, "type");

		assertEquals(refValues, testValues);
	}


	@Test
	public void testGetIncidentSummary() throws RemoteException
	{
		List<String> refAttributes = Arrays.asList("node", "service", "username");
		List<String> testAttributes = Arrays.asList("node", "service", "username");

		List<CausalityAggregate> ref = s_ReferenceDatabase.getIncidentDAO().getIncidentSummary(16538, "type", refAttributes);
		List<CausalityAggregate> test = s_RemoteDAO.getIncidentSummary(16538, "type", testAttributes);
		assertEquals(ref, test);

		ref = s_ReferenceDatabase.getIncidentDAO().getIncidentSummary(16538, "description", refAttributes);
		test = s_RemoteDAO.getIncidentSummary(16538, "description", testAttributes);
		assertEquals(ref, test);

		ref = s_ReferenceDatabase.getIncidentDAO().getIncidentSummary(16538, "source", refAttributes);
		test = s_RemoteDAO.getIncidentSummary(16538, "source", testAttributes);
		assertEquals(ref, test);
	}


	@Test
	public void testGetFirstPage() throws RemoteException
	{
		List<Incident> refIncidents = s_ReferenceDatabase.getIncidentDAO().getFirstPage(25, 20);
		List<Incident> testIncidents = s_RemoteDAO.getFirstPage(25, 20);

		assertEquals(refIncidents, testIncidents);
	}


	// NB. 28-9-11: reference DB is corrupted in that there are 4 incidents
	// whose top_evidence is not in the evidence table, towards the end of
	// the time range.
	//@Test
	public void testGetLastPage() throws RemoteException
	{
		// TODO - enable Test annotation if reference DB is fixed.
		List<Incident> refIncidents = s_ReferenceDatabase.getIncidentDAO().getLastPage(25, 20);
		List<Incident> testIncidents = s_RemoteDAO.getLastPage(25, 20);

		assertEquals(refIncidents, testIncidents);
	}


	@Test
	public void testGetNextPage() throws RemoteException, ParseException
	{
		// Loads page after incident at 2010-02-08 06:53:43, evidence id 25415
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date rowTime = dateFormatter.parse("2010-02-08 06:53:43");
		int rowEvidenceId = 25415;
		int anomalyThreshold = 5;
		int pageSize = 20;

		List<Incident> refIncidents = s_ReferenceDatabase.getIncidentDAO().getNextPage(
				rowTime, rowEvidenceId, anomalyThreshold, pageSize);
		List<Incident> testIncidents = s_RemoteDAO.getNextPage(
				rowTime, rowEvidenceId, anomalyThreshold, pageSize);

		assertEquals(refIncidents, testIncidents);
	}


	@Test
	public void testGetPreviousPage() throws RemoteException, ParseException
	{
		// Loads page previous to incident at 2010-02-08 06:40:41, evidence id 25389
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date rowTime = dateFormatter.parse("2010-02-08 06:40:41");
		int rowEvidenceId = 25389;
		int anomalyThreshold = 5;
		int pageSize = 20;

		List<Incident> refIncidents = s_ReferenceDatabase.getIncidentDAO().getPreviousPage(
				rowTime, rowEvidenceId, anomalyThreshold, pageSize);
		List<Incident> testIncidents = s_RemoteDAO.getPreviousPage(
				rowTime, rowEvidenceId, anomalyThreshold, pageSize);

		assertEquals(refIncidents, testIncidents);
	}


	@Test
	public void testGetAtTime() throws RemoteException, ParseException
	{
		// Loads at 2010-02-08 06:40:41
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date time = dateFormatter.parse("2010-02-08 06:40:41");
		int anomalyThreshold = 5;
		int pageSize = 20;

		List<Incident> refIncidents = s_ReferenceDatabase.getIncidentDAO().getAtTime(
				time, anomalyThreshold, pageSize, false);
		List<Incident> testIncidents = s_RemoteDAO.getAtTime(
				time, anomalyThreshold, pageSize, false);

		assertEquals(refIncidents, testIncidents);
	}


	@Test
	public void testGetIncidentForId() throws RemoteException
	{
		Incident refIncident = s_ReferenceDatabase.getIncidentDAO().getIncidentForId(16538);
		Incident testIncident = s_RemoteDAO.getIncidentForId(16538);

		assertEquals(refIncident, testIncident);
	}


	/**
	 * Test getting metric path names.
	 */
	@Test
	public void testGetIncidentMetricPathNodes() throws ParseException, RemoteException
	{
		// This test relies on the exact data in the reference and
		// test_external_time_series databases

		// For nodes from the reference database, we should get the metric path
		// names
		List<MetricTreeNode> referenceNodes = s_ReferenceDatabase.getIncidentDAO().getIncidentMetricPathNodes(16954);
		assertEquals(4, referenceNodes.size());
		for (MetricTreeNode node : referenceNodes)
		{
			assertNotNull(node.getType());
			assertNotNull(node.getPrefix());
			assertNotNull(node.getName());
			assertNull(node.getExternalKey());
		}
		

		List<MetricTreeNode> externalNodes = s_RemoteDAO.getIncidentMetricPathNodes(16954);
		assertEquals(4, externalNodes.size());
		for (MetricTreeNode node : externalNodes)
		{
			assertNotNull(node.getType());
			assertNotNull(node.getPrefix());
			assertNotNull(node.getName());
			assertNull(node.getExternalKey());
		}
	}

}
