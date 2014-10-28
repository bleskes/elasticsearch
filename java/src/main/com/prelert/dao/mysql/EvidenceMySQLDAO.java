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
import org.springframework.jdbc.core.simple.SimpleJdbcDaoSupport;

import com.prelert.dao.EvidenceDAO;
import com.prelert.data.Evidence;
import com.prelert.data.Attribute;
import com.prelert.data.MetricPath;
import com.prelert.dao.spring.EvidenceRowMapper;
import com.prelert.dao.spring.MetricPathRowMapper;
import com.prelert.server.ServerUtil;


/**
 * Implementation for a MySQL database of the EvidenceViewDAO interface which 
 * predominantly uses calls to stored procedures to obtain information for 
 * Evidence list views.
 * @author Pete Harverson
 */
public class EvidenceMySQLDAO extends SimpleJdbcDaoSupport implements EvidenceDAO
{
	static Logger s_Logger = Logger.getLogger(EvidenceMySQLDAO.class);
	
	
	@Override
	public List<String> getAllColumns(String dataType)
	{
		// PROC is: call display_columns(type, getCompulsory, getOptional)
    	String qry = "call display_columns(?, 1, 1);";

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
	public List<String> getFilterableColumns(String dataType, boolean getCompulsory, boolean getOptional)
	{
		// PROC is: call filterable_columns(dataType, getCompulsory, getOptional)
		// Compulsory/Optional parameters not yet implemented 
		// Non-zero value: true
		String query = "call filterable_columns(?, ?, ?)";		
	
		return getSimpleJdbcTemplate().query(query.toString(), 
				new SingleColumnRowMapper<String>(), dataType, getCompulsory, getOptional);
	}
	
	
	@Override
	public List<String> getColumnValues(String dataType, String columnName, int maxRows)
	{
		// PROC is: call evidence_distinct_attribute(dataType, attribute, maxRows)
		String query = "CALL evidence_distinct_attribute(?, ?, ?)";	
		
		String debugQuery = "CALL evidence_distinct_attribute({0}, {1}, {2})";
		debugQuery = MessageFormat.format(debugQuery, dataType, columnName, maxRows);
		s_Logger.debug("getColumnValues() query: " + debugQuery);
		
		return getSimpleJdbcTemplate().query(query.toString(), 
			new SingleColumnRowMapper<String>(), dataType, columnName, maxRows);
	}
	
	@Override
	public List<Evidence> getFirstPage(String dataType, String source,
			List<String> filterAttributes, List<String> filterValues, int pageSize)
	{
		String query = "CALL evidence_first_page(?, ?, ?, ?, ?)";
		
		String filterAttrsArg = ServerUtil.prepareFilterArgument(filterAttributes);
		String filterValsArg = ServerUtil.prepareFilterArgument(filterValues);

				
		String debugQuery = "CALL evidence_first_page({0}, {1}, {2}, {3}, {4})";
		debugQuery = MessageFormat.format(debugQuery, dataType, source, 
						filterAttrsArg, filterValsArg, pageSize);
		s_Logger.debug("getFirstPage() query: " + debugQuery);
				
		return getSimpleJdbcTemplate().query(query, new EvidenceRowMapper(),
						dataType, source, filterAttrsArg, filterValsArg, pageSize);
	}
	
	
	@Override
	public List<Evidence> getLastPage(String dataType, String source, 
			List<String> filterAttributes, List<String> filterValues, int pageSize)
	{	
		String query = "CALL evidence_last_page(?, ?, ?, ?, ?)";
		
		String filterAttrsArg = ServerUtil.prepareFilterArgument(filterAttributes);
		String filterValsArg = ServerUtil.prepareFilterArgument(filterValues);
			
		String debugQuery = "CALL evidence_last_page({0}, {1}, {2}, {3}, {4})";
		debugQuery = MessageFormat.format(debugQuery, dataType, source, 
						filterAttrsArg, filterValsArg, pageSize);
		s_Logger.debug("getLastPage() query: " + debugQuery);
				
		return getSimpleJdbcTemplate().query(query, new EvidenceRowMapper(), 
						dataType, source, filterAttrsArg, filterValsArg, pageSize);
	}
	
	
	@Override
	public List<Evidence> getNextPage(String dataType, String source, 
			Date bottomRowTime, int bottomRowId,
			List<String> filterAttributes, List<String> filterValues, int pageSize)
	{
		String query = "CALL evidence_next_page(?, ?, ?, ?, ?, ?, ?)";
		
		Timestamp bottomRowTimeStamp = new Timestamp(bottomRowTime.getTime());
		String filterAttrsArg = ServerUtil.prepareFilterArgument(filterAttributes);
		String filterValsArg = ServerUtil.prepareFilterArgument(filterValues);
				
		String debugQuery = "CALL evidence_next_page({0}, {1}, {2}, {3}, {4}, {5}, {6})";
		debugQuery = MessageFormat.format(debugQuery, dataType, source, bottomRowTimeStamp, 
			bottomRowId, filterAttrsArg, filterValsArg, pageSize);
		s_Logger.debug("getNextPage() query: " + debugQuery);
				
		return getSimpleJdbcTemplate().query(query, new EvidenceRowMapper(), 
						dataType, source, bottomRowTimeStamp, bottomRowId, 
						filterAttrsArg, filterValsArg, pageSize);
	}
	
	
	@Override
	public List<Evidence> getPreviousPage(String dataType, String source, 
			Date topRowTime, int topRowId, List<String> filterAttributes, 
			List<String> filterValues, int pageSize)
	{
		String query = "CALL evidence_previous_page(?, ?, ?, ?, ?, ?, ?)";
		
		Timestamp topRowTimeStamp = new Timestamp(topRowTime.getTime());
		String filterAttrsArg = ServerUtil.prepareFilterArgument(filterAttributes);
		String filterValsArg = ServerUtil.prepareFilterArgument(filterValues);
	
		String debugQuery = "CALL evidence_previous_page({0}, {1}, {2}, {3}, {4}, {5}, {6})";
		debugQuery = MessageFormat.format(debugQuery, dataType, source, topRowTimeStamp, 
						topRowId, filterAttrsArg, filterValsArg, pageSize);
		s_Logger.debug("getPreviousPage() query: " + debugQuery);
				
		return getSimpleJdbcTemplate().query(query, new EvidenceRowMapper(), 
					dataType, source, topRowTimeStamp, topRowId, 
					filterAttrsArg, filterValsArg, pageSize);
	}
	
	
	@Override
	public List<Evidence> getAtTime(String dataType, String source, Date time, 
			List<String> filterAttributes, List<String> filterValues, int pageSize)
	{
		String query = "CALL evidence_at_time(?, ?, ?, ?, ?, ?)";
		
		Timestamp timeStamp = new Timestamp(time.getTime());
		String filterAttrsArg = ServerUtil.prepareFilterArgument(filterAttributes);
		String filterValsArg = ServerUtil.prepareFilterArgument(filterValues);

		String debugQuery = "CALL evidence_at_time({0}, {1}, {2}, {3}, {4}, {5})";
		debugQuery = MessageFormat.format(
				debugQuery, dataType, source, timeStamp, 
				filterAttrsArg, filterValsArg, pageSize);
		s_Logger.debug("getAtTime() query: " + debugQuery);
				
		return getSimpleJdbcTemplate().query(query, new EvidenceRowMapper(), 
						dataType, source, timeStamp, filterAttrsArg, filterValsArg, pageSize);
	}
	
	
	@Override
	public List<Evidence> getIdPage(String dataType, String source, int id, int pageSize)
	{
		String query = "CALL evidence_id_page(?, ?, ?, ?);";
		
		String debugQuery = "CALL evidence_id_page({0}, {1}, {2}, {3})";
		debugQuery = MessageFormat.format(
				debugQuery, dataType, source, id, pageSize);
		s_Logger.debug("getIdPage() query: " + debugQuery);
		
		return getSimpleJdbcTemplate().query(query, new EvidenceRowMapper(), 
				dataType, source, id, pageSize);
	}
	
	
	@Override
	public List<Evidence> searchFirstPage(
			String dataType, String source, String containsText, int pageSize)
	{
		// Query calls a stored procedure in the form:
		// CALL evidence_search_first_page(type, source, searchString, pageSize)
		String query = "CALL evidence_search_first_page(?, ?, ?, ?)";
		
		String debugQuery = "CALL evidence_search_first_page({0}, {1}, {2}, {3})";
		debugQuery = MessageFormat.format(debugQuery, dataType, source, 
				containsText, pageSize);
		s_Logger.debug("searchFirstPage() query: " + debugQuery);
		
		return getSimpleJdbcTemplate().query(query, new EvidenceRowMapper(),
				dataType, source, containsText, pageSize);
	}
	
	
	@Override
	public List<Evidence> searchLastPage(
			String dataType, String source, String containsText, int pageSize)
	{
		// Query calls a stored procedure in the form:
		// CALL evidence_search_last_page(type, source, searchString, pageSize)
		String query = "CALL evidence_search_last_page(?, ?, ?, ?)";
		
		String debugQuery = "CALL evidence_search_last_page({0}, {1}, {2}, {3})";
		debugQuery = MessageFormat.format(debugQuery, dataType, source, 
				containsText, pageSize);
		s_Logger.debug("searchLastPage() query: " + debugQuery);
		
		return getSimpleJdbcTemplate().query(query, new EvidenceRowMapper(),
				dataType, source, containsText, pageSize);
	}
	
	
	@Override
	public List<Evidence> searchNextPage(
			String dataType, String source, Date bottomRowTime, 
			int bottomRowId, String containsText, int pageSize)
	{
		// Query calls a stored procedure in the form:
		// evidence_search_next_page(type, source, time, id, searchString, pageSize)
		String query = "CALL evidence_search_next_page(?, ?, ?, ?, ?, ?)";
		
		String debugQuery = "CALL evidence_search_next_page({0}, {1}, {2}, {3}, {4}, {5})";
		debugQuery = MessageFormat.format(debugQuery, dataType, source, 
				bottomRowTime, bottomRowId, containsText, pageSize);
		s_Logger.debug("searchNextPage() query: " + debugQuery);
		
		return getSimpleJdbcTemplate().query(query, new EvidenceRowMapper(),
				dataType, source, bottomRowTime, bottomRowId, containsText, pageSize);
	}
	
	
	@Override
	public List<Evidence> searchPreviousPage(
			String dataType, String source, Date topRowTime, 
			int topRowId, String containsText, int pageSize)
	{
		// Query calls a stored procedure in the form:
		// evidence_search_previous_page(type, source, time, id, searchString, pageSize)
		String query = "CALL evidence_search_previous_page(?, ?, ?, ?, ?, ?)";
		
		String debugQuery = "CALL evidence_search_previous_page({0}, {1}, {2}, {3}, {4}, {5})";
		debugQuery = MessageFormat.format(debugQuery, dataType, source, 
				topRowTime, topRowId, containsText, pageSize);
		s_Logger.debug("searchPreviousPage() query: " + debugQuery);
		
		return getSimpleJdbcTemplate().query(query, new EvidenceRowMapper(),
				dataType, source, topRowTime, topRowId, containsText, pageSize);

	}
	
	
	@Override
	public List<Evidence> searchAtTime(String dataType, String source, 
			Date time, String containsText, int pageSize)
	{
		// Query calls a stored procedure in the form:
		// evidence_search_at_time(type, source, time, searchString, pageSize)
		String query = "CALL evidence_search_at_time(?, ?, ?, ?, ?)";
		
		String debugQuery = "CALL evidence_search_at_time({0}, {1}, {2}, {3}, {4})";
		debugQuery = MessageFormat.format(debugQuery, dataType, source, 
				time, containsText, pageSize);
		s_Logger.debug("searchAtTime() query: " + debugQuery);
		
		return getSimpleJdbcTemplate().query(query, new EvidenceRowMapper(),
				dataType, source, time, containsText, pageSize);
	}
	
	
	/**
	 * Returns data for a single item of evidence with the specified id.
	 * @param id the unique identifier for the item of evidence.
	 * @return full data model for the evidence item with the specified id.
	 */
	public Evidence getEvidenceSingle(int id)
	{
		String query = "CALL evidence_single_complete(?);";
		
		String debugQuery = "CALL evidence_single_complete({0});";
		debugQuery = MessageFormat.format(debugQuery, id);
		s_Logger.debug("getEvidenceSingle() query: " + debugQuery);

		List<Evidence> evidenceList = 
			getSimpleJdbcTemplate().query(query, new EvidenceRowMapper(), id);
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
	
	
	/**
	 * Returns all attributes for the item of evidence with the specified id 
  	 * (instead of just the current display columns).
	 * @param id the value of the id column for the evidence to obtain information on.
	 * @return List of Attribute objects for the row with the specified id.
	 * 		Note that values for time fields are transported as a String representation
	 * 		of the number of milliseconds since January 1, 1970, 00:00:00 GMT.
	 */
	public List<Attribute> getEvidenceAttributes(int id)
	{
		String query = "CALL evidence_single_complete(?)";

		return getSimpleJdbcTemplate().queryForObject(
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
	public Evidence getEarliestEvidence(String dataType, String source,
			List<String> filterAttributes, List<String> filterValues)
	{		
		// NB. Does not use TimeFrame - all views will use evidence_second_view.
		
		String filterAttribute = ServerUtil.prepareFilterArgument(filterAttributes);
		String filterValue = ServerUtil.prepareFilterArgument(filterValues);
		
		String query = "CALL evidence_min_time(?, ?, ?, ?)";
		return getSimpleJdbcTemplate().queryForObject(query.toString(), 
				new EvidenceRowMapper(), dataType, source, filterAttribute, filterValue);
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
	public Evidence getLatestEvidence(String dataType, String source,
			List<String> filterAttributes, List<String> filterValues)
	{
		// NB. Does not use TimeFrame - all views will use evidence_second_view.
		String filterAttribute = ServerUtil.prepareFilterArgument(filterAttributes);
		String filterValue = ServerUtil.prepareFilterArgument(filterValues);
		
		String query = "CALL evidence_max_time(?, ?, ?, ?)";		
		return getSimpleJdbcTemplate().queryForObject(query.toString(), 
				new EvidenceRowMapper(), dataType, source, filterAttribute, filterValue);
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
	public Evidence searchEarliestEvidence(String dataType, String source, String containsText)
	{		
		String query = "CALL evidence_search_min_time(?, ?, ?)";
		return getSimpleJdbcTemplate().queryForObject(query.toString(), 
				new EvidenceRowMapper(), dataType, source, containsText);
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
	public Evidence searchLatestEvidence(String dataType, String source, String containsText)
	{
		String query = "CALL evidence_search_max_time(?, ?, ?)";
		return getSimpleJdbcTemplate().queryForObject(query.toString(), 
				new EvidenceRowMapper(), dataType, source, containsText);
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
		String query = "CALL evidence_for_time_series(?, ?, ?, ?, ?, ?, ?)";

		String attributeNames = ServerUtil.prepareAttributeNameArgument(attributes);
		String attributeValues = ServerUtil.prepareAttributeValueArgument(attributes);

		return getSimpleJdbcTemplate().query(query, new EvidenceRowMapper(),
											datatype, metric, source,
											startTime, endTime,
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
		String query = "CALL evidence_for_external_time_series(?, ?, ?, ?, ?, ?, ?)";

		String attributeNames = ServerUtil.prepareAttributeNameArgument(attributes);
		String attributeValues = ServerUtil.prepareAttributeValueArgument(attributes);

		return getSimpleJdbcTemplate().query(query, new EvidenceRowMapper(),
											datatype, metric, source,
											startTime, endTime,
											attributeNames, attributeValues);
	}
	
	
	/**
	 * If evidenceId is a time series feature the metric path for 
	 * that time series will be returned or <code>null</code> 
	 * if it can't be found.
	 */
	@Override
	public MetricPath getMetricPathFromEvidenceId(int evidenceId) 
	{
		String query = "CALL metric_path_from_evidence_id(?)";
		
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
	 * 
	 * @author Pete Harverson
	 */
	class EvidenceAttributeRowMapper implements RowMapper<List<Attribute>>
	{
		private final static String s_TimeColumnName = com.prelert.data.Evidence.COLUMN_NAME_TIME;

		@Override
	    public List<Attribute> mapRow(ResultSet rs, int rowNum) throws SQLException
	    {
			ArrayList<Attribute> rowData = new ArrayList<Attribute>();
			
			ResultSetMetaData metaData = rs.getMetaData();

			String columnName;
			String columnValue;
			for (int i = 1; i <= metaData.getColumnCount(); i++)
			{
				columnName = metaData.getColumnLabel(i);
				
				if (columnName.equals(s_TimeColumnName) == true)
				{
					// Convert time value to a Long so that formatting can be
					// done client side.
					Date evidenceTime = rs.getTimestamp(s_TimeColumnName);
					columnValue = Long.toString(evidenceTime.getTime());
				}
				else
				{
					columnValue = rs.getString(i);
				}
				
				rowData.add(new Attribute(columnName, columnValue));
				
			}

	        return rowData;
	    }
	}

}
