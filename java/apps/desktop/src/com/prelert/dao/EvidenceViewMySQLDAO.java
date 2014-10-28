/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2009     *
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

package com.prelert.dao;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.ParameterizedSingleColumnRowMapper;

import com.prelert.data.*;
import com.prelert.data.gxt.GridRowInfo;


/**
 * Implementation for a MySQL database of the EvidenceViewDAO interface which 
 * predominantly uses calls to stored procedures to obtain information for 
 * Evidence list views.
 * @author Pete Harverson
 */
public class EvidenceViewMySQLDAO extends SpringJdbcTemplateDAO implements EvidenceViewDAO
{
	static Logger logger = Logger.getLogger(EvidenceViewMySQLDAO.class);
	
	
	/**
	 * Returns a list of all of the column names which are available for an
	 * Evidence view with the specified time frame.
	 * @param dataType identifier for the type of evidence data.
	 * @param time frame for which to obtain the columns.
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
	public List<String> getFilterableColumns(String dataType, 
			boolean getCompulsory, boolean getOptional)
	{
		// PROC is: call filterable_columns(getCompulsory, getOptional)
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
	 * Returns the total number of rows in the specified Evidence View.
	 * @param view Evidence View for which to obtain the row count.
	 * @return the row count for the database/view which the specified Evidence
	 * 			View is mapped to.
	 */
	public int getTotalRowCount(EvidenceView view)
	{
		// NB. May need to add in a WHERE clause at a later date.
		StringBuilder qryBlder = new StringBuilder("SELECT count(*) from ");
		qryBlder.append(view.getDatabaseView());
		
		String countQry = qryBlder.toString();
		logger.debug("getTotalRowCount() query: " + countQry);
		
		return m_SimpleJdbcTemplate.queryForInt(countQry);
	}
	
	
	/**
	 * Returns the first page of evidence data for a view with the specified
	 * time frame.
	 * @param dataType identifier for the type of evidence data.
	 * @param timeFrame time frame for which to obtain the first page of records.
	 * @return List of Evidence records comprising the first page of data.
	 */
	public List<EventRecord> getFirstPage(String dataType, TimeFrame timeFrame, 
			String filterAttribute, String filterValue)
	{
		List<EventRecord> evidenceList = null;
		String query = "";
		
		switch (timeFrame)
		{
			case ALL:
			case WEEK:
			case DAY:
			case HOUR:
			case MINUTE:
				// Query calls a stored procedure in the form:
		    	// CALL evidence_drill_down_first_page('apache_logs', 'minute', 'service has shutdown')
				query = "CALL evidence_drill_down_first_page(?, ?, ?)";
				logger.debug("getFirstPage() query: " + query);
				evidenceList = m_SimpleJdbcTemplate.query(query, new EventRecordRowMapper(), 
						dataType, timeFrame.toString().toLowerCase(), filterValue);
				break;
					
			case SECOND:
				query = "CALL evidence_first_page(?, ?, ?)";
				
				String debugQuery = "CALL evidence_first_page({0}, {1}, {2})";
				debugQuery = MessageFormat.format(debugQuery, dataType, filterAttribute, filterValue);
				logger.debug("getFirstPage() query: " + debugQuery);
				
				evidenceList = m_SimpleJdbcTemplate.query(query, new EventRecordRowMapper(), 
						dataType, filterAttribute, filterValue);
				break;
		}
		
		return evidenceList;
	}
	
	
	/**
	 * Returns the last page of evidence data for a view with the specified
	 * time frame.
	 * @param dataType identifier for the type of evidence data.
	 * @param timeFrame time frame for which to obtain the last page of records.
	 * @return List of Evidence records comprising the last page of data.
	 */
	public List<EventRecord> getLastPage(String dataType, TimeFrame timeFrame, 
			String filterAttribute, String filterValue)
	{	
		List<EventRecord> evidenceList = null;
		String query = "";
		
		switch (timeFrame)
		{
			case ALL:
			case WEEK:
			case DAY:
			case HOUR:
			case MINUTE:
				// Query calls a stored procedure in the form:
		    	// CALL evidence_drill_down_last_page('apache_logs', 'minute', 'description')
				query = "CALL evidence_drill_down_last_page(?, ?, ?)";
				logger.debug("getLastPage() query: " + query);
				evidenceList = m_SimpleJdbcTemplate.query(query, new EventRecordRowMapper(), 
						dataType, timeFrame.toString().toLowerCase(), filterValue);
				break;
					
			case SECOND:
				query = "CALL evidence_last_page(?, ?, ?)";
				
				String debugQuery = "CALL evidence_last_page({0}, {1}, {2})";
				debugQuery = MessageFormat.format(debugQuery, dataType, filterAttribute, filterValue);
				logger.debug("getLastPage() query: " + debugQuery);
				
				evidenceList = m_SimpleJdbcTemplate.query(query, new EventRecordRowMapper(), 
						dataType, filterAttribute, filterValue);
				break;
		}
		
		return evidenceList;
		
	}
	
	
	/**
	 * Returns the next page of evidence data following on from the row with the
	 * specified time and id.
	 * @param dataType identifier for the type of evidence data.
	 * @param timeFrame time frame for which to obtain the next page of records.
	 * @param bottomRowTime the value of the time column for the bottom row of 
	 * evidence in the current page.
	 * @param bottomRowId the value of the id column for the bottom row of evidence 
	 * in the current page.
	 * @return List of Evidence records comprising the next page of data.
	 */
	public List<EventRecord> getNextPage(
			String dataType, TimeFrame timeFrame, Date bottomRowTime, 
			String bottomRowId, String filterAttribute, String filterValue)
	{
		List<EventRecord> evidenceList = null;
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
		    	// CALL evidence_drill_down_next_page('minute', timeForBottomRow, 
				// 'bottom row description', false)				
				query = "CALL evidence_drill_down_next_page(?, ?, ?, ?, ?)";
				logger.debug("getNextPage() query: " + query);
				boolean filteredView = (filterValue != null);
				evidenceList = m_SimpleJdbcTemplate.query(
						query, new EventRecordRowMapper(), 
						dataType, timeFrame.toString().toLowerCase(), 
						bottomRowTimeStamp, bottomRowId, filteredView);
				break;
					
			case SECOND:
				query = "CALL evidence_next_page(?, ?, ?, ?, ?)";
				
				String debugQuery = "CALL evidence_next_page({0}, {1}, {2}, {3}, {4})";
				debugQuery = MessageFormat.format(debugQuery, dataType, bottomRowTimeStamp, 
						Integer.parseInt(bottomRowId), filterAttribute, filterValue);
				logger.debug("getNextPage() query: " + debugQuery);
				
				evidenceList = m_SimpleJdbcTemplate.query(query, new EventRecordRowMapper(), 
						dataType, bottomRowTimeStamp, Integer.parseInt(bottomRowId), 
						filterAttribute, filterValue);
				break;
		}
		
