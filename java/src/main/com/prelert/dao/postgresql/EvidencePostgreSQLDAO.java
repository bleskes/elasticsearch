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

package com.prelert.dao.postgresql;

import java.sql.ResultSet;


import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.prelert.dao.EvidenceDAO;
import com.prelert.data.Evidence;
import com.prelert.data.MetricPath;
import com.prelert.data.TimeFrame;
import com.prelert.data.Attribute;
import com.prelert.dao.spring.EvidenceRowMapper;
import com.prelert.dao.spring.EvidenceListRowMapper;
import com.prelert.dao.spring.MetricPathRowMapper;
import com.prelert.server.ServerUtil;


/**
 * Implementation for a PostgreSQL database of the EvidenceDAO interface which 
 * uses calls to functions to obtain information for Evidence views.
 * @author Pete Harverson
 */
public class EvidencePostgreSQLDAO extends SimpleJdbcDaoSupport
	implements EvidenceDAO
{
	
	static Logger s_Logger = Logger.getLogger(EvidencePostgreSQLDAO.class);
	
	
	@Override
    public List<String> getAllColumns(String dataType)
    {
		// Query calls a function of the form:
		// call display_columns(type, getCompulsory, getOptional)
		String qry = "select * from display_columns(?, true, true);";
		
		RowMapper<String> mapper = new RowMapper<String>(){
			
			@Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException
            {				
				return rs.getString(1);
            }
		};
		
		
		return getSimpleJdbcTemplate().query(qry, mapper, dataType);
    }
	
	
	@Override
    public List<String> getFilterableColumns(String dataType,
            boolean getCompulsory, boolean getOptional)
    {
		// Function is: filterable_columns(dataType, getCompulsory, getOptional)
		String query = "select * from filterable_columns(?, ?, ?)";		
	
		return getSimpleJdbcTemplate().query(query, 
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
		s_Logger.debug("getColumnValues() query: " + debugQuery);
		
		return getSimpleJdbcTemplate().query(query, 
			new SingleColumnRowMapper<String>(), dataType, columnName, maxRows);
    }
	
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public List<Evidence> getFirstPage(String dataType, String source,
			List<String> filterAttributes, List<String> filterValues, int pageSize)
	{
		String filterAttrsArg = ServerUtil.prepareFilterArgument(filterAttributes);
		String filterValsArg = ServerUtil.prepareFilterArgument(filterValues);
		

		// Function is:		
		// evidence_first_page(dataType, source, filterArrs, filterVals, pageSize)
		String query = "select * from evidence_first_page(?, ?, ?, ?, ?);";
		
		String debugQuery = "select * from evidence_first_page({0}, {1}, {2}, {3}, {4})";
		debugQuery = MessageFormat.format(debugQuery, dataType, source, 
				filterAttrsArg, filterValsArg, pageSize);
		s_Logger.debug("getFirstPage() query: " + debugQuery);
				
		return getSimpleJdbcTemplate().queryForObject(
						query, new EvidenceListRowMapper(),
						dataType, source, filterAttrsArg, filterValsArg, pageSize);

	}
	
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
    public List<Evidence> getLastPage(String dataType, String source,
            List<String> filterAttributes, List<String> filterValues, int pageSize)
    {
		String filterAttrsArg = ServerUtil.prepareFilterArgument(filterAttributes);
		String filterValsArg = ServerUtil.prepareFilterArgument(filterValues);

		// Function is:		
		// evidence_last_page(dataType, source, filterArrs, filterVals, pageSize)
		String query = "select * from evidence_last_page(?, ?, ?, ?, ?);";
				
		String debugQuery = "select * from evidence_last_page({0}, {1}, {2}, {3}, {4})";
		debugQuery = MessageFormat.format(debugQuery, dataType, source, 
				filterAttrsArg, filterValsArg, pageSize);
		s_Logger.debug("getLastPage() query: " + debugQuery);
		
		return getSimpleJdbcTemplate().queryForObject(
				query, new EvidenceListRowMapper(),
				dataType, source, filterAttrsArg, filterValsArg, pageSize);
    }
	
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
    public List<Evidence> getNextPage(String dataType, String source,
            Date bottomRowTime, int bottomRowId,
            List<String> filterAttributes, List<String> filterValues, int pageSize)
    {
		String filterAttrsArg = ServerUtil.prepareFilterArgument(filterAttributes);
		String filterValsArg = ServerUtil.prepareFilterArgument(filterValues);

		// Function is:	
    	// evidence_next_page(dataType, source, time, bottomRowId, filterAttrs, filterVals, pageSize)
		String query = "select * from evidence_next_page(?, ?, ?, ?, ?, ?, ?)";
				
		String debugQuery = "select * from evidence_next_page({0}, {1}, {2}, {3}, {4}, {5}, {6})";
		debugQuery = MessageFormat.format(debugQuery, dataType, source, 
				ServerUtil.formatTimeField(bottomRowTime, TimeFrame.SECOND), 
				bottomRowId, filterAttrsArg, filterValsArg, pageSize);
		s_Logger.debug("getNextPage() query: " + debugQuery);
				
		return getSimpleJdbcTemplate().queryForObject(query, new EvidenceListRowMapper(), 
						dataType, source, bottomRowTime, bottomRowId, 
						filterAttrsArg, filterValsArg, pageSize);
    }
	
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
    public List<Evidence> getPreviousPage(String dataType, String source,
            Date topRowTime, int topRowId,
            List<String> filterAttributes, List<String> filterValues, int pageSize)
    {
		String filterAttrsArg = ServerUtil.prepareFilterArgument(filterAttributes);
		String filterValsArg = ServerUtil.prepareFilterArgument(filterValues);

		// Function is:	
    	// evidence_previous_page(dataType, source, topRowTime, topRowId, filterAttrs, filterVals, pageSize)
		String query = "select * from evidence_previous_page(?, ?, ?, ?, ?, ?, ?)";
				
		String debugQuery = "select * from evidence_previous_page({0}, {1}, {2}, {3}, {4}, {5}, {6})";
		debugQuery = MessageFormat.format(debugQuery, dataType, source, topRowTime, 
				topRowId, filterAttrsArg, filterValsArg, pageSize);
		s_Logger.debug("getPreviousPage() query: " + debugQuery);
		
		return getSimpleJdbcTemplate().queryForObject(query, new EvidenceListRowMapper(), 
				dataType, source, topRowTime, topRowId, 
				filterAttrsArg, filterValsArg, pageSize);
    }
	
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
    public List<Evidence> getAtTime(String dataType, String source, Date time, 
    		List<String> filterAttributes, List<String> filterValues, int pageSize)
    {
		String filterAttrsArg = ServerUtil.prepareFilterArgument(filterAttributes);
		String filterValsArg = ServerUtil.prepareFilterArgument(filterValues);

		// Function is: 
		// evidence_at_time(dataType, source, time, filterArgs, filterVals, pageSize)
		String query = "select * from evidence_at_time(?, ?, ?, ?, ?, ?)";
				
		String debugQuery = "select * from evidence_at_time({0}, {1}, {2}, {3}, {4}, {5})";
		debugQuery = MessageFormat.format(
				debugQuery, dataType, source, 
				ServerUtil.formatTimeField(time, TimeFrame.SECOND), 
				filterAttrsArg, filterValsArg, pageSize);
		s_Logger.debug("getAtTime() query: " + debugQuery);

		return getSimpleJdbcTemplate().queryForObject(query, new EvidenceListRowMapper(), 
					dataType, source, time, filterAttrsArg, filterValsArg, pageSize);
    }

	
	/**
	 * For the Time Series which can be uniquely defined by <code>datatype</code>,
	 * <code>metric</code>, <code>source</code> and <code>attributes</code> this 
	 * function returns any evidence features which occurred between the time period 
	 * <code>startTime</code> and <code>endTime</code>.
	 * 
	 * @param datatype 
	 * @param metric
	 * @param source
	 * @param attributes Filter by attributes
	 * @param startTime
	 * @param endTime
	 * @return List of <code>Evidence</code> features, if any, for the time series 
	 * defined by datatype, metric, source in the time period.
	 * The only the <code>Id</code> and <code>Time</code> fields will be populated
	 * in the returned <code>Evidence</code> objects. 
	 */
	@Override
	public List<Evidence> getEvidenceInTimeSeries(String datatype, String metric,
												String source, List<Attribute> attributes,
												Date startTime, Date endTime)
	{
		String query = "select * from evidence_for_time_series(?, ?, ?, ?, ?, ?, ?)";

		String attributeNames = ServerUtil.prepareAttributeNameArgument(attributes);
		String attributeValues = ServerUtil.prepareAttributeValueArgument(attributes);
		
		String debugQuery = "select * from evidence_for_time_series(''{0}'', ''{1}'', ''{2}'', ''{3}'', " +
				"''{4}'', ''{5}'', ''{6}'')";
		debugQuery = MessageFormat.format(
				debugQuery, datatype, metric, source, startTime, endTime,
				attributeNames, attributeValues);
		s_Logger.debug("getEvidenceInTimeSeries() query: " + debugQuery);

		// NB: Unlike the other procs that return evidence, this one does NOT
		// return a refcursor, so it uses a different row mapper
		return getSimpleJdbcTemplate().query(query, new EvidenceRowMapper(),
				datatype, metric, source, startTime, endTime,
				attributeNames, attributeValues);
	}
	

	/**
	 * For a Time Series which can be uniquely defined by <code>datatype</code>,
	 * <code>metric</code> and <code>source</code> this function returns any
	 * evidence features which occurred between the time period <code>startTime</code>
	 * and <code>endTime</code>.
	 * @param datatype
	 * @param metric
	 * @param source
	 * @param attributes Filter by attributes
	 * @param startTime
	 * @param endTime
	 * @return List of <code>Evidence</code> features, if any, for the time series
	 * defined by datatype, metric, source in the time period.
	 * The only the <code>Id</code> and <code>Time</code> fields will be populated
	 * in the returned <code>Evidence</code> objects.
	 */
	@Override
	public List<Evidence> getEvidenceInExternalTimeSeries(String datatype, String metric,
												String source, List<Attribute> attributes,
												Date startTime, Date endTime)
	{
		String query = "select * from evidence_for_external_time_series(?, ?, ?, ?, ?, ?, ?)";

		String attributeNames = ServerUtil.prepareAttributeNameArgument(attributes);
		String attributeValues = ServerUtil.prepareAttributeValueArgument(attributes);
		
		String debugQuery = "select * from evidence_for_external_time_series(''{0}'', ''{1}'', ''{2}'', ''{3}'', " +
				"''{4}'', ''{5}'', ''{6}'')";
		debugQuery = MessageFormat.format(
				debugQuery, datatype, metric, source, startTime, endTime,
				attributeNames, attributeValues);
		s_Logger.debug("getEvidenceInExternalTimeSeries() query: " + debugQuery);

		// NB: Unlike the other procs that return evidence, this one does NOT
		// return a refcursor, so it uses a different row mapper
		return getSimpleJdbcTemplate().query(query, new EvidenceRowMapper(),
				datatype, metric, source, startTime, endTime,
				attributeNames, attributeValues);
	}
	
	
	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    public List<Evidence> getIdPage(String dataType, String source, int id, int pageSize)
    {
		// Function is:
		// evidence_id_page(type, source, id, pageSize)
		String query = "select * from evidence_id_page(?, ?, ?, ?);";
		
		String debugQuery = "select * from evidence_id_page({0}, {1}, {2}, {3})";
		debugQuery = MessageFormat.format(
				debugQuery, dataType, source, id, pageSize);
		s_Logger.debug("getIdPage() query: " + debugQuery);
		
		return getSimpleJdbcTemplate().queryForObject(query, new EvidenceListRowMapper(), 
				dataType, source, id, pageSize);
    }
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
    public List<Evidence> searchFirstPage(String dataType, String source,
            String containsText, int pageSize)
    {
		// Function is:
		// evidence_search_first_page(type, source, searchString, pageSize)
		String query = "select * from evidence_search_first_page(?, ?, ?, ?)";
		
		String debugQuery = "select * from evidence_search_first_page({0}, {1}, {2}, {3})";
		debugQuery = MessageFormat.format(debugQuery, dataType, source, 
				containsText, pageSize);
		s_Logger.debug("searchFirstPage() query: " + debugQuery);
		
		return getSimpleJdbcTemplate().queryForObject(query, new EvidenceListRowMapper(),
				dataType, source, containsText, pageSize);
    }


	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
    public List<Evidence> searchLastPage(String dataType, String source,
            String containsText, int pageSize)
    {
		// Function is:
		// evidence_search_last_page(type, source, searchString, pageSize)
		String query = "select * from evidence_search_last_page(?, ?, ?, ?)";
		
		String debugQuery = "select * from evidence_search_last_page({0}, {1}, {2}, {3})";
		debugQuery = MessageFormat.format(debugQuery, dataType, source, 
				containsText, pageSize);
		s_Logger.debug("searchLastPage() query: " + debugQuery);
		
		return getSimpleJdbcTemplate().queryForObject(query, new EvidenceListRowMapper(),
				dataType, source, containsText, pageSize);
    }
	
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
    public List<Evidence> searchNextPage(String dataType, String source,
            Date bottomRowTime, int bottomRowId, String containsText, int pageSize)
    {
		// Function is:
		// evidence_search_next_page(type, source, time, id, searchString, pageSize)
		String query = "select * from evidence_search_next_page(?, ?, ?, ?, ?, ?)";
		
		String debugQuery = "select * from evidence_search_next_page({0}, {1}, {2}, {3}, {4}, {5})";
		debugQuery = MessageFormat.format(debugQuery, dataType, source, 
				ServerUtil.formatTimeField(bottomRowTime, TimeFrame.SECOND), 
				bottomRowId, containsText, pageSize);
		s_Logger.debug("searchNextPage() query: " + debugQuery);
		
		return getSimpleJdbcTemplate().queryForObject(query, new EvidenceListRowMapper(),
				dataType, source, bottomRowTime, bottomRowId, containsText, pageSize);
    }


	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
    public List<Evidence> searchPreviousPage(String dataType, String source, 
    		Date topRowTime, int topRowId, String containsText, int pageSize)
    {
		// Function is:
		// evidence_search_previous_page(type, source, time, id, searchString, pageSize)
		String query = "select * from evidence_search_previous_page(?, ?, ?, ?, ?, ?)";
		
		String debugQuery = "select * from evidence_search_previous_page({0}, {1}, {2}, {3}, {4}, {5})";
		debugQuery = MessageFormat.format(debugQuery, dataType, source, 
				ServerUtil.formatTimeField(topRowTime, TimeFrame.SECOND), 
				topRowId, containsText, pageSize);
		s_Logger.debug("searchPreviousPage() query: " + debugQuery);
		
		return getSimpleJdbcTemplate().queryForObject(query, new EvidenceListRowMapper(),
				dataType, source, topRowTime, topRowId, containsText, pageSize);
    }
	
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
    public List<Evidence> searchAtTime(String dataType, String source,
            Date time, String containsText, int pageSize)
    {
		// Function is:
		// evidence_search_at_time(type, source, time, searchString, pageSize)
		String query = "select * from evidence_search_at_time(?, ?, ?, ?, ?)";
		
		String debugQuery = "select * from evidence_search_at_time({0}, {1}, {2}, {3}, {4})";
		debugQuery = MessageFormat.format(debugQuery, dataType, source, 
				ServerUtil.formatTimeField(time, TimeFrame.SECOND), containsText, pageSize);
		s_Logger.debug("searchAtTime() query: " + debugQuery);
		
		return getSimpleJdbcTemplate().queryForObject(query, new EvidenceListRowMapper(),
				dataType, source, time, containsText, pageSize);
    }
	
	
	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    public Evidence getEvidenceSingle(int id)
    {
		String query = "select * from evidence_single_complete(?);";
		
		String debugQuery = "select * from evidence_single_complete({0});";
		debugQuery = MessageFormat.format(debugQuery, id);
		s_Logger.debug("getEvidenceSingle() query: " + debugQuery);
		
		List<Evidence> evidenceList = getSimpleJdbcTemplate().queryForObject(
				query, new EvidenceListRowMapper(), id);
		Evidence evidence = null;
		if (evidenceList.size() > 0 && evidenceList.get(0) != null)
		{
			evidence = evidenceList.get(0);
		}
		else
		{
			s_Logger.warn("No results from query: " + debugQuery);
		}

		return evidence;
    }
	
	
	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    public List<Attribute> getEvidenceAttributes(int id)
    {
		String query = "select * from evidence_single_complete(?)";

		return getSimpleJdbcTemplate().queryForObject(
				query, new AttributeListRowMapper(), id);
    }
	
	
	@Override
    public Evidence getEarliestEvidence(String dataType, String source,
            List<String> filterAttributes, List<String> filterValues)
    {
		// NB. Does not use TimeFrame - all views will use evidence_second_view.
		String filterAttribute = ServerUtil.prepareFilterArgument(filterAttributes);
		String filterValue = ServerUtil.prepareFilterArgument(filterValues);
		
		String query = "select * from evidence_min_time(?, ?, ?, ?)";		
		return getSimpleJdbcTemplate().queryForObject(query, 
				new EvidenceRowMapper(), dataType, source, filterAttribute, filterValue);
    }


	@Override
    public Evidence getLatestEvidence(String dataType, String source,
            List<String> filterAttributes, List<String> filterValues)
    {
		// NB. Does not use TimeFrame - all views will use evidence_second_view.
		String filterAttribute = ServerUtil.prepareFilterArgument(filterAttributes);
		String filterValue = ServerUtil.prepareFilterArgument(filterValues);
		
		String query = "select * from evidence_max_time(?, ?, ?, ?)";		
		return getSimpleJdbcTemplate().queryForObject(query, 
				new EvidenceRowMapper(), dataType, source, filterAttribute, filterValue);
    }
	
	
	@Override
    public Evidence searchEarliestEvidence(String dataType, String source,
            String containsText)
    {
		String query = "select * from evidence_search_min_time(?, ?, ?)";
		return getSimpleJdbcTemplate().queryForObject(query, 
				new EvidenceRowMapper(), dataType, source, containsText);
    }


	@Override
    public Evidence searchLatestEvidence(String dataType, String source,
            String containsText)
    {
		String query = "select * from evidence_search_max_time(?, ?, ?)";
		return getSimpleJdbcTemplate().queryForObject(query, 
				new EvidenceRowMapper(), dataType, source, containsText);
    }

	
	/**
	 * If evidenceId is a time series feature the metric path for 
	 * that time series will be returned or <code>null</code> 
	 * if it can't be found.
	 */
	@Override
	public MetricPath getMetricPathFromEvidenceId(int evidenceId) 
	{
		String query = "select * from metric_path_from_evidence_id(?)";
		
		List<MetricPath> paths =  getSimpleJdbcTemplate().query(query, 
											new MetricPathRowMapper(), evidenceId);
		
		if (paths.size() > 0)
		{
			return paths.get(0);
		}
		else 
		{
			return null;
		}
	}
	
	
	/**
	 * ParameterizedRowMapper class for mapping a single evidence query result 
	 * set to a list of Attribute objects. Attribute values for time fields
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
	class AttributeListRowMapper implements RowMapper<List<Attribute>>
	{
		private final static String s_TimeColumnName = com.prelert.data.Evidence.COLUMN_NAME_TIME;

		@Override
	    public List<Attribute> mapRow(ResultSet rs, int rowNum) throws SQLException
	    {
			ArrayList<Attribute> rowData = new ArrayList<Attribute>();
			
			ResultSet refCursor = (ResultSet) rs.getObject(1);
			ResultSetMetaData metaData = refCursor.getMetaData();

			String columnName;
			String columnValue;
			
			while (refCursor.next())
			{
				for (int i = 1; i <= metaData.getColumnCount(); i++)
				{
					columnName = metaData.getColumnLabel(i);
					
					if (columnName.equals(s_TimeColumnName) == true)
					{
						// Convert time value to a Long so that formatting can be
						// done client side.
						Date evidenceTime = refCursor.getTimestamp(s_TimeColumnName);
						columnValue = Long.toString(evidenceTime.getTime());
					}
					else
					{
						columnValue = refCursor.getString(i);
					}
					
					rowData.add(new Attribute(columnName, columnValue));
					
				}
			}

	        return rowData;
	    }
	}
	
}
