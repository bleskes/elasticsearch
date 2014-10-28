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

import org.apache.log4j.Logger;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.simple.SimpleJdbcDaoSupport;

import com.prelert.data.Attribute;
import com.prelert.data.DataSourceCategory;
import com.prelert.server.ServerUtil;

/**
 * PostgreSQL implementation of DatabaseManagerDAO
 */
public class DatabaseManagerPostgreSQLDAO extends SimpleJdbcDaoSupport implements DatabaseManagerDAO 
{
	private static Logger s_Logger = Logger.getLogger(DatabaseManagerPostgreSQLDAO.class);
	
	/**
	 * The PostgreSQL pruning functions may deadlock and back off, but obviously
	 * after a while we should suspect a bug rather than merely contention
	 */
	static final int MAX_DEADLOCKS = 100;
	
	
	@Override
	public boolean cleanDatabase()
	{
		boolean result = true;

		s_Logger.info("About to clean database");

		try
		{
			Date timeNow = new Date();
			Date pruneTime = new Date(timeNow.getTime() + FUTURE_MS);

			// Required for queries that return void.
			JdbcOperations jdbcOps = getSimpleJdbcTemplate().getJdbcOperations();

			// This query never fails and returns void
			jdbcOps.execute("select * from prune_incidents()");

			// The other queries return false if they need to back off due to a
			// deadlock, so we have to try them multiple times
			boolean complete = false;
			int attempt = 1;
			while (attempt <= MAX_DEADLOCKS && !complete)
			{
				++attempt;
				complete = getSimpleJdbcTemplate().queryForObject("select * from prune_evidence(?)",
									java.lang.Boolean.class, pruneTime);
			}
			result = result && complete;

			complete = false;
			attempt = 1;
			while (attempt <= MAX_DEADLOCKS && !complete)
			{
				++attempt;
				complete = getSimpleJdbcTemplate().queryForObject("select * from prune_time_series_points(?)",
												java.lang.Boolean.class, pruneTime);
			}
			result = result && complete;

			complete = false;
			attempt = 1;
			while (attempt <= MAX_DEADLOCKS && !complete)
			{
				++attempt;
				complete = getSimpleJdbcTemplate().queryForObject("select * from delete_all_external_time_series()",
												java.lang.Boolean.class);
			}
			result = result && complete;

			complete = false;
			attempt = 1;
			while (attempt <= MAX_DEADLOCKS && !complete)
			{
				++attempt;
				complete = getSimpleJdbcTemplate().queryForObject("select * from prune_time_series_metric_paths()",
																	java.lang.Boolean.class);
			}
			result = result && complete;

			complete = false;
			attempt = 1;
			while (attempt <= MAX_DEADLOCKS && !complete)
			{
				++attempt;
				complete = getSimpleJdbcTemplate().queryForObject("select * from prune_attribute()",
															java.lang.Boolean.class);
			}
			result = result && complete;

			if (result == true)
			{
				s_Logger.info("Finished cleaning database");
			}
			else
			{
				s_Logger.error("Finished cleaning database - some data could not be removed");
			}
		}
		catch (BadSqlGrammarException e)
		{
			s_Logger.warn("Database not cleaned due to bad grammar exception -"
					+ " assuming this is because the database is not PostgreSQL");
		}

		return result;
	}
	
	
	@Override
	public String getCustomerId() 
	{
		String query = "select * from cust_id()";
		try
		{
		    return getSimpleJdbcTemplate().queryForObject(query, String.class);
		}
		catch (BadSqlGrammarException e)
		{
			s_Logger.warn("Customer ID not found due to bad grammar exception -"
					+ " assuming this is because the database is not PostgreSQL");
		}
		return null;
	}

	
	@Override
	public void setMinActivityTime(Date minTime) 
	{
		String query = "select * from set_evidence_link_min_time(?)";
		try
		{
			// We don't care what the result is here, as we're setting a value
			getSimpleJdbcTemplate().queryForList(query, minTime);

			s_Logger.info("Set minimum activity time to " + minTime);
		}
		catch (BadSqlGrammarException e)
		{
			s_Logger.warn("Minimum activity time not set due to bad grammar exception -"
					+ " assuming this is because the database is not PostgreSQL");
		}
	}
	

	@Override
	public void populateDisplayColumns(String datatype,
			DataSourceCategory category, List<Attribute> attributes) 
	{
		String query = "select * from populate_display_columns(?,?,?,?)";
		try
		{
			String attributeNames = ServerUtil.prepareAttributeNameArgument(attributes);
			getSimpleJdbcTemplate().queryForList(query, datatype, category.toString(),
									ServerUtil.DELIMITER, attributeNames);
		}
		catch (BadSqlGrammarException e)
		{
			s_Logger.error("Could not set the GUI display columns.");
		}
	}

}
