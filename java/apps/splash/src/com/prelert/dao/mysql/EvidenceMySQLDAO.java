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

package com.prelert.dao.mysql;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;

import com.prelert.dao.*;
import com.prelert.data.TimeFrame;
import com.prelert.data.gxt.AttributeModel;
import com.prelert.data.gxt.EvidenceModel;
import com.prelert.server.ServerUtil;


/**
 * Implementation for a MySQL database of the EvidenceViewDAO interface which 
 * predominantly uses calls to stored procedures to obtain information for 
 * Evidence list views.
 * @author Pete Harverson
 */
public class EvidenceMySQLDAO extends SpringJdbcTemplateDAO implements EvidenceDAO
{
	static Logger logger = Logger.getLogger(EvidenceMySQLDAO.class);
	
	private int m_PageSize = 20;
	
	
	/**
	 * Returns a list of all of the column names for evidence data with the
	 * specified data type for display in a view with the given time frame.
	 * @param dataType identifier for the type of evidence data.
	 * @param timeFrame time frame for which to obtain the list of column names.
	 * @return List of column names for the given Evidence View.
	 */
	public List<String> getAllColumns(String dataType, TimeFrame timeFrame)
	{
    	String qry = "";
    	switch (timeFrame)
    	{
    		case ALL:
    		case WEEK:
    			qry = "desc evidence_week_view";
    			break;
    			
    		case DAY:
    			qry = "desc evidence_day_view";
    			break;
    			
    		case HOUR:
    			qry = "desc evidence_hour_view";
    			break;
    			
    		case MINUTE:
    			qry = "desc evidence_minute_view";
    			break;
    			
    		case SECOND:
    		default:
    			// PROC is: call display_columns(type, getCompulsory, getOptional)
    			qry = "call display_columns(?, 1, 1);";
    			break;
    	}
		
		List<String> allColumns;
		RowMapper<String> mapper = new RowMapper<String>(){
			
			@Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException
            {				
				return rs.getString(1);
            }
		};
		
		if (timeFrame == TimeFrame.SECOND)
		{
			allColumns = m_SimpleJdbcTemplate.query(qry, mapper, dataType);
		}
		else
		{
			allColumns = m_SimpleJdbcTemplate.query(qry, mapper);
		}
		
		return allColumns;
	}
	
	
	/**
	 * Returns a list of the names of the attributes that support filtering.
	 * @param dataType identifier for the type of evidence data.
	 * @param getCompulsory <code>true</code> to return compulsory columns, 
	 * 		<code>false</code> otherwise.
	 * @param getOptional <code>true</code> to return optional columns, 
	 * 		<code>false</code> otherwise.
	 * @return a list of the filterable attributes.
	 */
	public List<String> getFilterableColumns(String dataType, boolean getCompulsory, boolean getOptional)
	{
		// PROC is: call filterable_columns(dataType, getCompulsory, getOptional)
		// Compulsory/Optional parameters not yet implemented 
		// Non-zero value: true
		String query = "call filterable_columns(?, ?, ?)";		
	
		return m_SimpleJdbcTemplate.query(query.toString(), 
				new SingleColumnRowMapper<String>(), dataType, getCompulsory, getOptional);
	}
	
	
	/**
	 * Returns the list of values in the evidence table for the specified column.
	 * @param dataType identifier for the type of evidence data.
	 * @param columnName name of the column for which to return the values.
	 * @param maxRows maximum number of values to return.
	 * @return list of all the distinct values in the evidence table for the
	 * 			given column.
	 */
	public List<String> getColumnValues(String dataType, String columnName, int maxRows)
	{
		// PROC is: call evidence_distinct_attribute(dataType, attribute, maxRows)
		String query = "CALL evidence_distinct_attribute(?, ?, ?)";	
		
		String debugQuery = "CALL evidence_distinct_attribute({0}, {1}, {2})";
		debugQuery = MessageFormat.format(debugQuery, dataType, columnName, maxRows);
		logger.debug("getColumnValues() query: " + debugQuery);
		
		return m_SimpleJdbcTemplate.query(query.toString(), 
			new SingleColumnRowMapper<String>(), dataType, columnName, maxRows);
	}
	
	
	@Override
	public List<EvidenceModel> getFirstPage(String dataType, String source,
			TimeFrame timeFrame, List<String> filterAttributes, List<String> filterValues)
	{
		List<EvidenceModel> evidenceList = null;
		String query = "";
		
		String filterAttrsArg = ServerUtil.prepareFilterArgument(filterAttributes);
		String filterValsArg = ServerUtil.prepareFilterArgument(filterValues);
		
		switch (timeFrame)
		{
			case ALL:
			case WEEK:
			case DAY:
			case HOUR:
			case MINUTE:
				// Query calls a stored procedure in the form:
		    	// CALL evidence_drill_down_first_page('apache_logs', 'lon-server1', 
				// 'minute', 'service has shutdown', 20)
				query = "CALL evidence_drill_down_first_page(?, ?, ?, ?, ?)";
				logger.debug("getFirstPage() query: " + query);
				evidenceList = m_SimpleJdbcTemplate.query(query, new EvidenceModelRowMapper(),
						dataType, source, timeFrame.toString().toLowerCase(), filterValsArg, m_PageSize);
				break;
					
			case SECOND:
				query = "CALL evidence_first_page(?, ?, ?, ?, ?)";
				
				String debugQuery = "CALL evidence_first_page({0}, {1}, {2}, {3}, {4})";
				debugQuery = MessageFormat.format(debugQuery, dataType, source, 
						filterAttrsArg, filterValsArg, m_PageSize);
				logger.debug("getFirstPage() query: " + debugQuery);
				
				evidenceList = m_SimpleJdbcTemplate.query(query, new EvidenceModelRowMapper(),
						dataType, source, filterAttrsArg, filterValsArg, m_PageSize);
				break;
		}
		
		return evidenceList;
	}
	
	
	@Override
	public List<EvidenceModel> getLastPage(String dataType, String source, 
			TimeFrame timeFrame, List<String> filterAttributes, List<String> filterValues)
	{	
		List<EvidenceModel> evidenceList = null;
		String query = "";
		
		String filterAttrsArg = ServerUtil.prepareFilterArgument(filterAttributes);
		String filterValsArg = ServerUtil.prepareFilterArgument(filterValues);
		
		switch (timeFrame)
		{
			case ALL:
			case WEEK:
			case DAY:
			case HOUR:
			case MINUTE:
				// Query calls a stored procedure in the form:
				// CALL evidence_drill_down_last_page('apache_logs', 'lon-server1', 
				// 'minute', 'service has shutdown', 20)
				query = "CALL evidence_drill_down_last_page(?, ?, ?, ?, ?)";
				logger.debug("getLastPage() query: " + query);
				evidenceList = m_SimpleJdbcTemplate.query(query, new EvidenceModelRowMapper(), 
						dataType, source, timeFrame.toString().toLowerCase(), filterValsArg, m_PageSize);
				break;
					
			case SECOND:
				query = "CALL evidence_last_page(?, ?, ?, ?, ?)";
				
				String debugQuery = "CALL evidence_last_page({0}, {1}, {2}, {3}, {4})";
				debugQuery = MessageFormat.format(debugQuery, dataType, source, 
						filterAttrsArg, filterValsArg, m_PageSize);
				logger.debug("getLastPage() query: " + debugQuery);
				
				evidenceList = m_SimpleJdbcTemplate.query(query, new EvidenceModelRowMapper(), 
						dataType, source, filterAttrsArg, filterValsArg, m_PageSize);
				break;
		}
		
		return evidenceList;
		
	}
	
	
	@Override
	public List<EvidenceModel> getNextPage(String dataType, String source, 
			TimeFrame timeFrame, Date bottomRowTime, String bottomRowId,
			List<String> filterAttributes, List<String> filterValues)
	{

		List<EvidenceModel> evidenceList = null;
		String query = "";
		Timestamp bottomRowTimeStamp = new Timestamp(bottomRowTime.getTime());
		
		String filterAttrsArg = ServerUtil.prepareFilterArgument(filterAttributes);
		String filterValsArg = ServerUtil.prepareFilterArgument(filterValues);
		
		switch (timeFrame)
		{
			case ALL:
			case WEEK:
			case DAY:
			case HOUR:
			case MINUTE:
				// Query calls a stored procedure in the form:
		    	// CALL evidence_drill_down_next_page('apache_logs', 'lon-webserver1', 
				// 'minute', timeForBottomRow, bottom row id, false, 20)				
				query = "CALL evidence_drill_down_next_page(?, ?, ?, ?, ?, ?, ?)";
				logger.debug("getNextPage() query: " + query);
				boolean filteredView = (filterValsArg != null);
				evidenceList = m_SimpleJdbcTemplate.query(
						query, new EvidenceModelRowMapper(), 
						dataType, source, timeFrame.toString().toLowerCase(), 
						bottomRowTimeStamp, bottomRowId, filteredView, m_PageSize);
				break;
					
			case SECOND:
				query = "CALL evidence_next_page(?, ?, ?, ?, ?, ?, ?)";
				
				String debugQuery = "CALL evidence_next_page({0}, {1}, {2}, {3}, {4}, {5}, {6})";
				debugQuery = MessageFormat.format(debugQuery, dataType, source, bottomRowTimeStamp, 
						Integer.parseInt(bottomRowId), filterAttrsArg, filterValsArg, m_PageSize);
				logger.debug("getNextPage() query: " + debugQuery);
				
				evidenceList = m_SimpleJdbcTemplate.query(
						query, new EvidenceModelRowMapper(), 
						dataType, source, bottomRowTimeStamp, Integer.parseInt(bottomRowId), 
						filterAttrsArg, filterValsArg, m_PageSize);
				break;
		}
		
		return evidenceList;
	}
	
	
	@Override
	public List<EvidenceModel> getPreviousPage(String dataType, String source, 
			TimeFrame timeFrame, Date topRowTime, String topRowId,
			List<String> filterAttributes, List<String> filterValues)
	{
		List<EvidenceModel> evidenceList = null;
		String query = "";
		Timestamp topRowTimeStamp = new Timestamp(topRowTime.getTime());
		
		String filterAttrsArg = ServerUtil.prepareFilterArgument(filterAttributes);
		String filterValsArg = ServerUtil.prepareFilterArgument(filterValues);

		switch (timeFrame)
		{
			case ALL:
			case WEEK:
			case DAY:
			case HOUR:
			case MINUTE:
				// Query calls a stored procedure in the form:
				// CALL evidence_drill_down_previous_page('apache_logs', 'lon-webserver1', 
				// 'minute', timeForTopRow, top row id, false, 20)				
				query = "CALL evidence_drill_down_previous_page(?, ?, ?, ?, ?, ?, ?)";
				logger.debug("getPreviousPage() query: " + query);
				boolean filteredView = (filterValsArg != null);
				evidenceList = m_SimpleJdbcTemplate.query(
						query, new EvidenceModelRowMapper(), 
						dataType, source, timeFrame.toString().toLowerCase(), 
						topRowTimeStamp, topRowId, filteredView, m_PageSize);
				break;
					
			case SECOND:
				query = "CALL evidence_previous_page(?, ?, ?, ?, ?, ?, ?)";
				
				String debugQuery = "CALL evidence_previous_page({0}, {1}, {2}, {3}, {4}, {5}, {6})";
				debugQuery = MessageFormat.format(debugQuery, dataType, source, topRowTimeStamp, 
						Integer.parseInt(topRowId), filterAttrsArg, filterValsArg, m_PageSize);
				logger.debug("getPreviousPage() query: " + debugQuery);
				
				evidenceList = m_SimpleJdbcTemplate.query(query, new EvidenceModelRowMapper(), 
						dataType, source, topRowTimeStamp, Integer.parseInt(topRowId), 
						filterAttrsArg, filterValsArg, m_PageSize);
				break;
		}
		
		return evidenceList;
	}
	
	
	@Override
	public List<EvidenceModel> getForDescription(String dataType, String source, 
			TimeFrame timeFrame, Date time, String description)
	{
		// NB. Need to switch on TimeFrame when other views are added in.
		
		String query = "CALL evidence_at_description_minute(?, ?, ?, ?, ?)";
		logger.debug("getForDescription() query: " + query);
		
		Timestamp timeStamp = new Timestamp(time.getTime());
		
		return m_SimpleJdbcTemplate.query(query, new EvidenceModelRowMapper(), 
				dataType, source, timeStamp, description, m_PageSize);
	}
	
	
	@Override
	public List<EvidenceModel> getAtTime(String dataType, String source,
			TimeFrame timeFrame, Date time, 
			List<String> filterAttributes, List<String> filterValues)
	{
		List<EvidenceModel> evidenceList = null;
		String query = "";
		String debugQuery = "";
		Timestamp timeStamp = new Timestamp(time.getTime());
		
		String filterAttrsArg = ServerUtil.prepareFilterArgument(filterAttributes);
		String filterValsArg = ServerUtil.prepareFilterArgument(filterValues);
		
		switch (timeFrame)
		{
			case ALL:
			case WEEK:
			case DAY:
			case HOUR:
			case MINUTE:
				// Query calls a stored procedure in the form:
		    	// CALL evidence_drill_down_previous_page('apache_logs', 'minute', timeForTopRow, 
				// 'top row description', false)				
				query = "CALL evidence_drill_down_at_time(?, ?, ?, ?, ?, ?)";
				
				debugQuery = "CALL evidence_drill_down_at_time({0}, {1}, {2}, {3}, {4}, {5})";
				debugQuery = MessageFormat.format(
						debugQuery, dataType, source, timeFrame.toString().toLowerCase(), 
						timeStamp, filterValsArg, m_PageSize);
				logger.debug("getAtTime() query: " + debugQuery);
				
				evidenceList = m_SimpleJdbcTemplate.query(query, new EvidenceModelRowMapper(), 
						dataType, source, timeFrame.toString().toLowerCase(), 
						timeStamp, filterValsArg, m_PageSize);
				break;
					
			case SECOND:
				query = "CALL evidence_at_time(?, ?, ?, ?, ?, ?)";
				
				debugQuery = "CALL evidence_at_time({0}, {1}, {2}, {3}, {4}, {5})";
				debugQuery = MessageFormat.format(
						debugQuery, dataType, source, timeStamp, 
						filterAttrsArg, filterValsArg, m_PageSize);
				logger.debug("getAtTime() query: " + debugQuery);
				
				evidenceList = m_SimpleJdbcTemplate.query(query, new EvidenceModelRowMapper(), 
						dataType, source, timeStamp, filterAttrsArg, filterValsArg, m_PageSize);
				break;
		}

		return evidenceList;
	}
	
	
	@Override
	public List<EvidenceModel> getIdPage(String dataType, String source, int id)
	{
		String query = "CALL evidence_id_page(?, ?, ?, ?);";
		
		String debugQuery = "CALL evidence_id_page({0}, {1}, {2}, {3})";
		debugQuery = MessageFormat.format(
				debugQuery, dataType, source, id, m_PageSize);
		logger.debug("getIdPage() query: " + debugQuery);
		
		return m_SimpleJdbcTemplate.query(query, new EvidenceModelRowMapper(), 
				dataType, source, id, m_PageSize);
	}
	
	
	@Override
	public List<EvidenceModel> searchFirstPage(
			String dataType, String source, String containsText)
	{
		// Query calls a stored procedure in the form:
		// CALL evidence_search_first_page(type, source, searchString, pageSize)
		String query = "CALL evidence_search_first_page(?, ?, ?, ?)";
		
		String debugQuery = "CALL evidence_search_first_page({0}, {1}, {2}, {3})";
		debugQuery = MessageFormat.format(debugQuery, dataType, source, 
				containsText, m_PageSize);
		logger.debug("searchFirstPage() query: " + debugQuery);
		
		return m_SimpleJdbcTemplate.query(query, new EvidenceModelRowMapper(),
				dataType, source, containsText, m_PageSize);
	}
	
	
	@Override
	public List<EvidenceModel> searchLastPage(
			String dataType, String source, String containsText)
	{
		// Query calls a stored procedure in the form:
		// CALL evidence_search_last_page(type, source, searchString, pageSize)
		String query = "CALL evidence_search_last_page(?, ?, ?, ?)";
		
		String debugQuery = "CALL evidence_search_last_page({0}, {1}, {2}, {3})";
		debugQuery = MessageFormat.format(debugQuery, dataType, source, 
				containsText, m_PageSize);
		logger.debug("searchLastPage() query: " + debugQuery);
		
		return m_SimpleJdbcTemplate.query(query, new EvidenceModelRowMapper(),
				dataType, source, containsText, m_PageSize);
	}
	
	
	@Override
	public List<EvidenceModel> searchNextPage(
			String dataType, String source, Date bottomRowTime, 
			int bottomRowId, String containsText)
	{
		// Query calls a stored procedure in the form:
		// evidence_search_next_page(type, source, time, id, searchString, pageSize)
		String query = "CALL evidence_search_next_page(?, ?, ?, ?, ?, ?)";
		
		String debugQuery = "CALL evidence_search_next_page({0}, {1}, {2}, {3}, {4}, {5})";
		debugQuery = MessageFormat.format(debugQuery, dataType, source, 
				bottomRowTime, bottomRowId, containsText, m_PageSize);
		logger.debug("searchNextPage() query: " + debugQuery);
		
		return m_SimpleJdbcTemplate.query(query, new EvidenceModelRowMapper(),
				dataType, source, bottomRowTime, bottomRowId, containsText, m_PageSize);
	}
	
	
	@Override
	public List<EvidenceModel> searchPreviousPage(
			String dataType, String source, Date topRowTime, 
			int topRowId, String containsText)
	{
		// Query calls a stored procedure in the form:
		// evidence_search_previous_page(type, source, time, id, searchString, pageSize)
		String query = "CALL evidence_search_previous_page(?, ?, ?, ?, ?, ?)";
		
		String debugQuery = "CALL evidence_search_previous_page({0}, {1}, {2}, {3}, {4}, {5})";
		debugQuery = MessageFormat.format(debugQuery, dataType, source, 
				topRowTime, topRowId, containsText, m_PageSize);
		logger.debug("searchPreviousPage() query: " + debugQuery);
		
		return m_SimpleJdbcTemplate.query(query, new EvidenceModelRowMapper(),
				dataType, source, topRowTime, topRowId, containsText, m_PageSize);

	}
	
	
	@Override
	public List<EvidenceModel> searchAtTime(String dataType, String source, 
			Date time, String containsText)
	{
		// Query calls a stored procedure in the form:
		// evidence_search_at_time(type, source, time, searchString, pageSize)
		String query = "CALL evidence_search_at_time(?, ?, ?, ?, ?)";
		
		String debugQuery = "CALL evidence_search_at_time({0}, {1}, {2}, {3}, {4})";
		debugQuery = MessageFormat.format(debugQuery, dataType, source, 
				time, containsText, m_PageSize);
		logger.debug("searchAtTime() query: " + debugQuery);
		
		return m_SimpleJdbcTemplate.query(query, new EvidenceModelRowMapper(),
				dataType, source, time, containsText, m_PageSize);
	}
	
	
	/**
	 * Returns data for a single item of evidence with the specified id.
	 * @param id the unique identifier for the item of evidence.
	 * @return full data model for the evidence item with the specified id.
	 */
	public EvidenceModel getEvidenceSingle(int id)
	{
		String query = "CALL evidence_single_complete(?);";
		
		return m_SimpleJdbcTemplate.queryForObject(
				query, new EvidenceModelRowMapper(), id);
	}
	
	
	/**
	 * Returns all attributes for the item of evidence with the specified id 
  	 * (instead of just the current display columns).
	 * @param id the value of the id column for the evidence to obtain information on.
	 * @return List of AttributeModel objects for the row with the specified id.
	 * 		Note that values for time fields are transported as a String representation
	 * 		of the number of milliseconds since January 1, 1970, 00:00:00 GMT.
	 */
	public List<AttributeModel> getEvidenceAttributes(int id)
	{
		String query = "CALL evidence_single_complete(?)";

		return m_SimpleJdbcTemplate.queryForObject(
				query, new EvidenceAttributeRowMapper(), id);
	}
	
	
	/**
	 * Returns the earliest evidence record in the Prelert database for the given
	 * properties.
	 * @param dataType identifier for the type of evidence data.
	 * @param source name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 * @param filterAttributes optional attribute names on which to filter the query.
	 * @param filterValues optional attribute values on which to filter the query.
	 * @return earliest evidence record for the specified properties. Note that the
	 * 	returned evidence will only contain id and time fields.
	 */
	@Override
	public EvidenceModel getEarliestEvidence(String dataType, String source,
			List<String> filterAttributes, List<String> filterValues)
	{		
		// NB. Does not use TimeFrame - all views will use evidence_second_view.
		
		String filterAttribute = ServerUtil.prepareFilterArgument(filterAttributes);
		String filterValue = ServerUtil.prepareFilterArgument(filterValues);
		
		String query = "CALL evidence_min_time(?, ?, ?, ?)";
		return m_SimpleJdbcTemplate.queryForObject(query.toString(), 
				new EvidenceModelRowMapper(), dataType, source, filterAttribute, filterValue);
	}

	
	/**
	 * Returns the latest evidence record in the Prelert database for the given
	 * properties.
	 * @param dataType identifier for the type of evidence data.
	 * @param source name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 * @param filterAttributes optional attribute names on which to filter the query.
	 * @param filterValues optional attribute values on which to filter the query.
	 * @return latest evidence record for the specified properties. Note that the
	 * 	returned evidence will only contain id and time fields.
	 */
	@Override
	public EvidenceModel getLatestEvidence(String dataType, String source,
			List<String> filterAttributes, List<String> filterValues)
	{
		// NB. Does not use TimeFrame - all views will use evidence_second_view.
		String filterAttribute = ServerUtil.prepareFilterArgument(filterAttributes);
		String filterValue = ServerUtil.prepareFilterArgument(filterValues);
		
		String query = "CALL evidence_max_time(?, ?, ?, ?)";		
		return m_SimpleJdbcTemplate.queryForObject(query.toString(), 
				new EvidenceModelRowMapper(), dataType, source, filterAttribute, filterValue);
	}
	
	
	/**
	 * Runs a search to return the earliest evidence record where the 
	 * specified text is contained within one or more of the evidence attributes.
	 * @param dataType identifier for the type of evidence data.
	 * @param source name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 * @param containsText String to search for within the evidence attributes.
	 * @return earliest evidence record containing specified text. Note that the
	 * 	returned evidence will only contain id and time fields.
	 */
	@Override
	public EvidenceModel searchEarliestEvidence(String dataType, String source, String containsText)
	{		
		String query = "CALL evidence_search_min_time(?, ?, ?)";
		return m_SimpleJdbcTemplate.queryForObject(query.toString(), 
				new EvidenceModelRowMapper(), dataType, source, containsText);
	}

	
	/**
	 * Runs a search to return the latest evidence record where the 
	 * specified text is contained within one or more of the evidence attributes.
	 * @param dataType identifier for the type of evidence data.
	 * @param source name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 * @param containsText String to search for within the evidence attributes.
	 * @return latest evidence record containing specified text. Note that the
	 * 	returned evidence will only contain id and time fields.
	 */
	@Override
	public EvidenceModel searchLatestEvidence(String dataType, String source, String containsText)
	{
		String query = "CALL evidence_search_max_time(?, ?, ?)";
		return m_SimpleJdbcTemplate.queryForObject(query.toString(), 
				new EvidenceModelRowMapper(), dataType, source, containsText);
	}
	
	
	/**
	 * Returns the page size being used for queries.
	 * @return the page size.
	 */
	public int getPageSize()
	{
		return m_PageSize;
	}
	
	
	/**
	 * Sets the page size to be used for queries.
	 * @param pageSize the page size.
	 */
	public void setPageSize(int pageSize)
	{
		m_PageSize = pageSize;
	}
	
	
	/**
	 * ParameterizedRowMapper class for mapping a single evidence query result 
	 * set to a list of AttributeModel objects. Attribute values for time fields
	 * are converted to a String representation of the number of milliseconds 
	 * since January 1, 1970, 00:00:00 GMT.
	 * 
	 * @author Pete Harverson
	 */
	class EvidenceAttributeRowMapper implements RowMapper<List<AttributeModel>>
	{
		private String m_TimeColumnName = EvidenceModel.getTimeColumnName(TimeFrame.SECOND);

		@Override
	    public List<AttributeModel> mapRow(ResultSet rs, int rowNum) throws SQLException
	    {
			ArrayList<AttributeModel> rowData = new ArrayList<AttributeModel>();
			
			ResultSetMetaData metaData = rs.getMetaData();

			String columnName;
			String columnValue;
			for (int i = 1; i <= metaData.getColumnCount(); i++)
			{
				columnName = metaData.getColumnLabel(i);
				
				if (columnName.equals(m_TimeColumnName) == true)
				{
					// Convert time value to a Long so that formatting can be
					// done client side.
					Date evidenceTime = rs.getTimestamp(m_TimeColumnName);
					columnValue = Long.toString(evidenceTime.getTime());
				}
				else
				{
					columnValue = rs.getString(i);
				}
				
				rowData.add(new AttributeModel(columnName, columnValue));
				
			}

	        return rowData;
	    }
	}

}
