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

import static org.junit.Assert.assertEquals;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;
import com.prelert.data.Evidence;
import com.prelert.proxy.dao.RemoteEvidenceDAO;
import com.prelert.proxy.dao.RemoteObjectFactoryDAO;

/**
 * Test suite for the Proxy functions exposed by the EvidenceDAO interface.
 * 
 * NOTE: Most of these tests are redundant as the EvidenceServerRMI object 
 * contains not logic it simply channels the calls straight to a database 
 * DAO. 
 * 
 * Prelert notification types and time series features are always stored
 * internally so they will never be returned directly by a plugin.
 *
 * Each test compares the results returned by the proxy to the results from 
 * a known good source.
 */


/*
 * Some of these tests are commented out as they currently fail on Postgres. 
 * Postgres needs a global transactionManager to be defined to handle functions
 * that return cursors. This test works with two different database sources:
 * 'test_external_time_series' which is setup with external datatypes and 
 * 'reference' which is an exact copy but all the datatypes are set as 
 * internal.
 * 
 * The testPlugin uses the reference database and the tests assert that 
 * the data returned through the proxy is the same as the data read 
 * directly from the database.
 * 
 * Spring doesn't work with 2 transaction managers defined for different
 * data sources so, for now, we can't use 2 different data sources in the 
 * Postgres tests.
 * 
 */

public class RemoteEvidenceTest 
{
	private static RemoteEvidenceDAO s_RemoteDAO; 
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
		
		s_RemoteDAO = factory.getEvidenceDAO("");
		
