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

package com.prelert.proxy.datamanager;

import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.prelert.data.Attribute;
import com.prelert.data.DataSourceCategory;
import com.prelert.proxy.datamanager.dao.DatabaseManagerDAO;


/**
 * Class for managing the Prelert database.
 */
public class DatabaseManager 
{
	private static Logger s_Logger = Logger.getLogger(DatabaseManager.class);

	/**
	 * Database data access object.
	 */
	private DatabaseManagerDAO m_DatabaseManagerDao;


	/**
	 * Delete all incidents, evidence, time series points, external time series,
	 * metric paths and attributes from the database
	 */
	public boolean cleanDatabase()
	{
		return m_DatabaseManagerDao.cleanDatabase();
	}


	/**
	 * Return the customer ID for this installation.  A null customer ID
	 * indicates that no usage data is to be gathered.
	 * @return the customer ID for this installation (which may be null)
	 */
	public String getCustomerId()
	{
		return m_DatabaseManagerDao.getCustomerId();
	}


	/**
	 * Set the minimum time for which the engine should create activities.
	 * @param minTime The minimum time for which the engine should create
	 *                activities.
	 */
	public void setMinActivityTime(Date minTime)
	{
		s_Logger.debug("Setting min activity time to " + minTime);
		
		m_DatabaseManagerDao.setMinActivityTime(minTime);
	}
	
	
	/**
	 * Set the columns in the GUI display grids with the list of attributes.
	 * 
	 * @param datatype The Prelert datatype.
	 * @param category Should be either NOTIFICATION or TIME_SERIES_FEATURE.
	 * @param attributes Columns will be named with the names of these attributes.
	 */
	public void populateDisplayColumns(String datatype, DataSourceCategory category, 
										List<Attribute> attributes)
	{
		s_Logger.info("Setting the GUI display columns.");
		
		m_DatabaseManagerDao.populateDisplayColumns(datatype, category, attributes);
	}
	
	
	/**
	 * The database data access object.
	 * @return
	 */
	public DatabaseManagerDAO getDatabaseManagerDAO()
	{
		return m_DatabaseManagerDao;
	}
	
	public void setDatabaseManagerDAO(DatabaseManagerDAO databaseManagerDao)
	{
		m_DatabaseManagerDao = databaseManagerDao;
	}
}

