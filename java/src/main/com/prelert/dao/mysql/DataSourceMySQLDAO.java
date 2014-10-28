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

package com.prelert.dao.mysql;

import java.sql.ResultSet;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcDaoSupport;

import com.prelert.dao.DataSourceDAO;
import com.prelert.dao.spring.MetricTreeNodeRowMapper;
import com.prelert.data.DataSource;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;
import com.prelert.data.MetricTreeNode;

/**
 * Implementation for a MySQL database of the DataSourceDAO interface which 
 * predominantly uses calls to stored procedures to obtain information on
 * Prelert data sources.
 * @author Pete Harverson
 */
public class DataSourceMySQLDAO extends SimpleJdbcDaoSupport implements DataSourceDAO
{
	static Logger s_Logger = Logger.getLogger(DataSourceMySQLDAO.class);
	

	@Override
	public List<DataSourceType> getDataSourceTypes()
	{
		String query = "CALL data_types()";
		s_Logger.debug("getDataSourceTypes() query: " + query);
		
		RowMapper<DataSourceType> mapper = new RowMapper<DataSourceType>(){

			@Override
            public DataSourceType mapRow(ResultSet rs, int rowNum) throws SQLException
            {
				DataSourceType sourceType = null;
				
				String typeName = rs.getString("type");
				String categoryStr = rs.getString("category");
				int count = rs.getInt("count");
				
				try
				{
					DataSourceCategory category = DataSourceCategory.getValue(categoryStr);
					sourceType = new DataSourceType(typeName, category);
					sourceType.setCount(count);
					s_Logger.debug("getDataSourceTypes() loaded: " + sourceType);
				}
				catch (Exception e)
				{
					s_Logger.error("Error extracting data source type " +
							"from result set for type " + typeName, e);
				}
				
				return sourceType;
            }
		};
		
		return getSimpleJdbcTemplate().query(query, mapper);
	}
	
	
	/**
	 * Returns a list of all sources for a specified source type, ordered by 
	 * source name e.g. a list of p2ps servers supplying service usage information.
	 * @param dataSourceType the source type for which to return the list of sources.
	 * @return the complete list of sources for the given data source type.
	 */
	public List<DataSource> getDataSources(DataSourceType dataSourceType)
	{
		String query = "CALL data_sources_by_name(?, ?)";
		s_Logger.debug("getSources() query: " + query);
		
		final DataSourceType dsType = dataSourceType;
		
		RowMapper<DataSource> mapper = new RowMapper<DataSource>(){

			@Override
            public DataSource mapRow(ResultSet rs, int rowNum) throws SQLException
            {
				DataSource dataSource = new DataSource();
				dataSource.setDataSourceType(dsType);
				dataSource.setSource(rs.getString("source"));
				dataSource.setCount(rs.getInt("count"));
				return dataSource;
            }
		};
		
		String dataSourceName = dataSourceType.getName();
		String dbCategory = dataSourceType.getDataCategory().toString();
		
		return getSimpleJdbcTemplate().query(query, mapper, dataSourceName, dbCategory);
	}
	
	
	/**
	 * Returns the complete list of sources from which data has been retrieved
	 * by the Prelert engine.
	 * @return the full list of data sources.
	 */
	public List<DataSource>	getAllDataSources()
	{
		String query = "CALL data_sources_complete()";
		s_Logger.debug("getAllDataSources() query: " + query);
		
		RowMapper<DataSource> mapper = new RowMapper<DataSource>(){

			@Override
            public DataSource mapRow(ResultSet rs, int rowNum) throws SQLException
            {
				DataSource dataSource = new DataSource();
				
				
				DataSourceType sourceType = null;
				
				String typeName = rs.getString("type");
				String categoryStr = rs.getString("category");
				
				try
				{
					DataSourceCategory category = DataSourceCategory.getValue(categoryStr);
					sourceType = new DataSourceType(typeName, category);
				}
				catch (Exception e)
				{
					s_Logger.error("getAllDataSources(): " +
							"Error extracting data source type from result set: ", e);
				}
				
				dataSource.setDataSourceType(sourceType);
				dataSource.setSource(rs.getString("source"));
				dataSource.setCount(rs.getInt("count"));
				
				return dataSource;
            }
		};
		
		return getSimpleJdbcTemplate().query(query, mapper);
	}


	@Override
	public List<MetricTreeNode> getDataSourceTreeNextLevel(String type, String previousPath, 
									String currentValue, Integer opaqueNum,	String opaqueStr)
	{
		String query = "CALL data_source_tree_next_level(?, ?, ?, ?, ?)";

		String debugQuery = "CALL data_source_tree_next_level({0}, {1}, {2}, {3}, {4})";
		debugQuery = MessageFormat.format(debugQuery, type, previousPath, 
				currentValue, opaqueNum, opaqueStr);
		s_Logger.debug("getDataSourceTreeNextLevel() query: " + debugQuery);
		
		return getSimpleJdbcTemplate().query(query, new MetricTreeNodeRowMapper(),
							type, previousPath, currentValue, opaqueNum, opaqueStr);
	}


	@Override
	public List<MetricTreeNode> getDataSourceTreePreviousLevel(String type, String previousPath,
								Integer opaqueNum, String opaqueStr) 
	{	
		String query = "CALL data_source_tree_previous_level(?, ?, ?, ?)";
		
		String debugQuery = "CALL data_source_tree_previous_level({0}, {1}, {2}, {3})";
		debugQuery = MessageFormat.format(debugQuery, type, previousPath, 
				opaqueNum, opaqueStr);
		s_Logger.debug("getDataSourceTreePreviousLevel() query: " + debugQuery);

		return getSimpleJdbcTemplate().query(query, new MetricTreeNodeRowMapper(),
							type, previousPath, opaqueNum, opaqueStr);
	}


	@Override
	public List<MetricTreeNode> getDataSourceTreeCurrentLevel(String type,
							String previousPath, Integer opaqueNum, String opaqueStr) 
	{
		String query = "CALL data_source_tree_current_level(?, ?, ?, ?)";
		
		String debugQuery = "CALL data_source_tree_current_level({0}, {1}, {2}, {3})";
		debugQuery = MessageFormat.format(debugQuery, type, previousPath, 
									opaqueNum, opaqueStr);
		
		s_Logger.debug("getDataSourceTreeCurrentLevel() query: " + debugQuery);

		return getSimpleJdbcTemplate().query(query, new MetricTreeNodeRowMapper(),
									type, previousPath, opaqueNum, opaqueStr);
	}
	
	
	@Override
    public Date getEndTime()
    {
	    // Returns the expiry time of the product license.
    	// NB. Placed in this DAO to hide its purpose from the user.
    	String query = "CALL end_time()";
		return getSimpleJdbcTemplate().queryForObject(
				query, java.sql.Timestamp.class);
    }


	@Override
	public String getCustomerId()
	{
    	String query = "CALL cust_id()";
		return getSimpleJdbcTemplate().queryForObject(
				query, String.class);
	}

}