		s_ReferenceDatabase = new LocalDatabaseTestUtil();		
	}
	
	// See comment at the top of the file.
	//@Test
	public void testGetAtTime() throws RemoteException, ParseException
	{
		DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
		List<String> filterAttributes = null;
		List<String> filterValues = null;
		
		List<Evidence> refResults;
		List<Evidence> testResults;

		String dataType = new String("data_log");
		String source = new String("lon-data02");
		Date time = df.parse("2/11/10 8:01 AM");	
		int pageSize = 20;
		
		testResults = s_RemoteDAO.getAtTime(dataType, source, time, filterAttributes, filterValues, pageSize);
		refResults = s_ReferenceDatabase.getEvidenceDAO().getAtTime(
				dataType, source, time, filterAttributes, filterValues, pageSize);
		assertEquals(refResults, testResults);
		

		dataType = new String("app_log");
		source = new String("lon-app01");
		time = df.parse("2/11/10 6:44 PM");		
		
		testResults = s_RemoteDAO.getAtTime(dataType, source, time, filterAttributes, filterValues, pageSize);
		refResults = s_ReferenceDatabase.getEvidenceDAO().getAtTime(
				dataType, source, time, filterAttributes, filterValues, pageSize);
		assertEquals(refResults, testResults);
		
		
		dataType = new String("data_log");
		source = new String("lon-data01");
		time = df.parse("2/11/10 8:01 AM");		
		
		testResults = s_RemoteDAO.getAtTime(dataType, source, time, filterAttributes, filterValues, pageSize);
		refResults = s_ReferenceDatabase.getEvidenceDAO().getAtTime(
				dataType, source, time, filterAttributes, filterValues, pageSize);
		assertEquals(refResults, testResults);
	}
	
	
	//@Test
	public void testGetColumnValues() throws RemoteException
	{
		List<String> ref;
		List<String> test;

		final int MAX_ROWS = 2147483647;
		List<String> dataTypes = getNotificationDataTypes();

		for (String dataType : dataTypes)
		{
			List<String> refColumns = s_ReferenceDatabase.getEvidenceDAO().getAllColumns(dataType);
			List<String> testColumns = s_RemoteDAO.getAllColumns(dataType);
			assertEquals(refColumns, testColumns);

			for (String col : refColumns)
			{	
				ref = s_ReferenceDatabase.getEvidenceDAO().getColumnValues(dataType, col, MAX_ROWS);
				test = s_RemoteDAO.getColumnValues(dataType, col, MAX_ROWS);
				assertEquals(ref, test);
			}
		}
	}
	
	// See comment at the top of the file.
	//@Test
	public void getFirstPage() throws RemoteException
	{
		String dataType = "app_log";
		String source = null;
		
		List<String> filterAttributes = null;
		List<String> filterValues = null;
		int pageSize = 20;
		
		List<Evidence> refResults = s_ReferenceDatabase.getEvidenceDAO().getFirstPage(
				dataType, source, filterAttributes, filterValues, pageSize);
		List<Evidence> testResults = s_RemoteDAO.getFirstPage(
				dataType, source, filterAttributes, filterValues, pageSize);
		assertEquals(refResults, testResults);
		
		
		dataType = "data_log";
		
		refResults = s_ReferenceDatabase.getEvidenceDAO().getFirstPage(
				dataType, source, filterAttributes, filterValues, pageSize);
		testResults = s_RemoteDAO.getFirstPage(
				dataType, source, filterAttributes, filterValues, pageSize);
		assertEquals(refResults, testResults);
		
		
		dataType = "app_log";
		source = "lon-app04";
		
		refResults = s_ReferenceDatabase.getEvidenceDAO().getFirstPage(
				dataType, source, filterAttributes, filterValues, pageSize);
		testResults = s_RemoteDAO.getFirstPage(
				dataType, source, filterAttributes, filterValues, pageSize);
		assertEquals(refResults, testResults);


		dataType = "data_log";
		source = "lon-data01";
		
		refResults = s_ReferenceDatabase.getEvidenceDAO().getFirstPage(
				dataType, source, filterAttributes, filterValues, pageSize);
		testResults = s_RemoteDAO.getFirstPage(
				dataType, source, filterAttributes, filterValues, pageSize);
		assertEquals(refResults, testResults);
	}
	
	
	// See comment at the top of the file.
	//@Test
	public void getLastPage() throws RemoteException
	{
		String dataType = "app_log";
		String source = "lon-app01";
		
		List<String> filterAttributes = null;
		List<String> filterValues = null;
		int pageSize = 20;
		
		List<Evidence> refResults = s_ReferenceDatabase.getEvidenceDAO().getLastPage(
				dataType, source, filterAttributes, filterValues, pageSize);
		List<Evidence> testResults = s_RemoteDAO.getLastPage(
				dataType, source, filterAttributes, filterValues, pageSize);
		assertEquals(refResults, testResults);
		
		
		dataType = "app_log";
		source = "lon-app02";
		
		refResults = s_ReferenceDatabase.getEvidenceDAO().getLastPage(
				dataType, source, filterAttributes, filterValues, pageSize);
		testResults = s_RemoteDAO.getLastPage(
				dataType, source, filterAttributes, filterValues, pageSize);
		assertEquals(refResults, testResults);
		
		
		dataType = "data_log";
		source = "lon-data01";
		
		refResults = s_ReferenceDatabase.getEvidenceDAO().getLastPage(
				dataType, source, filterAttributes, filterValues, pageSize);
		testResults = s_RemoteDAO.getLastPage(
				dataType, source, filterAttributes, filterValues, pageSize);
		assertEquals(refResults, testResults);
		
		
		dataType = "data_log";
		source = "lon-data02";
		
		refResults = s_ReferenceDatabase.getEvidenceDAO().getLastPage(
				dataType, source, filterAttributes, filterValues, pageSize);
		testResults = s_RemoteDAO.getLastPage(
				dataType, source, filterAttributes, filterValues, pageSize);
		assertEquals(refResults, testResults);
	}
	
	
	// See comment at the top of the file.
	//@Test
	public void getNextPage() throws RemoteException, ParseException
	{
		DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
		
		String dataType = "app_log";
		String source = "lon-app01";
		Date time = df.parse("2/11/10 6:35 PM");
		int rowId = 7925;
		int pageSize = 20;
		
		List<String> filterAttributes = null;
		List<String> filterValues = null;
		
		List<Evidence> refResults = s_ReferenceDatabase.getEvidenceDAO().getNextPage(
				dataType, source, time, rowId, filterAttributes, filterValues, pageSize);
		List<Evidence> testResults = s_RemoteDAO.getNextPage(
				dataType, source, time, rowId, filterAttributes, filterValues, pageSize);
		assertEquals(refResults, testResults);
		
		
		dataType = "app_log";
		source = "lon-app01";
		time = df.parse("2/8/10 5:27 PM");
		rowId = 1772;
		
		refResults = s_ReferenceDatabase.getEvidenceDAO().getNextPage(
				dataType, source, time, rowId, filterAttributes, filterValues, pageSize);
		testResults = s_RemoteDAO.getNextPage(
				dataType, source, time, rowId, filterAttributes, filterValues, pageSize);
		assertEquals(refResults, testResults);


		dataType = "app_log";
		source = null;
		time = df.parse("2/11/10 6:35 PM");
		rowId = 7925;
		
		refResults = s_ReferenceDatabase.getEvidenceDAO().getNextPage(
				dataType, source, time, rowId, filterAttributes, filterValues, pageSize);
		testResults = s_RemoteDAO.getNextPage(
				dataType, source, time, rowId, filterAttributes, filterValues, pageSize);
		assertEquals(refResults, testResults);
		
		dataType = "data_log";
		source = "lon-app04";
		time = df.parse("2/11/10 4:37 PM");
		rowId = 15893;
		
		refResults = s_ReferenceDatabase.getEvidenceDAO().getNextPage(
				dataType, source, time, rowId, filterAttributes, filterValues, pageSize);
		testResults = s_RemoteDAO.getNextPage(
				dataType, source, time, rowId, filterAttributes, filterValues, pageSize);
		assertEquals(refResults, testResults);
	}
	
	
	// See comment at the top of the file.
	//@Test
	public void getPreviousPage() throws RemoteException, ParseException
	{
		DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
		
		String dataType = "app_log";
		String source = null;
		Date time = df.parse("2/10/10 3:01 PM");
		int rowId = 14803;
		int pageSize = 20;
		
		List<String> filterAttributes = null;
		List<String> filterValues = null;
		
		List<Evidence> refResults = s_ReferenceDatabase.getEvidenceDAO().getPreviousPage(
				dataType, source, time, rowId, filterAttributes, filterValues, pageSize);
		List<Evidence> testResults = s_RemoteDAO.getPreviousPage(
				dataType, source, time, rowId, filterAttributes, filterValues, pageSize);
		assertEquals(refResults, testResults);
		
		
		dataType = "app_log";
		source = null;
		time = df.parse("2/10/10 3:01 PM");
		rowId = 5507;
		
		refResults = s_ReferenceDatabase.getEvidenceDAO().getPreviousPage(
				dataType, source, time, rowId, filterAttributes, filterValues, pageSize);
		testResults = s_RemoteDAO.getPreviousPage(
				dataType, source, time, rowId, filterAttributes, filterValues, pageSize);
		assertEquals(refResults, testResults);
		
		dataType = "data_log";
		source = null;
		time = df.parse("2/10/10 3:01 PM");
		rowId = 15331;
		
		refResults = s_ReferenceDatabase.getEvidenceDAO().getPreviousPage(
				dataType, source, time, rowId, filterAttributes, filterValues, pageSize);
		testResults = s_RemoteDAO.getPreviousPage(
				dataType, source, time, rowId, filterAttributes, filterValues, pageSize);
		assertEquals(refResults, testResults);
		
		
		dataType = "data_log";
		source = null;
		time = df.parse("2/10/10 5:30 PM");
		rowId = 15348;
		
		refResults = s_ReferenceDatabase.getEvidenceDAO().getPreviousPage(
				dataType, source, time, rowId, filterAttributes, filterValues, pageSize);
		testResults = s_RemoteDAO.getPreviousPage(
				dataType, source, time, rowId, filterAttributes, filterValues, pageSize);
		assertEquals(refResults, testResults);
		
		dataType = "app_log";
		source = "lon-app04";
		time = df.parse("2/8/10 3:15 PM");
		rowId = 14455;
		
		refResults = s_ReferenceDatabase.getEvidenceDAO().getPreviousPage(
				dataType, source, time, rowId, filterAttributes, filterValues, pageSize);
		testResults = s_RemoteDAO.getPreviousPage(
				dataType, source, time, rowId, filterAttributes, filterValues, pageSize);
		assertEquals(refResults, testResults);
	}
	
	
	@Test 
	public void testGetEarliestEvidence() throws RemoteException
	{
		Evidence ref;
		Evidence test;

		String dataType = null;
		String source = null;
		List<String> filterValues = null;
		List<String> filterAttributes = null;
		
		ref = s_ReferenceDatabase.getEvidenceDAO().getEarliestEvidence(dataType, source, filterAttributes, filterValues);
		test = s_RemoteDAO.getEarliestEvidence(dataType, source, filterAttributes, filterValues);
		assertEquals(ref, test);		
		
		dataType = "app_log";
		source = null;
		
		ref = s_ReferenceDatabase.getEvidenceDAO().getEarliestEvidence(dataType, source, filterAttributes, filterValues);
		test = s_RemoteDAO.getEarliestEvidence(dataType, source, filterAttributes, filterValues);
		assertEquals(ref, test);		
		
		dataType = "app_log";
		source = "lon-app01";
		
		ref = s_ReferenceDatabase.getEvidenceDAO().getEarliestEvidence(dataType, source, filterAttributes, filterValues);
		test = s_RemoteDAO.getEarliestEvidence(dataType, source, filterAttributes, filterValues);
		assertEquals(ref, test);
	}	
	
	
	@Test 
	public void testGetLatestEvidence() throws RemoteException
	{
		Evidence ref;
		Evidence test;

		String dataType = null;
		String source = null;
		List<String> filterValues = null;
		List<String> filterAttributes = null;
		
		ref = s_ReferenceDatabase.getEvidenceDAO().getLatestEvidence(dataType, source, filterAttributes, filterValues);
		test = s_RemoteDAO.getLatestEvidence(dataType, source, filterAttributes, filterValues);
		assertEquals(ref, test);		
		
		dataType = "app_log";
		source = null;
		
		ref = s_ReferenceDatabase.getEvidenceDAO().getLatestEvidence(dataType, source, filterAttributes, filterValues);
		test = s_RemoteDAO.getLatestEvidence(dataType, source, filterAttributes, filterValues);
		assertEquals(ref, test);		
		
		dataType = "app_log";
		source = "lon-app01";
		
		ref = s_ReferenceDatabase.getEvidenceDAO().getLatestEvidence(dataType, source, filterAttributes, filterValues);
		test = s_RemoteDAO.getLatestEvidence(dataType, source, filterAttributes, filterValues);
		assertEquals(ref, test);
		
		
		dataType = "app_log";
		source = "lon-app02";
		
		ref = s_ReferenceDatabase.getEvidenceDAO().getLatestEvidence(dataType, source, filterAttributes, filterValues);
		test = s_RemoteDAO.getLatestEvidence(dataType, source, filterAttributes, filterValues);
		assertEquals(ref, test);
	}	
	
	
	@Test 
	public void testGetEvidenceSingle() throws RemoteException
	{
		// TODO 
	}
	
	@Test 
	public void testGetEvidenceAttributes() throws RemoteException
	{
		// TODO
	}
	
	
	private List<String> getNotificationDataTypes()
	{
		List<DataSourceType> types = s_ReferenceDatabase.getDataSourceDAO().getDataSourceTypes();
		List<String> dataTypeNames = new ArrayList<String>();
		
		for (DataSourceType type : types)
		{
			if (type.getDataCategory() == DataSourceCategory.NOTIFICATION)
			{
				dataTypeNames.add(type.getName());
			}
		}
		
		return dataTypeNames;
	}
	
}
