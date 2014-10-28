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

import java.sql.CallableStatement;
import java.sql.SQLException;
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
 * MySQL implementation of DatabaseManagerDAO
 */
public class DatabaseManagerMySQLDAO extends SimpleJdbcDaoSupport implements DatabaseManagerDAO
{
	private static Logger s_Logger = Logger.getLogger(DatabaseManagerMySQLDAO.class);
	
	
	@Override
	public boolean cleanDatabase()
	{
		s_Logger.info("About to clean database");

		try
		{
			Date timeNow = new Date();
			Date pruneTime = new Date(timeNow.getTime() + FUTURE_MS);

			// Required for queries that return void.
			JdbcOperations jdbcOps = getSimpleJdbcTemplate().getJdbcOperations();

			// This query never fails and returns void
			jdbcOps.execute("CALL prune_incidents()");

			getSimpleJdbcTemplate().update("CALL prune_evidence(?)", pruneTime);
			
			getSimpleJdbcTemplate().update("CALL prune_time_series_points(?)", pruneTime) ;

			try
			{
				// This database procedure has a OUT parameter 
				// so must be called through a CallableStatement.
				CallableStatement callStatement = getConnection().prepareCall("CALL delete_all_external_time_series(?)");
				callStatement.registerOutParameter(1, java.sql.Types.BOOLEAN);
				callStatement.execute();
			}
			catch (SQLException e)
			{
				s_Logger.warn("Could not delete the external time series when cleaning database.");
			}
			
	
			getSimpleJdbcTemplate().update("CALL prune_time_series_metric_paths()");
			
			getSimpleJdbcTemplate().update("CALL prune_attribute()");
			
			
			s_Logger.info("Finished cleaning database");
		}
		catch (BadSqlGrammarException e)
		{
			s_Logger.warn("Database not cleaned due to bad grammar exception -"
					+ " assuming this is because the database is not PostgreSQL", e);
		}

		return true;
	}
	
	@Override
	public String getCustomerId() 
	{
		String query = "CALL cust_id()";
		try
		{
		    return getSimpleJdbcTemplate().queryForObject(query, String.class);
		}
		catch (BadSqlGrammarException e)
		{
			s_Logger.warn("Customer ID not found due to bad grammar exception.");
		}
		return null;
	}

	@Override
	public void setMinActivityTime(Date minTime) 
	{
		String query = "CALL set_evidence_link_min_time(?)";
		try
		{
			// We don't care what the result is here, as we're setting a value
			getSimpleJdbcTemplate().update(query, minTime);

			s_Logger.info("Set minimum activity time to " + minTime);
		}
		catch (BadSqlGrammarException e)
		{
			s_Logger.warn("Minimum activity time not set due to bad grammar exception.");
		}
		
	}

	@Override
	public void populateDisplayColumns(String datatype,
			DataSourceCategory category, List<Attribute> attributes) 
	{
		String query = "CALL populate_display_columns(?,?,?,?)";
		try
		{
			String attributeNames = ServerUtil.prepareAttributeNameArgument(attributes);
			getSimpleJdbcTemplate().update(query, datatype, category.toString(),
									ServerUtil.DELIMITER, attributeNames);
		}
		catch (BadSqlGrammarException e)
		{
			s_Logger.error("Could not set the GUI display columns.", e);
		}
	}

}
