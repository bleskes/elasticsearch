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

package demo.app.dao;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.ParameterizedSingleColumnRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import demo.app.data.TimeFrame;
import demo.app.data.gxt.EvidenceModel;
import demo.app.data.gxt.GridRowInfo;


/**
 * Implementation for a MySQL database of the EvidenceViewDAO interface which 
 * predominantly uses calls to stored procedures to obtain information for 
 * Evidence list views.
 * @author Pete Harverson
 */
public class EvidenceMySQLDAO implements EvidenceDAO
{
	static Logger logger = Logger.getLogger(EvidenceMySQLDAO.class);
	
	private SimpleJdbcTemplate	m_SimpleJdbcTemplate;
	
	private int m_PageSize = 20;
	
	
	/**
	 * Sets the data source to be used for connections to the Prelert database.
	 * @param dataSource
	 */
	public void setDataSource(DataSource dataSource)
	{
		m_SimpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}
	
	
	
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
    	
		logger.debug("getAllColumns() query: " + qry);
		
		List<String> allColumns;
		ParameterizedRowMapper<String> mapper = new ParameterizedRowMapper<String>(){
			
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
		
		logger.debug("getFilterableColumns() query: " + query);
	
		return m_SimpleJdbcTemplate.query(query.toString(), 
				new ParameterizedSingleColumnRowMapper<String>(), dataType, getCompulsory, getOptional);
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
			new ParameterizedSingleColumnRowMapper<String>(), dataType, columnName, maxRows);
	}
	
	
	/**
	 * Returns the first page of evidence data for a view with the specified
	 * time frame.
	 * @param dataType identifier for the type of evidence data.
	 * @param source name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 * @param timeFrame time frame for which to obtain the first page of records.
	 * @param filterAttribute optional attribute name on which to filter the query.
	 * @param filterValue optional attribute value on which to filter the query.
	 * @return List of Evidence records comprising the first page of data.
	 */
	public List<EvidenceModel> getFirstPage(String dataType, String source,
			TimeFrame timeFrame, String filterAttribute, String filterValue)
	{
		List<EvidenceModel> evidenceList = null;
		String query = "";
		
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
				evidenceList = m_SimpleJdbcTemplate.query(query, new EventRecordRowMapper(),
						dataType, source, timeFrame.toString().toLowerCase(), filterValue, m_PageSize);
				break;
					
			case SECOND:
				query = "CALL evidence_first_page(?, ?, ?, ?, ?)";
				
				String debugQuery = "CALL evidence_first_page({0}, {1}, {2}, {3}, {4})";
				debugQuery = MessageFormat.format(debugQuery, dataType, source, 
						filterAttribute, filterValue, m_PageSize);
				logger.debug("getFirstPage() query: " + debugQuery);
				
				evidenceList = m_SimpleJdbcTemplate.query(query, new EventRecordRowMapper(),
						dataType, source, filterAttribute, filterValue, m_PageSize);
				break;
		}
		
		logger.debug("getFirstPage() returning " + evidenceList.size());
		return evidenceList;
	}
	
	
	/**
	 * Returns the last page of evidence data for a view with the specified
	 * time frame.
	 * @param dataType identifier for the type of evidence data.
	 * @param source name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 * @param timeFrame time frame for which to obtain the last page of records.
	 * @param filterAttribute optional attribute name on which to filter the query.
	 * @param filterValue optional attribute value on which to filter the query.
	 * @return List of Evidence records comprising the last page of data.
	 */
	public List<EvidenceModel> getLastPage(String dataType, String source, 
			TimeFrame timeFrame, String filterAttribute, String filterValue)
	{	
		List<EvidenceModel> evidenceList = null;
		String query = "";
		
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
				evidenceList = m_SimpleJdbcTemplate.query(query, new EventRecordRowMapper(), 
						dataType, source, timeFrame.toString().toLowerCase(), filterValue, m_PageSize);
				break;
					
			case SECOND:
				query = "CALL evidence_last_page(?, ?, ?, ?, ?)";
				
				String debugQuery = "CALL evidence_last_page({0}, {1}, {2}, {3}, {4})";
				debugQuery = MessageFormat.format(debugQuery, dataType, source, 
						filterAttribute, filterValue, m_PageSize);
				logger.debug("getLastPage() query: " + debugQuery);
				
				evidenceList = m_SimpleJdbcTemplate.query(query, new EventRecordRowMapper(), 
						dataType, source, filterAttribute, filterValue, m_PageSize);
				break;
		}
		
		return evidenceList;
		
	}
	
	
	/**
	 * Returns the next page of evidence data following on from the row with the
	 * specified time and id.
	 * @param dataType identifier for the type of evidence data.
	 * @param source name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 * @param timeFrame time frame for which to obtain the next page of records.
	 * @param bottomRowTime the value of the time column for the bottom row of 
	 * evidence in the current page.
	 * @param bottomRowId the value of the id column for the bottom row of evidence 
	 * in the current page.
	 * @param filterAttribute optional attribute name on which to filter the query.
	 * @param filterValue optional attribute value on which to filter the query.
	 * @return List of Evidence records comprising the next page of data.
	 */
	public List<EvidenceModel> getNextPage(String dataType, String source, 
			TimeFrame timeFrame, Date bottomRowTime, String bottomRowId,
			String filterAttribute, String filterValue)
	{

		List<EvidenceModel> evidenceList = null;
		String query = "";
		Timestamp bottomRowTimeStamp = new Timestamp(bottomRowTime.getTime());
		
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
				boolean filteredView = (filterValue != null);
				evidenceList = m_SimpleJdbcTemplate.query(
						query, new EventRecordRowMapper(), 
						dataType, source, timeFrame.toString().toLowerCase(), 
						bottomRowTimeStamp, bottomRowId, filteredView, m_PageSize);
				break;
					
			case SECOND:
				query = "CALL evidence_next_page(?, ?, ?, ?, ?, ?, ?)";
				
				String debugQuery = "CALL evidence_next_page({0}, {1}, {2}, {3}, {4}, {5}, {6})";
				debugQuery = MessageFormat.format(debugQuery, dataType, source, bottomRowTimeStamp, 
						Integer.parseInt(bottomRowId), filterAttribute, filterValue, m_PageSize);
				logger.debug("getNextPage() query: " + debugQuery);
				
				evidenceList = m_SimpleJdbcTemplate.query(
						query, new EventRecordRowMapper(), 
						dataType, source, bottomRowTimeStamp, Integer.parseInt(bottomRowId), 
						filterAttribute, filterValue, m_PageSize);
				break;
		}
		
		return evidenceList;
	}
	
	
	/**
	 * Returns the previous page of evidence data to the row with the specified 
	 * time and id.
	 * @param dataType identifier for the type of evidence data.
	 * @param source name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 * @param timeFrame time frame for which to obtain the previous page of records.
	 * @param topRowTime the value of the time column for the top row of 
	 * evidence in the current page.
	 * @param topRowId the value of the id column for the top row of evidence 
	 * in the current page.
	 * @param filterAttribute optional attribute name on which to filter the query.
	 * @param filterValue optional attribute value on which to filter the query.
	 * @return List of Evidence records comprising the previous page of data.
	 */
	public List<EvidenceModel> getPreviousPage(String dataType, String source, 
			TimeFrame timeFrame, Date topRowTime, String topRowId,
			String filterAttribute, String filterValue)
	{
		List<EvidenceModel> evidenceList = null;
		String query = "";
		Timestamp topRowTimeStamp = new Timestamp(topRowTime.getTime());

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
				boolean filteredView = (filterValue != null);
				evidenceList = m_SimpleJdbcTemplate.query(
						query, new EventRecordRowMapper(), 
						dataType, source, timeFrame.toString().toLowerCase(), 
						topRowTimeStamp, topRowId, filteredView, m_PageSize);
				break;
					
			case SECOND:
				query = "CALL evidence_previous_page(?, ?, ?, ?, ?, ?, ?)";
				
				String debugQuery = "CALL evidence_previous_page({0}, {1}, {2}, {3}, {4}, {5}, {6})";
				debugQuery = MessageFormat.format(debugQuery, dataType, source, topRowTimeStamp, 
						Integer.parseInt(topRowId), filterAttribute, filterValue, m_PageSize);
				logger.debug("getPreviousPage() query: " + debugQuery);
				
				evidenceList = m_SimpleJdbcTemplate.query(query, new EventRecordRowMapper(), 
						dataType, source, topRowTimeStamp, Integer.parseInt(topRowId), 
						filterAttribute, filterValue, m_PageSize);
				break;
		}
		
		return evidenceList;
	}
	
	
	/**
	 * Returns a page of evidence data, the top row of which matches the specified
	 * description and time.
	 * @param dataType identifier for the type of evidence data.
	 * @param source name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 * @param timeFrame time frame for which to obtain evidence records.
	 * @param time value of the time column to match on for the first row of 
	 * evidence data that will be returned.
	 * @param description the value of the description column to match on for the
	 * top row of evidence data that will be returned.
	 * @return List of Evidence records, sorted descending on time.
	 */
	public List<EvidenceModel> getForDescription(String dataType, String source, 
			TimeFrame timeFrame, Date time, String description)
	{
		// NB. Need to switch on TimeFrame when other views are added in.
		
		String query = "CALL evidence_at_description_minute(?, ?, ?, ?, ?)";
		logger.debug("getForDescription() query: " + query);
		
		Timestamp timeStamp = new Timestamp(time.getTime());
		
		return m_SimpleJdbcTemplate.query(query, new EventRecordRowMapper(), 
				dataType, source, timeStamp, description, m_PageSize);
	}
	
	
	/**
	 * Returns a page of evidence data, whose top row will match the supplied time.
	 * @param dataType identifier for the type of evidence data.
	 * @param source name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 * @param timeFrame time frame for which to obtain evidence records.
	 * @param time value of the time column to match on for the first row of 
	 * evidence data that will be returned.
	 * @param filterAttribute optional attribute name on which to filter the query.
	 * @param filterValue optional attribute value on which to filter the query.
	 * @return List of Evidence records, sorted descending on time.
	 */
	public List<EvidenceModel> getAtTime(String dataType, String source,
			TimeFrame timeFrame, Date time, String filterAttribute, String filterValue)
	{
		List<EvidenceModel> evidenceList = null;
		String query = "";
		String debugQuery = "";
		Timestamp timeStamp = new Timestamp(time.getTime());
		
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
						timeStamp, filterValue, m_PageSize);
				logger.debug("getAtTime() query: " + debugQuery);
				
				evidenceList = m_SimpleJdbcTemplate.query(query, new EventRecordRowMapper(), 
						dataType, source, timeFrame.toString().toLowerCase(), 
						timeStamp, filterValue, m_PageSize);
				break;
					
			case SECOND:
				query = "CALL evidence_at_time(?, ?, ?, ?, ?, ?)";
				
				debugQuery = "CALL evidence_at_time({0}, {1}, {2}, {3}, {4}, {5})";
				debugQuery = MessageFormat.format(
						debugQuery, dataType, source, timeStamp, 
						filterAttribute, filterValue, m_PageSize);
				logger.debug("getAtTime() query: " + debugQuery);
				
				evidenceList = m_SimpleJdbcTemplate.query(query, new EventRecordRowMapper(), 
						dataType, source, timeStamp, filterAttribute, filterValue, m_PageSize);
				break;
		}

		return evidenceList;
	}
	
	
	/**
	 * Returns a page of real-time (SECOND time frame) evidence data, 
	 * the top row of which matches the specified id.
	 * @param dataType identifier for the type of evidence data.
	 * @param source name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 * @param id evidence id for the top row of evidence data that will be returned.
	 * @return List of Evidence records, sorted descending on time.
	 */
	public List<EvidenceModel> getIdPage(String dataType, String source, int id)
	{
		String query = "CALL evidence_id_page(?, ?, ?, ?);";
		logger.debug("getIdPage() query: " + query);
		
		return m_SimpleJdbcTemplate.query(query, new EventRecordRowMapper(), 
				dataType, source, id, m_PageSize);
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
				query, new EventRecordRowMapper(), id);
	}
	
	
	/**
	 * Returns all attributes for the row of evidence with the specified id (instead of just
  	 * the current display columns).
	 * @param id the value of the id column for the item of evidence to obtain information on.
	 * @return List of GridRowInfo objects for the row with the specified id.
	 */
	public List<GridRowInfo> getRowInfo(int rowId)
	{
		String query = "CALL evidence_single_complete(?)";
		logger.debug("getRowInfo() query: " + query);

		ParameterizedRowMapper<List<GridRowInfo>> mapper = new ParameterizedRowMapper<List<GridRowInfo>>(){

			@Override
            public List<GridRowInfo> mapRow(ResultSet rs, int rowNum) throws SQLException
            {
				ArrayList<GridRowInfo> rowData = new ArrayList<GridRowInfo>();
				
				ResultSetMetaData metaData = rs.getMetaData();

				String columnName;
				String columnValue;
				for (int i = 1; i <= metaData.getColumnCount(); i++)
				{
					columnName = metaData.getColumnLabel(i);
					columnValue = rs.getString(i);
					rowData.add(new GridRowInfo(columnName, columnValue));
				}

	            return rowData;
            }
		};
		
		return m_SimpleJdbcTemplate.queryForObject(query, mapper, rowId);
	}
	
	
	/**
	 * Returns the date of the earliest evidence record in the Prelert database
	 * for the given time frame.
	 * @param dataType identifier for the type of evidence data.
	 * @param source name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 * @param filterAttribute attribute name on which to filter on, or
	 * <code>null</code> if no WHERE clause should be applied.
	 * @param filterValue attribute value to use as the filter in a WHERE clause.
	 * @return date of earliest evidence record for the specified time frame.
	 */
	public Date getEarliestDate(String dataType, String source,
			String filterAttribute, String filterValue)
	{		
		// NB. Does not use TimeFrame - all views will use evidence_second_view.
		
		String query = "CALL evidence_min_time(?, ?, ?, ?)";
		return m_SimpleJdbcTemplate.queryForObject(query.toString(), 
				java.sql.Timestamp.class, dataType, source, filterAttribute, filterValue);
	}

	
	/**
	 * Returns the date of the latest evidence record in the Prelert database
	 * for the given time frame.
	 * @param dataType identifier for the type of evidence data.
	 * @param source name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 * @param filterAttribute attribute name on which to filter on, or
	 * <code>null</code> if no WHERE clause should be applied.
	 * @param filterValue attribute value to use as the filter in a WHERE clause.
	 * @return date of latest evidence record for the specified time frame.
	 */
	public Date getLatestDate(String dataType, String source,
			String filterAttribute, String filterValue)
	{
		// NB. Does not use TimeFrame - all views will use evidence_second_view.
		
		String query = "CALL evidence_max_time(?, ?, ?, ?)";
		return m_SimpleJdbcTemplate.queryForObject(query.toString(), 
				java.sql.Timestamp.class, dataType, source, filterAttribute, filterValue);	
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
    

}
