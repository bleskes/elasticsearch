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

package demo.app.splash.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.ResultSetExtractor;

import demo.app.dao.SpringJdbcTemplateDAO;
import demo.app.data.DataSourceCategory;
import demo.app.data.DataSourceType;


/**
 * Implementation for a MySQL database of the DataSourceDAO interface which 
 * predominantly uses calls to stored procedures to obtain information on
 * Prelert data sources.
 * @author Pete Harverson
 */
public class DataSourceMySQLDAO extends SpringJdbcTemplateDAO implements DataSourceDAO
{
	static Logger logger = Logger.getLogger(DataSourceMySQLDAO.class);
	

	/**
	 * Returns a list of all the source types from which data has been retrieved
	 * by the Prelert engine e.g. p2ps logs, CPU data, p2psmon user usage data.
	 * @return the complete list of data source types, in ascending alphabetical 
	 * 			order of data source type name
	 */
	public List<DataSourceType> getDataSourceTypes()
	{
		String query = "CALL data_types()";
		logger.debug("getDataSourceTypes() query: " + query);
		
		ParameterizedRowMapper<DataSourceType> mapper = new ParameterizedRowMapper<DataSourceType>(){

			@Override
            public DataSourceType mapRow(ResultSet rs, int rowNum) throws SQLException
            {
				DataSourceType sourceType = null;
				
				String typeName = rs.getString("type");
				String categoryStr = rs.getString("category");
				
				try
				{
					DataSourceCategory category = toDataSourceCategory(categoryStr);
					sourceType = new DataSourceType(typeName, category);
				}
				catch (Exception e)
				{
					logger.error("Error extracting data source type " +
							"from result set for type " + typeName, e);
				}
				
				return sourceType;
            }
		};
		
		List<DataSourceType> dataSourceTypes = m_SimpleJdbcTemplate.query(query, mapper);
		
		// Place in ascending alphabetical order of data source type name.
		Collections.sort(dataSourceTypes, new DataSourceTypeNameComparator());
		
		return dataSourceTypes;
	}
	
	
	/**
	 * Returns a map of the source types against the total number of data points
	 * that have been collected for each type, ordered alphabetically by source type.
	 * @return TreeMap of data source types against the number of 
	 * 			data points collected for each, ordered by source type.
	 */
	public TreeMap<DataSourceType, Integer> getDataSourceTypeCounts()
	{
		String query = "CALL data_types()";
		logger.debug("getDataSourceTypeCounts() query: " + query);
		
		ResultSetExtractor resultExtractor = new ResultSetExtractor(){

			@Override
            public Object extractData(ResultSet rs) throws SQLException,
                    DataAccessException
            {
				TreeMap<DataSourceType, Integer> typeCounts = 
					new TreeMap<DataSourceType, Integer>(new DataSourceTypeNameComparator());
				
				String typeName;
				
				DataSourceType sourceType;
				String categoryStr;
				DataSourceCategory category;
				int count;
				
	            while (rs.next())
	            {
	            	typeName = rs.getString("type");
					categoryStr = rs.getString("category");
					count = rs.getInt("count");
					
					try
					{
						category = toDataSourceCategory(categoryStr);
						sourceType = new DataSourceType(typeName, category);
						typeCounts.put(sourceType, count);
						
						logger.debug("getDataSourceTypeCounts() added: " + sourceType);
					}
					catch (Exception e)
					{
						logger.error("Error extracting data source type " +
								"from result set for type " + typeName, e);
					}
	            }
	            
	            return typeCounts;
            }
			
		};
		
		TreeMap<DataSourceType, Integer> typesMap = 
			(TreeMap<DataSourceType, Integer>) m_SimpleJdbcTemplate.getJdbcOperations().query(query, resultExtractor);
		
		logger.debug("getDataSourceTypeCounts() size of returned TreeMap: " + typesMap.size());
		
		return typesMap;
	}
	
	
	/**
	 * Returns a list of all sources for a specified source type, ordered by 
	 * source name e.g. a list of p2ps servers supplying service usage information.
	 * @param dataSourceType the source type for which to return the list of sources.
	 * @return the complete list of sources for the given data source type.
	 */
	public List<String> getSources(DataSourceType dataSourceType)
	{
		String query = "CALL data_sources_by_name(?, ?)";
		logger.debug("getSources() query: " + query);
		
		ParameterizedRowMapper<String> mapper = new ParameterizedRowMapper<String>(){

			@Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException
            {
				return rs.getString("source");
            }
		};
		
		String dataSourceName = dataSourceType.getName();
		String dbCategory = toDatabaseCategory(dataSourceType.getDataCategory());
		
		return m_SimpleJdbcTemplate.query(query, mapper, dataSourceName, dbCategory);
	}
	
	
	/**
	 * Returns a map of the sources for the specified source type against the
	 * total number of data points that have been collected for each source,
	 * ordered by source name.
	 * @param dataSourceType the source type for which to return the sources.
	 * @return TreeMap of data sources against the number of data points 
	 * 			collected for each, ordered by source name.
	 */
	public TreeMap<String, Integer> getSourceCounts(DataSourceType dataSourceType)
	{
		String query = "CALL data_sources_by_name(?, ?)";
		logger.debug("getSourceCounts() query: " + query);
		
		ResultSetExtractor resultExtractor = new ResultSetExtractor(){

			@Override
            public Object extractData(ResultSet rs) throws SQLException,
                    DataAccessException
            {
				TreeMap<String, Integer> sourceCounts = new TreeMap<String, Integer>();
				
				String source;
				int count;
				
	            while (rs.next())
	            {
	            	source = rs.getString("source");
					count = rs.getInt("count");
					
					sourceCounts.put(source, count);
	            }
	            
	            return sourceCounts;
            }
			
		};
		
		String dataSourceName = dataSourceType.getName();
		String dbCategory = toDatabaseCategory(dataSourceType.getDataCategory());
		
		String[] args = {dataSourceName, dbCategory};
		TreeMap<String, Integer> sourceCountsMap = 
			(TreeMap<String, Integer>) m_SimpleJdbcTemplate.getJdbcOperations().query(
					query, args, resultExtractor);
		
		return sourceCountsMap;
	}
	
	
	/**
	 * Converts the value of the category column returned by the database
	 * to the appropriate DataSourceCategory enum.
	 * @param dbCategory	the value returned by the database e.g. 'time series'.
	 * @return DataSourceCategory enum, or <code>null</code> if the value could
	 * 			was not successfully converted to a DataSourceCategory.
	 * @throws IllegalArgumentException if there is no DataSourceCategory matching
	 * 			the database value.
     * @throws NullPointerException if <tt>dbCategory</tt> is null.
	 */
	protected DataSourceCategory toDataSourceCategory(String dbCategory)
	{
		String categoryStr = dbCategory.replace(' ', '_').toUpperCase();
		return Enum.valueOf(DataSourceCategory.class, categoryStr);
	}
	
	
	/**
	 * Converts the given DataSourceCategory enum to the String value used by the
	 * database to represent the data source category.
	 * @param category	the DataSourceCategory enum.
	 * @return the value used by the database e.g. 'time series'.
	 */
	protected String toDatabaseCategory(DataSourceCategory category)
	{
		String categoryStr = "";
		
		if (category != null)
		{
			categoryStr = category.toString().replace('_', ' ').toLowerCase();
		}
		
		return categoryStr;
	}
	
	
    /**
     * Comparator which sorts DataSourceType objects in ascending alphabetical
     * order of data source type name.
     */
    class DataSourceTypeNameComparator implements Comparator<DataSourceType>
    {
    	
        public int compare(DataSourceType type1, DataSourceType type2)
        {
	        String name1 = type1.getName();
	        String name2 = type2.getName();
	        
	        int comp;
	        
	        if (name1.equals(name2) == false)
	        {
	        	comp = name1.compareTo(name2);
	        }
	        else
	        {
	        	// e.g. names are the same for TIME_SERIES and TIME_SERIES_FEATURE.
	        	String cat1 = type1.getDataCategory().toString();
	        	String cat2 = type2.getDataCategory().toString();
	        	
	        	comp = cat1.compareTo(cat2);
	        }
	        
	        return comp;
        }
    }

}