		return evidenceList;
		
	}
	
	
	/**
	 * Returns the previous page of evidence data to the row with the specified 
	 * time and id.
	 * @param dataType identifier for the type of evidence data.
	 * @param timeFrame time frame for which to obtain the previous page of records.
	 * @param topRowTime the value of the time column for the top row of 
	 * evidence in the current page.
	 * @param topRowId the value of the id column for the top row of evidence 
	 * in the current page.
	 * @return List of Evidence records comprising the previous page of data.
	 */
	public List<EventRecord> getPreviousPage(
			String dataType, TimeFrame timeFrame, Date topRowTime, 
			String topRowId, String filterAttribute, String filterValue)
	{
		List<EventRecord> evidenceList = null;
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
				// CALL evidence_drill_down_previous_page('apache_logs', 'minute', timeForTopRow, 
				// 'top row description', false)				
				query = "CALL evidence_drill_down_previous_page(?, ?, ?, ?, ?)";
				logger.debug("getPreviousPage() query: " + query);
				boolean filteredView = (filterValue != null);
				evidenceList = m_SimpleJdbcTemplate.query(query, new EventRecordRowMapper(), 
						dataType, timeFrame.toString().toLowerCase(), 
						topRowTimeStamp, topRowId, filteredView);
				break;
					
			case SECOND:
				query = "CALL evidence_previous_page(?, ?, ?, ?, ?)";
				
				String debugQuery = "CALL evidence_previous_page({0}, {1}, {2}, {3}, {4})";
				debugQuery = MessageFormat.format(debugQuery, dataType, topRowTimeStamp, 
						Integer.parseInt(topRowId), filterAttribute, filterValue);
				logger.debug("getPreviousPage() query: " + debugQuery);
				
				evidenceList = m_SimpleJdbcTemplate.query(query, new EventRecordRowMapper(), 
						dataType, topRowTimeStamp, Integer.parseInt(topRowId), 
						filterAttribute, filterValue);
				break;
		}
		
		return evidenceList;
	}
	
	
	/**
	 * Returns a page of evidence data, the top row of which matches the specified
	 * description and time.
	 * @param dataType identifier for the type of evidence data.
	 * @param timeFrame time frame for which to obtain evidence records.
	 * @param time value of the time column to match on for the first row of 
	 * evidence data that will be returned.
	 * @param description the value of the description column to match on for the
	 * top row of evidence data that will be returned.
	 * @return List of Evidence records, sorted descending on time.
	 */
	public List<EventRecord> getForDescription(
			String dataType, TimeFrame timeFrame, Date time, String description)
	{
		// NB. Need to switch on TimeFrame when other views are added in.
		
		String query = "CALL evidence_at_description_minute(?, ?, ?)";
		logger.debug("getForDescription() query: " + query);
		
		Timestamp timeStamp = new Timestamp(time.getTime());
		
		return m_SimpleJdbcTemplate.query(query, new EventRecordRowMapper(), 
				dataType, timeStamp, description);
	}
	
	
	/**
	 * Returns a page of evidence data, whose top row will match the supplied time.
	 * @param dataType identifier for the type of evidence data.
	 * @param timeFrame time frame for which to obtain evidence records.
	 * @param time value of the time column to match on for the first row of 
	 * evidence data that will be returned.
	 * @return List of Evidence records, sorted descending on time.
	 */
	public List<EventRecord> getAtTime(String dataType, TimeFrame timeFrame, Date time,
			String filterAttribute, String filterValue)
	{
		List<EventRecord> evidenceList = null;
		String query = "";
		String debugQuery;
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
				query = "CALL evidence_drill_down_at_time(?, ?, ?, ?)";
				
				debugQuery = "CALL evidence_drill_down_at_time({0}, {1}, {2}, {3})";
				debugQuery = MessageFormat.format(
						debugQuery, dataType, timeFrame.toString().toLowerCase(), timeStamp, filterValue);
				logger.debug("getAtTime() query: " + debugQuery);
				
				evidenceList = m_SimpleJdbcTemplate.query(query, new EventRecordRowMapper(), 
						dataType, timeFrame.toString().toLowerCase(), timeStamp, filterValue);
				break;
					
			case SECOND:
				query = "CALL evidence_at_time(?, ?, ?, ?)";
				
				debugQuery = "CALL evidence_at_time({0}, {1}, {2}, {3})";
				debugQuery = MessageFormat.format(
						debugQuery, dataType, timeStamp, filterAttribute, filterValue);
				logger.debug("getAtTime() query: " + debugQuery);
				
				evidenceList = m_SimpleJdbcTemplate.query(query, new EventRecordRowMapper(), 
						dataType, timeStamp, filterAttribute, filterValue);
				break;
		}
		
		
		
		return evidenceList;
	}
	
	
	/**
	 * Returns a page of real-time (SECOND time frame) evidence data, 
	 * the top row of which matches the specified id.
	 * @param dataType identifier for the type of evidence data.
	 * @param id evidence id for the top row of evidence data that will be returned.
	 * @return List of Evidence records, sorted descending on time.
	 */
	public List<EventRecord> getIdPage(String dataType, int id)
	{
		String query = "CALL evidence_id_page(?, ?);";
		logger.debug("getIdPage() query: " + query);
		
		return m_SimpleJdbcTemplate.query(query, new EventRecordRowMapper(), dataType, id);
	}
	
	
	/**
	 * Returns all attributes for the row of evidence with the specified id 
  	 * (instead of just the current display columns).
	 * @param id the value of the id column for the row to obtain information on.
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
	 * @param filterAttribute attribute name on which to filter on, or
	 * <code>null</code> if no WHERE clause should be applied.
	 * @param filterValue attribute value to use as the filter in a WHERE clause.
	 * @return date of latest evidence record for the specified time frame.
	 */
	public Date getEarliestDate(String dataType,  
			String filterAttribute, String filterValue)
	{	
		// NB. Does not use TimeFrame - all views will use evidence_second_view.
		
		String query = "CALL evidence_min_time(?, ?, ?)";
		return m_SimpleJdbcTemplate.queryForObject(query.toString(), 
				java.sql.Timestamp.class, dataType, filterAttribute, filterValue);
	}

	
	/**
	 * Returns the date of the latest evidence record in the Prelert database
	 * for the given time frame.
	 * @param dataType identifier for the type of evidence data.
	 * @param filterAttribute attribute name on which to filter on, or
	 * <code>null</code> if no WHERE clause should be applied.
	 * @param filterValue attribute value to use as the filter in a WHERE clause.
	 * @return date of latest evidence record for the specified time frame.
	 */
	public Date getLatestDate(String dataType, 
			String filterAttribute, String filterValue)
	{	
		// NB. Does not use TimeFrame - all views will use evidence_second_view.
		
		String query = "CALL evidence_max_time(?, ?, ?)";	
		return m_SimpleJdbcTemplate.queryForObject(query.toString(), 
				java.sql.Timestamp.class, dataType, filterAttribute, filterValue);
	}
    
}
