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
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;

import org.apache.commons.dbcp.BasicDataSourceFactory;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.prelert.dao.EvidenceDAO;
import com.prelert.dao.EvidenceModelListRowMapper;
import com.prelert.dao.EvidenceModelRowMapper;
import com.prelert.dao.SpringJdbcTemplateDAO;
import com.prelert.data.TimeFrame;
import com.prelert.data.gxt.AttributeModel;
import com.prelert.data.gxt.EvidenceModel;
import com.prelert.server.ServerUtil;


/**
 * Implementation for a PostgreSQL database of the EvidenceDAO interface which 
 * uses calls to function to obtain information for Evidence views.
 * @author Pete Harverson
 */
public class EvidencePostgreSQLDAO extends SpringJdbcTemplateDAO
	implements EvidenceDAO
{
	
	static Logger logger = Logger.getLogger(EvidencePostgreSQLDAO.class);
	
	private int m_PageSize = 20;
	
	
	@Override
    public List<String> getAllColumns(String dataType, TimeFrame timeFrame)
    {
		String qry = "";
		String viewName = "";
    	switch (timeFrame)
    	{
    		case ALL:
    		case WEEK:
    			viewName = "evidence_week_view";
    			break;
    			
    		case DAY:
    			viewName = "evidence_day_view";
    			break;
    			
    		case HOUR:
    			viewName = "evidence_hour_view";
    			break;
    			
    		case MINUTE:
    			viewName = "evidence_minute_view";
    			break;
    			
    		case SECOND:
    		default:
    			// Query calls a function of the form:
    			// call display_columns(type, getCompulsory, getOptional)
    			qry = "select * from display_columns(?, true, true);";
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
			qry="select column_name from information_schema.columns WHERE table_name = ? ORDER BY ordinal_position;";
			allColumns = m_SimpleJdbcTemplate.query(qry, mapper, viewName);
		}
		
		return allColumns;
    }
	
	
	@Override
    public List<String> getFilterableColumns(String dataType,
            boolean getCompulsory, boolean getOptional)
    {
		// Function is: filterable_columns(dataType, getCompulsory, getOptional)
		String query = "select * from filterable_columns(?, ?, ?)";		
	
		return m_SimpleJdbcTemplate.query(query.toString(), 
				new SingleColumnRowMapper<String>(), dataType, getCompulsory, getOptional);
    }
	
	
	@Override
    public List<String> getColumnValues(String dataType, String columnName,
            int maxRows)
    {
		// Function is: evidence_distinct_attribute(dataType, attribute, maxRows)
		String query = "select * from evidence_distinct_attribute(?, ?, ?)";	
		
		String debugQuery = "select * from evidence_distinct_attribute({0}, {1}, {2})";
		debugQuery = MessageFormat.format(debugQuery, dataType, columnName, maxRows);
		logger.debug("getColumnValues() query: " + debugQuery);
		
		return m_SimpleJdbcTemplate.query(query.toString(), 
			new SingleColumnRowMapper<String>(), dataType, columnName, maxRows);
    }
	
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
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
				// Function is:	
		    	// evidence_drill_down_first_page(dataType, source, timeFrame, description, pageSize)
				query = "select * from evidence_drill_down_first_page(?, ?, ?, ?, ?)";
				logger.debug("getFirstPage() query: " + query);
				evidenceList = m_SimpleJdbcTemplate.queryForObject(query, new EvidenceModelListRowMapper(),
						dataType, source, timeFrame.toString().toLowerCase(), filterValsArg, m_PageSize);
				break;
					
			case SECOND:
				// Function is:		
				// evidence_first_page(dataType, source, filterArrs, filterVals, pageSize)
				query = "select * from evidence_first_page(?, ?, ?, ?, ?);";
				
				String debugQuery = "select * from evidence_first_page({0}, {1}, {2}, {3}, {4})";
				debugQuery = MessageFormat.format(debugQuery, dataType, source, 
						filterAttrsArg, filterValsArg, m_PageSize);
				logger.debug("getFirstPage() query: " + debugQuery);
				
				evidenceList = m_SimpleJdbcTemplate.queryForObject(
						query, new EvidenceModelListRowMapper(),
						dataType, source, filterAttrsArg, filterValsArg, m_PageSize);
				break;
		}
		
		return evidenceList;
	}
	
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
    public List<EvidenceModel> getLastPage(String dataType, String source,
            TimeFrame timeFrame, List<String> filterAttributes,
            List<String> filterValues)
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
				// Function is:	
		    	// evidence_drill_down_last_page(dataType, source, timeFrame, description, pageSize)
				query = "select * from evidence_drill_down_last_page(?, ?, ?, ?, ?)";
				logger.debug("getLastPage() query: " + query);
				evidenceList = m_SimpleJdbcTemplate.queryForObject(query, new EvidenceModelListRowMapper(),
						dataType, source, timeFrame.toString().toLowerCase(), filterValsArg, m_PageSize);
				break;
					
			case SECOND:
				// Function is:		
				// evidence_last_page(dataType, source, filterArrs, filterVals, pageSize)
				query = "select * from evidence_last_page(?, ?, ?, ?, ?);";
				
				String debugQuery = "select * from evidence_last_page({0}, {1}, {2}, {3}, {4})";
				debugQuery = MessageFormat.format(debugQuery, dataType, source, 
						filterAttrsArg, filterValsArg, m_PageSize);
				logger.debug("getLastPage() query: " + debugQuery);
				
				evidenceList = m_SimpleJdbcTemplate.queryForObject(
						query, new EvidenceModelListRowMapper(),
						dataType, source, filterAttrsArg, filterValsArg, m_PageSize);
				break;
		}
		
		return evidenceList;
    }
	
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
    public List<EvidenceModel> getNextPage(String dataType, String source,
            TimeFrame timeFrame, Date bottomRowTime, String bottomRowId,
            List<String> filterAttributes, List<String> filterValues)
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
				// Function is:	
		    	// evidence_drill_down_next_page(dataType, source, timeFrame, time, bottomRowDesc, isFiltered, pageSize)			
				query = "select * from evidence_drill_down_next_page(?, ?, ?, ?, ?, ?, ?)";
				logger.debug("getNextPage() query: " + query);
				boolean filteredView = (filterValsArg != null);
				evidenceList = m_SimpleJdbcTemplate.queryForObject(
						query, new EvidenceModelListRowMapper(), 
						dataType, source, timeFrame.toString().toLowerCase(), 
						bottomRowTime, bottomRowId, filteredView, m_PageSize);
				break;
					
			case SECOND:
				// Function is:	
		    	// evidence_next_page(dataType, source, time, bottomRowId, filterAttrs, filterVals, pageSize)
				query = "select * from evidence_next_page(?, ?, ?, ?, ?, ?, ?)";
				
				String debugQuery = "select * from evidence_next_page({0}, {1}, {2}, {3}, {4}, {5}, {6})";
				debugQuery = MessageFormat.format(debugQuery, dataType, source, 
						ServerUtil.formatTimeField(bottomRowTime, TimeFrame.SECOND), 
						Integer.parseInt(bottomRowId), filterAttrsArg, filterValsArg, m_PageSize);
				logger.debug("getNextPage() query: " + debugQuery);
				
				evidenceList = m_SimpleJdbcTemplate.queryForObject(
						query, new EvidenceModelListRowMapper(), 
						dataType, source, bottomRowTime, Integer.parseInt(bottomRowId), 
						filterAttrsArg, filterValsArg, m_PageSize);
				break;
		}
		
		return evidenceList;
    }
	
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
    public List<EvidenceModel> getPreviousPage(String dataType, String source,
            TimeFrame timeFrame, Date topRowTime, String topRowId,
            List<String> filterAttributes, List<String> filterValues)
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
				// Function is:	
		    	// evidence_drill_down_previous_page(dataType, source, timeFrame, topRowTime, topRowDesc, isFiltered, pageSize)		
				query = "select * from evidence_drill_down_previous_page(?, ?, ?, ?, ?, ?, ?)";
				logger.debug("getPreviousPage() query: " + query);
				boolean filteredView = (filterValsArg != null);
				evidenceList = m_SimpleJdbcTemplate.queryForObject(
						query, new EvidenceModelListRowMapper(), 
						dataType, source, timeFrame.toString().toLowerCase(), 
						topRowTime, topRowId, filteredView, m_PageSize);
				break;
					
			case SECOND:
				// Function is:	
		    	// evidence_previous_page(dataType, source, topRowTime, topRowId, filterAttrs, filterVals, pageSize)
				query = "select * from evidence_previous_page(?, ?, ?, ?, ?, ?, ?)";
				
				String debugQuery = "select * from evidence_previous_page({0}, {1}, {2}, {3}, {4}, {5}, {6})";
				debugQuery = MessageFormat.format(debugQuery, dataType, source, topRowTime, 
						Integer.parseInt(topRowId), filterAttrsArg, filterValsArg, m_PageSize);
				logger.debug("getPreviousPage() query: " + debugQuery);
				
				evidenceList = m_SimpleJdbcTemplate.queryForObject(query, new EvidenceModelListRowMapper(), 
						dataType, source, topRowTime, Integer.parseInt(topRowId), 
						filterAttrsArg, filterValsArg, m_PageSize);
				break;
		}
		
		return evidenceList;
    }
	
	
	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    public List<EvidenceModel> getForDescription(String dataType,
            String source, TimeFrame timeFrame, Date time, String description)
    {
		// NB. Need to switch on TimeFrame when other views are added in.
		
		// Function is:
		// evidence_at_description_minute(type, source, time, description, pageSize)
		String query = "select * from evidence_at_description_minute(?, ?, ?, ?, ?)";
		logger.debug("getForDescription() query: " + query);
		
		return m_SimpleJdbcTemplate.queryForObject(query, new EvidenceModelListRowMapper(), 
				dataType, source, time, description, m_PageSize);
    }
	
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
    public List<EvidenceModel> getAtTime(String dataType, String source,
            TimeFrame timeFrame, Date time, List<String> filterAttributes,
            List<String> filterValues)
    {
		List<EvidenceModel> evidenceList = null;
		String query = "";
		String debugQuery = "";
		
		String filterAttrsArg = ServerUtil.prepareFilterArgument(filterAttributes);
		String filterValsArg = ServerUtil.prepareFilterArgument(filterValues);
		
		switch (timeFrame)
		{
			case ALL:
			case WEEK:
			case DAY:
			case HOUR:
			case MINUTE:
				// Function is:		
				// evidence_drill_down_at_time(dataType, source, timeFrame, time, description, pageSize)
				query = "select * from evidence_drill_down_at_time(?, ?, ?, ?, ?, ?);";
				
				debugQuery = "select * from evidence_drill_down_at_time({0}, {1}, {2}, {3}, {4}, {5})";
				debugQuery = MessageFormat.format(
						debugQuery, dataType, source, timeFrame.toString().toLowerCase(), 
						ServerUtil.formatTimeField(time, TimeFrame.SECOND), filterValsArg, m_PageSize);
				logger.debug("getAtTime() query: " + debugQuery);
				
				evidenceList = m_SimpleJdbcTemplate.queryForObject(
						query, new EvidenceModelListRowMapper(), 
						dataType, source, timeFrame.toString().toLowerCase(), 
						time, filterValsArg, m_PageSize);
				break;
					
			case SECOND:
				// Function is: 
				// evidence_at_time(dataType, source, time, filterArgs, filterVals, pageSize)
				query = "select * from evidence_at_time(?, ?, ?, ?, ?, ?)";
				
				debugQuery = "select * from evidence_at_time({0}, {1}, {2}, {3}, {4}, {5})";
				debugQuery = MessageFormat.format(
						debugQuery, dataType, source, 
						ServerUtil.formatTimeField(time, TimeFrame.SECOND), 
						filterAttrsArg, filterValsArg, m_PageSize);
				logger.debug("getAtTime() query: " + debugQuery);
				
				evidenceList = m_SimpleJdbcTemplate.queryForObject(
						query, new EvidenceModelListRowMapper(), 
						dataType, source, time, filterAttrsArg, filterValsArg, m_PageSize);
				break;
		}

		return evidenceList;
    }
	
	
	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    public List<EvidenceModel> getIdPage(String dataType, String source, int id)
    {
		// Function is:
		// evidence_id_page(type, source, id, pageSize)
		String query = "select * from evidence_id_page(?, ?, ?, ?);";
		
		String debugQuery = "select * from evidence_id_page({0}, {1}, {2}, {3})";
		debugQuery = MessageFormat.format(
				debugQuery, dataType, source, id, m_PageSize);
		logger.debug("getIdPage() query: " + debugQuery);
		
		return m_SimpleJdbcTemplate.queryForObject(query, new EvidenceModelListRowMapper(), 
				dataType, source, id, m_PageSize);
    }
	
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
    public List<EvidenceModel> searchFirstPage(String dataType, String source,
            String containsText)
    {
		// Function is:
		// evidence_search_first_page(type, source, searchString, pageSize)
		String query = "select * from evidence_search_first_page(?, ?, ?, ?)";
		
		String debugQuery = "select * from evidence_search_first_page({0}, {1}, {2}, {3})";
		debugQuery = MessageFormat.format(debugQuery, dataType, source, 
				containsText, m_PageSize);
		logger.debug("searchFirstPage() query: " + debugQuery);
		
		return m_SimpleJdbcTemplate.queryForObject(query, new EvidenceModelListRowMapper(),
				dataType, source, containsText, m_PageSize);
    }


	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
    public List<EvidenceModel> searchLastPage(String dataType, String source,
            String containsText)
    {
		// Function is:
		// evidence_search_last_page(type, source, searchString, pageSize)
		String query = "select * from evidence_search_last_page(?, ?, ?, ?)";
		
		String debugQuery = "select * from evidence_search_last_page({0}, {1}, {2}, {3})";
		debugQuery = MessageFormat.format(debugQuery, dataType, source, 
				containsText, m_PageSize);
		logger.debug("searchLastPage() query: " + debugQuery);
		
		return m_SimpleJdbcTemplate.queryForObject(query, new EvidenceModelListRowMapper(),
				dataType, source, containsText, m_PageSize);
    }
	
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
    public List<EvidenceModel> searchNextPage(String dataType, String source,
            Date bottomRowTime, int bottomRowId, String containsText)
    {
		// Function is:
		// evidence_search_next_page(type, source, time, id, searchString, pageSize)
		String query = "select * from evidence_search_next_page(?, ?, ?, ?, ?, ?)";
		
		String debugQuery = "select * from evidence_search_next_page({0}, {1}, {2}, {3}, {4}, {5})";
		debugQuery = MessageFormat.format(debugQuery, dataType, source, 
				ServerUtil.formatTimeField(bottomRowTime, TimeFrame.SECOND), 
				bottomRowId, containsText, m_PageSize);
		logger.debug("searchNextPage() query: " + debugQuery);
		
		return m_SimpleJdbcTemplate.queryForObject(query, new EvidenceModelListRowMapper(),
				dataType, source, bottomRowTime, bottomRowId, containsText, m_PageSize);
    }


	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
    public List<EvidenceModel> searchPreviousPage(String dataType,
            String source, Date topRowTime, int topRowId, String containsText)
    {
		// Function is:
		// evidence_search_previous_page(type, source, time, id, searchString, pageSize)
		String query = "select * from evidence_search_previous_page(?, ?, ?, ?, ?, ?)";
		
		String debugQuery = "select * from evidence_search_previous_page({0}, {1}, {2}, {3}, {4}, {5})";
		debugQuery = MessageFormat.format(debugQuery, dataType, source, 
				ServerUtil.formatTimeField(topRowTime, TimeFrame.SECOND), 
				topRowId, containsText, m_PageSize);
		logger.debug("searchPreviousPage() query: " + debugQuery);
		
		return m_SimpleJdbcTemplate.queryForObject(query, new EvidenceModelListRowMapper(),
				dataType, source, topRowTime, topRowId, containsText, m_PageSize);
    }
	
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
    public List<EvidenceModel> searchAtTime(String dataType, String source,
            Date time, String containsText)
    {
		// Function is:
		// evidence_search_at_time(type, source, time, searchString, pageSize)
		String query = "select * from evidence_search_at_time(?, ?, ?, ?, ?)";
		
		String debugQuery = "select * from evidence_search_at_time({0}, {1}, {2}, {3}, {4})";
		debugQuery = MessageFormat.format(debugQuery, dataType, source, 
				ServerUtil.formatTimeField(time, TimeFrame.SECOND), containsText, m_PageSize);
		logger.debug("searchAtTime() query: " + debugQuery);
		
		return m_SimpleJdbcTemplate.queryForObject(query, new EvidenceModelListRowMapper(),
				dataType, source, time, containsText, m_PageSize);
    }
	
	
	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    public EvidenceModel getEvidenceSingle(int id)
    {
		String query = "select * from evidence_single_complete(?);";
		
		String debugQuery = "select * from evidence_single_complete({0});";
		debugQuery = MessageFormat.format(debugQuery, id);
		logger.debug("getEvidenceSingle() query: " + debugQuery);
		
		List<EvidenceModel> evidenceList = m_SimpleJdbcTemplate.queryForObject(
				query, new EvidenceModelListRowMapper(), id);
		EvidenceModel evidence = null;
		if (evidenceList.size() > 0)
		{
			evidence = evidenceList.get(0);
		}
		
		return evidence;
    }
	
	
	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    public List<AttributeModel> getEvidenceAttributes(int id)
    {
		String query = "select * from evidence_single_complete(?)";

		return m_SimpleJdbcTemplate.queryForObject(
				query, new AttributeModelListRowMapper(), id);
    }
	
	
	@Override
    public EvidenceModel getEarliestEvidence(String dataType, String source,
            List<String> filterAttributes, List<String> filterValues)
    {
		// NB. Does not use TimeFrame - all views will use evidence_second_view.
		String filterAttribute = ServerUtil.prepareFilterArgument(filterAttributes);
		String filterValue = ServerUtil.prepareFilterArgument(filterValues);
		
		String query = "select * from evidence_min_time(?, ?, ?, ?)";		
		return m_SimpleJdbcTemplate.queryForObject(query.toString(), 
				new EvidenceModelRowMapper(), dataType, source, filterAttribute, filterValue);
    }


	@Override
    public EvidenceModel getLatestEvidence(String dataType, String source,
            List<String> filterAttributes, List<String> filterValues)
    {
		// NB. Does not use TimeFrame - all views will use evidence_second_view.
		String filterAttribute = ServerUtil.prepareFilterArgument(filterAttributes);
		String filterValue = ServerUtil.prepareFilterArgument(filterValues);
		
		String query = "select * from evidence_max_time(?, ?, ?, ?)";		
		return m_SimpleJdbcTemplate.queryForObject(query.toString(), 
				new EvidenceModelRowMapper(), dataType, source, filterAttribute, filterValue);
    }
	
	
	@Override
    public EvidenceModel searchEarliestEvidence(String dataType, String source,
            String containsText)
    {
		String query = "select * from evidence_search_min_time(?, ?, ?)";
		return m_SimpleJdbcTemplate.queryForObject(query.toString(), 
				new EvidenceModelRowMapper(), dataType, source, containsText);
    }


	@Override
    public EvidenceModel searchLatestEvidence(String dataType, String source,
            String containsText)
    {
		String query = "select * from evidence_search_max_time(?, ?, ?)";
		return m_SimpleJdbcTemplate.queryForObject(query.toString(), 
				new EvidenceModelRowMapper(), dataType, source, containsText);
    }
	
	
	@Override
    public int getPageSize()
    {
	    return m_PageSize;
    }


	@Override
    public void setPageSize(int pageSize)
    {
		m_PageSize = pageSize;
    }
	
	
	/**
	 * ParameterizedRowMapper class for mapping a single evidence query result 
	 * set to a list of AttributeModel objects. Attribute values for time fields
	 * are converted to a String representation of the number of milliseconds 
	 * since January 1, 1970, 00:00:00 GMT.
	 * <p>
	 * The RowMapper expects the evidence query to return a <i>refcursor</i>, 
	 * so that results are processed by first casting the return type of getObject() 
	 * to a ResultSet i.e.
	 * <pre>
	 * 	ResultSet refCursor = (ResultSet) rs.getObject(1);	
	 * </pre> 
	 */
	class AttributeModelListRowMapper implements RowMapper<List<AttributeModel>>
	{
		private String m_TimeColumnName = EvidenceModel.getTimeColumnName(TimeFrame.SECOND);

		@Override
	    public List<AttributeModel> mapRow(ResultSet rs, int rowNum) throws SQLException
	    {
			ArrayList<AttributeModel> rowData = new ArrayList<AttributeModel>();
			
			ResultSet refCursor = (ResultSet) rs.getObject(1);
			ResultSetMetaData metaData = refCursor.getMetaData();

			String columnName;
			String columnValue;
			
			while (refCursor.next())
			{
				for (int i = 1; i <= metaData.getColumnCount(); i++)
				{
					columnName = metaData.getColumnLabel(i);
					
					if (columnName.equals(m_TimeColumnName) == true)
					{
						// Convert time value to a Long so that formatting can be
						// done client side.
						Date evidenceTime = refCursor.getTimestamp(m_TimeColumnName);
						columnValue = Long.toString(evidenceTime.getTime());
					}
					else
					{
						columnValue = refCursor.getString(i);
					}
					
					rowData.add(new AttributeModel(columnName, columnValue));
					
				}
			}

	        return rowData;
	    }
	}
	
	
	public static void main(String[] args)
	{
		// main() method for standalone testing.
		
		EvidencePostgreSQLDAO evidenceDAO = new EvidencePostgreSQLDAO();
		
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
	        evidenceDAO.setDataSource(dataSource);
	        logger.debug("Initialised PostgreSQL datasource");
	        
	        // getLastPage() query.
	        List<EvidenceModel> evidenceList = 
	        	evidenceDAO.getLastPage("p2pslog", null, TimeFrame.MINUTE, null, null);
	        for (EvidenceModel evidence : evidenceList)
	        {
	        	logger.debug("Paging proc returned: " + evidence);
	        }
	        
	        EvidenceModel evidence = evidenceDAO.getEvidenceSingle(484);
	        logger.debug("ID 484 is: " + evidence);
	        
	        logger.debug("+++ getAllColumns() +++");
	        List<String> allColumns = evidenceDAO.getAllColumns(null, TimeFrame.MINUTE);
	        for (String column : allColumns)
	        {
	        	logger.debug("Got column: " + column);
	        }
	        
	        // getAtTime() query.
	        logger.debug("+++ getAtTime() +++");
	        GregorianCalendar calendar = new GregorianCalendar(2010, 3, 8);
	        evidenceList = evidenceDAO.getAtTime("p2pslog", null,
	                TimeFrame.HOUR, calendar.getTime(), null, null);
	        for (EvidenceModel ev : evidenceList)
	        {
	        	logger.debug("Paging proc returned: " + ev);
	        }
	        
	        
	        // getEvidenceAttributes() query.
	        logger.debug("+++ getEvidenceAttributes() +++");
	        List<AttributeModel> attributes = evidenceDAO.getEvidenceAttributes(484);
	        for (AttributeModel attribute : attributes)
	        {
	        	logger.debug("Attribute: " + attribute);
	        }
	        
	        
	        // getAtTime() query.
	        logger.debug("+++ getIdPage() +++");
	        evidenceList = evidenceDAO.getIdPage(null, null, 484);
	        for (EvidenceModel ev : evidenceList)
	        {
	        	logger.debug("Paging proc returned: " + ev);
	        }
	        
	        // searchPreviousPage() query.
	        logger.debug("+++ searchPreviousPage() +++");
	        calendar = new GregorianCalendar(2010, 3, 31, 19, 1, 47);
	        for (EvidenceModel ev : evidenceList)
	        {
	        	logger.debug("Paging proc returned: " + ev);
	        }
	        
        }
        catch (Exception e)
        {
	        e.printStackTrace();
        }
		
		
	}
	
}
