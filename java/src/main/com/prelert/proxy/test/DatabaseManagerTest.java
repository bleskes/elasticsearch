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

import java.util.Date;

import org.apache.log4j.Logger;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.prelert.proxy.datamanager.DatabaseManager;


/**
 * This class tests the methods of the proxy's database manager.
 */
public class DatabaseManagerTest
{
	static Logger s_Logger = Logger.getLogger(DatabaseManagerTest.class);

	static ApplicationContext s_ApplicationContext;


	/**
	 * Load the Spring Framework Application Context required for the tests in
	 * this file.
	 */
	@BeforeClass
	public static void createApplicationContext()
	{
		s_Logger.info("About to load databaseManagerTestContext.xml");
		s_ApplicationContext =
				new ClassPathXmlApplicationContext("databaseManagerTestContext.xml");
		s_Logger.info("Loaded databaseManagerTestContext.xml");
	}


	/**
	 * Release the Spring Framework Application Context that was loaded for
	 * the tests in this file.
	 */
	@AfterClass
	public static void releaseApplicationContext()
	{
		s_ApplicationContext = null;
		s_Logger.info("Released databaseManagerTestContext.xml");
	}


	/**
	 * Test that we can set a minimum activity time.
	 */
	@Test
	public void testSetMinActivityTime()
	{
		DatabaseManager databaseManager =
				s_ApplicationContext.getBean("databaseManager", DatabaseManager.class);

		// There's no return code, but we're still testing that no exception is
		// thrown
		databaseManager.setMinActivityTime(new Date(1234567890000L));
	}


	/**
	 * Test that we can get a customer ID from the database.
	 */
	@Test
	public void testGetCustomerId()
	{
		DatabaseManager databaseManager =
				s_ApplicationContext.getBean("databaseManager", DatabaseManager.class);

		String custId = databaseManager.getCustomerId();

		// Since there is no customer ID in the test database, null should have
		// been returned
		assertNull(custId);
	}


	/**
	 * Test that we can clean the database.
	 *
	 * Important: this test will mess up other tests that rely on the database
	 * containing particular data.  Therefore, it must be the penultimate test
	 * run (with the very last test being the shutdown test).
	 */
	@Test
	public void testCleanDatabase()
	{
		DatabaseManager databaseManager =
				s_ApplicationContext.getBean("databaseManager", DatabaseManager.class);

		assertTrue(databaseManager.cleanDatabase());
	}

}

