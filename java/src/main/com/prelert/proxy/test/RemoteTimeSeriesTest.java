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
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.prelert.data.Attribute;
import com.prelert.data.DataSource;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;
import com.prelert.data.TimeSeriesConfig;
import com.prelert.data.TimeSeriesDataPoint;
import com.prelert.proxy.dao.RemoteObjectFactoryDAO;
import com.prelert.proxy.dao.RemoteTimeSeriesDAO;


/**
 * Test suite for the Proxy functions exposed by the TimeSeriesDAO interface.
 * 
 * Each test compares the results returned by the proxy to the results from 
 * a known good source.
 */
public class RemoteTimeSeriesTest
{
	private static RemoteTimeSeriesDAO s_RemoteDAO; 
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
		
		s_RemoteDAO = factory.getTimeSeriesDAO("");
		
		s_ReferenceDatabase = new LocalDatabaseTestUtil();
	}
	
	
	@Test
	public void testGetAttributeNames() throws RemoteException
	{
		List<String> dataTypeNames = getDataTypes();		
		
		for (String dataType : dataTypeNames)
		{
			List<String> refNames = s_ReferenceDatabase.getTimeSeriesDAO().getAttributeNames(dataType);
			List<String> testNames = s_RemoteDAO.getAttributeNames(dataType);
			
			assertEquals(refNames, testNames);
		}		
	}
	
	@Test
	public void testGetAttributeValues() throws RemoteException
	{
		List<String> dataTypeNames = getDataTypes();		
		
		for (String dataType : dataTypeNames)
		{
			List<String> refNames = s_ReferenceDatabase.getTimeSeriesDAO().getAttributeNames(dataType);
			for (String attrName : refNames)
			{
				List<String> refValues = s_ReferenceDatabase.getTimeSeriesDAO().getAttributeValues(dataType, attrName, null);
				List<String> testValues = s_RemoteDAO.getAttributeValues(dataType, attrName, null);				
				assertEquals(refValues, testValues);
			}			
		}	
	}
	
	@Test 
	public void testGetLatestTime() throws RemoteException
	{
		List<DataSource> sources = s_ReferenceDatabase.getDataSourceDAO().getAllDataSources();

		for (DataSource source : sources)
		{
			String typeName = source.getDataSourceType().getName();
			String sourceName = source.getSource();
			
			Date refTime = s_ReferenceDatabase.getTimeSeriesDAO().getLatestTime(typeName, sourceName);
			Date testTime = s_RemoteDAO.getLatestTime(typeName, sourceName);
			
			assertEquals(refTime, testTime);
		}
	}
	
	
	//@Test 
	public void testGetLatestTimeNullDataType() throws RemoteException
	{	
		// Test with null datatype and null source.
		Date refTime = s_ReferenceDatabase.getTimeSeriesDAO().getLatestTime(null, null);
		Date testTime = s_RemoteDAO.getLatestTime(null, null);
			
		assertEquals(refTime, testTime);
		
		// check for each data source with a null datatype.
		List<DataSource> sources = s_ReferenceDatabase.getDataSourceDAO().getAllDataSources();
		for (DataSource source : sources)
		{
			refTime = s_ReferenceDatabase.getTimeSeriesDAO().getLatestTime(null, source.getSource());
			testTime = s_RemoteDAO.getLatestTime(null, source.getSource());

			assertEquals(refTime, testTime);
		}
		
	}
		

	@Test
	public void testGetMetrics() throws RemoteException
	{
		List<String> dataTypeNames = getDataTypes();
		
		for (String dataType : dataTypeNames)
		{
			List<String> refMetrics = s_ReferenceDatabase.getTimeSeriesDAO().getMetrics(dataType);
			List<String> testMetrics = s_RemoteDAO.getMetrics(dataType);
			
			Collections.sort(refMetrics);
			Collections.sort(testMetrics);
			
			assertEquals(refMetrics, testMetrics);
		}
	}
	
		
	/**
	 * Tests exhaustively for every datatype, metric and source
	 */
	
	@Test 
	public void testAllGetDataPointsForTimeSpan() throws RemoteException
	{
		List<String> dataTypeNames = getDataTypes();
		
		for (String dataType : dataTypeNames)
		{
			List<String> metrics = s_ReferenceDatabase.getTimeSeriesDAO().getMetrics(dataType);
			Date latestTime = s_ReferenceDatabase.getTimeSeriesDAO().getLatestTime(dataType, null);
			
			Calendar cal = Calendar.getInstance();
			cal.setTime(latestTime);
			cal.add(Calendar.DAY_OF_YEAR, -14);
			Date earliestTime = cal.getTime();
			
			List<Attribute> attributes = new ArrayList<Attribute>();
			
			DataSourceType dataSourceType = new DataSourceType();
			dataSourceType.setDataCategory(DataSourceCategory.TIME_SERIES);
			
			for (String metric : metrics)
			{
				dataSourceType.setName(dataType);
				List<String> sources = s_ReferenceDatabase.getTimeSeriesDAO().getSourcesOrderByName(dataSourceType);
				
				
 				for (String source : sources)
				{
					List<TimeSeriesDataPoint> refPoints = 
						s_ReferenceDatabase.getTimeSeriesDAO().getDataPointsForTimeSpan(dataType, 
																		metric, earliestTime, latestTime, 
																		source, attributes, true) ;
					

					List<TimeSeriesDataPoint> testPoints = 
						s_RemoteDAO.getDataPointsForTimeSpan(dataType, metric, 
															earliestTime, latestTime, 
															source, attributes, true);
					
					for (int i = 0; i < refPoints.size(); i++)
					{
						if (comparePointsIgnoreFeaturesInTestNotRef(refPoints.get(i), testPoints.get(i)) == false)
						{
							System.out.println(refPoints.get(i) + " " + testPoints.get(i));
						}
						
						assertTrue(comparePointsIgnoreFeaturesInTestNotRef(refPoints.get(i), testPoints.get(i)));
					}
				}
			}
		}
	}
	
	@Test 
	public void testGetDataPointsForTimeSpan() throws RemoteException, ParseException
	{
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		String dataType = "system_udp";
		String metric = "Packets Sent";
		Date earliestTime = df.parse("2010-02-08 07:24:00");
		Date latestTime = df.parse("2010-02-11 18:24:00");
		String source = "lon-app02";
		List<Attribute> attributes = new ArrayList<Attribute>();
		List<TimeSeriesDataPoint> refPoints;
		List<TimeSeriesDataPoint> testPoints;
		
		refPoints = 
			s_ReferenceDatabase.getTimeSeriesDAO().getDataPointsForTimeSpan(dataType, 
															metric, earliestTime, latestTime, 
															source, attributes, true) ;
		
		testPoints = 
			s_RemoteDAO.getDataPointsForTimeSpan(dataType, metric, 
												earliestTime, latestTime, 
												source, attributes, true);
		
		for (int i = 0; i < refPoints.size(); i++)
		{
			assertTrue(comparePointsIgnoreFeaturesInTestNotRef(refPoints.get(i), testPoints.get(i)));
		}
		
		
		dataType = "system_udp";
		metric = "Packets Sent";
		earliestTime = df.parse("2010-02-10 14:31:30");
		latestTime = df.parse("2010-02-10 15:31:30");
		source = "lon-data01";
		
		refPoints = s_ReferenceDatabase.getTimeSeriesDAO().getDataPointsForTimeSpan(dataType, 
															metric, earliestTime, latestTime, 
															source, attributes, true) ;
		
		
		testPoints = s_RemoteDAO.getDataPointsForTimeSpan(dataType, metric, 
												earliestTime, latestTime, 
												source, attributes, true);
		
		for (int i = 0; i < refPoints.size(); i++)
		{
			assertTrue(comparePointsIgnoreFeaturesInTestNotRef(refPoints.get(i), testPoints.get(i)));
		}
				
		
		dataType = "orca";
		metric = "Tcp Buffer Overflow'";
		earliestTime = df.parse("2010-02-09 14:55:00");
		latestTime = df.parse("2010-02-09 15:55:00");
		source = "lon-data01";
		
		refPoints = s_ReferenceDatabase.getTimeSeriesDAO().getDataPointsForTimeSpan(dataType, 
															metric, earliestTime, latestTime, 
															source, attributes, true) ;

		testPoints = s_RemoteDAO.getDataPointsForTimeSpan(dataType, metric, 
												earliestTime, latestTime, 
												source, attributes, true);
		
		for (int i = 0; i < refPoints.size(); i++)
		{
			assertTrue(comparePointsIgnoreFeaturesInTestNotRef(refPoints.get(i), testPoints.get(i)));
		}
		
		
		dataType = "app_usage";
		metric = "total'";
		earliestTime = df.parse("2010-02-08 17:00:00");
		latestTime = df.parse("2010-02-08 18:00:00");
		source = "lon-app01";
		
		refPoints = s_ReferenceDatabase.getTimeSeriesDAO().getDataPointsForTimeSpan(dataType, 
															metric, earliestTime, latestTime, 
															source, attributes, true) ;

		testPoints = s_RemoteDAO.getDataPointsForTimeSpan(dataType, metric, 
												earliestTime, latestTime, 
												source, attributes, true);
		
		for (int i = 0; i < refPoints.size(); i++)
		{
			assertTrue(comparePointsIgnoreFeaturesInTestNotRef(refPoints.get(i), testPoints.get(i)));
		}
	}
	
	
	@Test
	public void testPoints() throws ParseException, RemoteException
	{
		DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
		
		String dataType = "app_usage";
		String metric = "total";
		String source = "lon-app01";
		Date earliestTime = df.parse("02/08/10 7:00 am");
		Date latestTime = df.parse("02/11/10 18:46 pm");

		List<Attribute> attributes = new ArrayList<Attribute>();
		
		
		List<TimeSeriesDataPoint> refPoints = 
			s_ReferenceDatabase.getTimeSeriesDAO().getDataPointsForTimeSpan(dataType, 
															metric, earliestTime, latestTime, 
															source, attributes, true) ;
		

		List<TimeSeriesDataPoint> testPoints = 
			s_RemoteDAO.getDataPointsForTimeSpan(dataType, metric, 
												earliestTime, latestTime, 
												source, attributes, true);
			
		
		for (int i = 0; i < refPoints.size(); i++)
		{		
			assertTrue(comparePointsIgnoreFeaturesInTestNotRef(refPoints.get(i), testPoints.get(i)));
		}
		
	}


	/**
	 * Test for getting time series config corresponding to a given time series
	 * feature.
	 */
	@Test
	public void testConfigFromFeature() throws ParseException, RemoteException
	{
		int featureId = 16180;

		TimeSeriesConfig refConfig = s_ReferenceDatabase.getTimeSeriesDAO().getTimeSeriesFromFeature(featureId);

		TimeSeriesConfig testConfig = s_RemoteDAO.getTimeSeriesFromFeature(featureId);

		assertEquals(refConfig.getDataType(), testConfig.getDataType());
		assertEquals(refConfig.getMetric(), testConfig.getMetric());
		assertEquals(refConfig.getSource(), testConfig.getSource());
	}


	/**
	 * Test lookup of external keys to time series IDs and back.
	 */
	@Test
	public void testTimeSeriesIdFromExternalKey() throws ParseException, RemoteException
	{
		// This test relies on the exact data in the test_external_time_series
		// database
		for (int rowNum = 1; rowNum <= 15; ++rowNum)
		{
			Integer timeSeriesId = s_RemoteDAO.getTimeSeriesIdFromExternalKey("app_usage", "test-" + rowNum);

			assertNotNull(timeSeriesId);
			assertEquals(-rowNum, timeSeriesId.intValue());

			String externalKey = s_RemoteDAO.getExternalKeyFromTimeSeriesId(timeSeriesId);

			assertNotNull(externalKey);
			assertEquals("test-" + rowNum, externalKey);
		}

		Integer timeSeriesId = s_RemoteDAO.getTimeSeriesIdFromExternalKey("app_usage", "rubbish");
		assertNull(timeSeriesId);

		String externalKey = s_RemoteDAO.getExternalKeyFromTimeSeriesId(0);
		assertNull(externalKey);
	}


	private List<String> getDataTypes()
	{
		List<DataSourceType> types = s_ReferenceDatabase.getDataSourceDAO().getDataSourceTypes();
		List<String> dataTypeNames = new ArrayList<String>();
		
		for (DataSourceType type : types)
		{
			// The "coffee" plugin is a dummy type created by tests that test
			// creation of new types, and, since it doesn't exist, causes the
			// tests in this file to fail unless we filter it out
			if (!type.getName().equals("coffee") &&
				type.getDataCategory() == DataSourceCategory.TIME_SERIES)
			{
				dataTypeNames.add(type.getName());
			}
		}
		
		return dataTypeNames;
	}
	
	
	/**
	 * This function is to get around the zoom level on Time Series data.
	 * Not all features will appear in a Time Series if you zoom out and the points
	 * get aggregated. 
	 * 
	 * Compares 2 points for equality. Returns true if both are equal.
	 * Any feature in the refPoint must be present in the test point.
	 * Points can be equal if testPoint contains a feature which is not
	 * present in the refPoint. 
	 * @param refPoint
	 * @param testPoint
	 * @return 
	 */
	private boolean comparePointsIgnoreFeaturesInTestNotRef(TimeSeriesDataPoint refPoint, TimeSeriesDataPoint testPoint)
	{
		boolean result = refPoint.equalDisregardingFeatures(testPoint);
		if (result)
		{
			if (refPoint.getFeature() != null)
			{
				if (testPoint.getFeature() == null)
				{
					result = false;
				}
				else
				{
					int refId = refPoint.getFeature().getId();
					int testId = testPoint.getFeature().getId();

 					result = (refId == testId);
				}
			}
		}

		if (!result)
		{
			System.out.println(refPoint);
			System.out.println(testPoint);
		}
		
		return result;		
	}
	
}
