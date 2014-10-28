/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2010     *
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

package com.prelert.dao.postgresql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import org.apache.commons.dbcp.BasicDataSourceFactory;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.RowMapper;

import com.prelert.dao.DataSourceDAO;
import com.prelert.dao.SpringJdbcTemplateDAO;
import com.prelert.data.DataSource;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;


/**
 * Implementation for a PostgreSQL database of the DataSourceDAO interface which 
 * uses calls to functions to obtain information on Prelert data sources.
 * @author Pete Harverson
 */
public class DataSourcePostgreSQLDAO extends SpringJdbcTemplateDAO implements DataSourceDAO
{
	static Logger logger = Logger.getLogger(DataSourcePostgreSQLDAO.class);
	
	
	@Override
	public List<DataSourceType> getDataSourceTypes()
	{
		String query = "select * from data_types()";
		logger.debug("getDataSourceTypes() query: " + query);
		
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
					logger.debug("getDataSourceTypes() loaded: " + sourceType);
				}
				catch (Exception e)
				{
					logger.error("Error extracting data source type from result set for type " + 
							typeName, e);
				}
				
				return sourceType;
            }
		};
		
		return m_SimpleJdbcTemplate.query(query, mapper);
	}


	@Override
	public List<DataSource> getDataSources(DataSourceType dataSourceType)
	{
		String query = "select * from data_sources_by_name(?, ?)";
		logger.debug("getDataSources() query: " + query);
		
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
		
		return m_SimpleJdbcTemplate.query(query, mapper, dataSourceName, dbCategory);
	}
	
	
	@Override
	public List<DataSource> getAllDataSources()
	{
		String query = "select * from data_sources_complete()";
		logger.debug("getAllDataSources() query: " + query);
		
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
					logger.error("getAllDataSources(): " +
							"Error extracting data source type from result set: ", e);
				}
				
				dataSource.setDataSourceType(sourceType);
				dataSource.setSource(rs.getString("source"));
				dataSource.setCount(rs.getInt("count"));
				
				return dataSource;
            }
		};
		
		return m_SimpleJdbcTemplate.query(query, mapper);
	}
	
	
	public static void main(String[] args)
	{
		// main() method for standalone testing.
		
		DataSourcePostgreSQLDAO dataSourceDAO = new DataSourcePostgreSQLDAO();
		
		// Initialise connection.
		Properties connectionProps = new Properties();
		connectionProps.setProperty("driverClassName", "org.postgresql.Driver");
		connectionProps.setProperty("url", "jdbc:postgresql://localhost:5432/statestreet");
		connectionProps.setProperty("username", "postgres");
		connectionProps.setProperty("password", "root123");
		connectionProps.setProperty("defaultAutoCommit", "false");
		
		
        javax.sql.DataSource dataSource = null;
        try
        {
	        dataSource = BasicDataSourceFactory.createDataSource(connectionProps);
	        logger.debug("Initialised PostgreSQL datasource");
	        
	        dataSourceDAO.setDataSource(dataSource);
	        
	        dataSourceDAO.getDataSourceTypes();
	        
	        dataSourceDAO.getAllDataSources();
        }
        catch (Exception e)
        {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
       
	}

}
