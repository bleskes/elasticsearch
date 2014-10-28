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

package com.prelert.proxy.datamanager.dao;

import java.util.Date;
import java.util.List;

import com.prelert.data.Attribute;
import com.prelert.data.DataSourceCategory;

/**
 * Interface to DAO objects used for managing the Prelert 
 * database and various utility routines. 
 */
public interface DatabaseManagerDAO 
{
	/**
	 * How far in the future should we prune the database?
	 */
	static final long FUTURE_MS = 86400000;
	
	
	/**
	 * Delete all incidents, evidence, time series points, external time series,
	 * metric paths and attributes from the database
	 */
	public boolean cleanDatabase();
	
	
	/**
	 * Return the customer ID for this installation.  A null customer ID
	 * indicates that no usage data is to be gathered.
	 * @return the customer ID for this installation (which may be null)
	 */
	public String getCustomerId();
	
	
	/**
	 * Set the minimum time for which the engine should create activities.
	 * @param minTime The minimum time for which the engine should create
	 *                activities.
	 */
	public void setMinActivityTime(Date minTime);
	
	
	/**
	 * Set the columns in the GUI display grids with the list of attributes.
	 * 
	 * @param datatype The Prelert datatype.
	 * @param category Should be either NOTIFICATION or TIME_SERIES_FEATURE.
	 * @param attributes Columns will be named with the names of these attributes.
	 */
	public void populateDisplayColumns(String datatype, DataSourceCategory category, 
						List<Attribute> attributes);
}
